/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.doi.generator;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.api.model.common.DoiStatus;
import org.gbif.common.messaging.api.Message;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.ChangeDoiMessage;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.service.InvalidMetadataException;
import org.gbif.doi.service.datacite.DataCiteValidator;
import org.gbif.registry.doi.config.DoiConfigurationProperties;
import org.gbif.registry.domain.doi.DoiType;
import org.gbif.registry.persistence.mapper.DoiMapper;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Service
public class DoiGeneratorMQ implements DoiGenerator {

  private static final Logger LOG = LoggerFactory.getLogger(DoiGeneratorMQ.class);

  private final DoiMapper doiMapper;
  private final MessagePublisher messagePublisher;

  private final URI datasetTarget;
  private final URI downloadTarget;
  private final URI dataPackageTarget;
  private static final int RANDOM_LENGTH = 6;
  private static final String DOI_CHARACTERS = "23456789abcdefghjkmnpqrstuvwxyz"; // Exclude 0o 1il

  private final String prefix;

  public DoiGeneratorMQ(
      @Value("${portal.url}") URI portal,
      DoiConfigurationProperties doiConfigProperties,
      DoiMapper doiMapper,
      @Lazy MessagePublisher messagePublisher) {
    prefix = doiConfigProperties.getPrefix();
    checkArgument(prefix.startsWith("10."), "DOI prefix must begin with '10.'");
    this.doiMapper = doiMapper;
    checkNotNull(portal, "portal base URL can't be null");
    checkArgument(portal.isAbsolute(), "portal base URL must be absolute");
    datasetTarget = portal.resolve("dataset/");
    downloadTarget = portal.resolve("occurrence/download/");
    dataPackageTarget = portal.resolve("data_package/");
    this.messagePublisher = messagePublisher;
  }

  @Override
  public DOI newDatasetDOI() {
    return newDOI("", DoiType.DATASET);
  }

  @Override
  public DOI newDownloadDOI() {
    return newDOI("dl.", DoiType.DOWNLOAD);
  }

  @Override
  public DOI newDataPackageDOI() {
    return newDOI("dp.", DoiType.DATA_PACKAGE);
  }

  private DOI newDOI(final String shoulder, DoiType type) {
    // try a thousand times then fail
    for (int x = 0; x < 1000; x++) {
      DOI doi = random(shoulder);
      try {
        doiMapper.create(doi, type);
        if (x > 100) {
          LOG.warn("Had to search {} times to find the available {} DOI {}.", x, type, doi);
        }
        return doi;
      } catch (Exception e) {
        // might have hit a unique constraint, try another doi
        if (x <= 100) {
          LOG.debug("Random {} DOI {} already exists at attempt {}", type, doi, x);
        } else {
          LOG.info("Random {} DOI {} already exists at attempt {}", type, doi, x);
        }
      }
    }
    throw new IllegalStateException("Tried 1000 random DOIs and none worked, giving up.");
  }

  /**
   * @return a random DOI with the given prefix. It is not guaranteed to be unique and might exist
   *     already
   */
  private DOI random(@Nullable String shoulder) {
    String suffix =
        Strings.nullToEmpty(shoulder) + RandomStringUtils.random(RANDOM_LENGTH, DOI_CHARACTERS);
    return new DOI(prefix, suffix);
  }

  @Override
  public boolean isGbif(DOI doi) {
    return doi != null && doi.getPrefix().equalsIgnoreCase(prefix);
  }

  @Override
  public void failed(DOI doi, InvalidMetadataException e) {
    // Updates the doi table to FAILED status and uses the error stacktrace as the xml for
    // debugging.
    doiMapper.update(doi, new DoiData(DoiStatus.FAILED, null), ExceptionUtils.getStackTrace(e));
  }

  @Override
  public void registerDataset(DOI doi, DataCiteMetadata metadata, UUID datasetKey)
      throws InvalidMetadataException {
    checkNotNull(doi, "DOI required");
    checkNotNull(datasetKey, "Dataset key required");
    checkNotNull(messagePublisher, "No message publisher configured to send DoiChangeMessage");

    String xml = DataCiteValidator.toXml(doi, metadata);
    Message message =
        new ChangeDoiMessage(
            DoiStatus.REGISTERED, doi, xml, datasetTarget.resolve(datasetKey.toString()));

    try {
      messagePublisher.send(message);
    } catch (IOException e) {
      LOG.error("Failed sending DoiChangeMessage for {} and dataset {}", doi, datasetKey, e);
    }
  }

  @Override
  public void registerDownload(DOI doi, DataCiteMetadata metadata, String downloadKey)
      throws InvalidMetadataException {
    checkNotNull(doi, "DOI required");
    checkNotNull(downloadKey, "Download key required");
    checkNotNull(messagePublisher, "No message publisher configured to send DoiChangeMessage");

    String xml = DataCiteValidator.toXml(doi, metadata);
    Message message =
        new ChangeDoiMessage(DoiStatus.REGISTERED, doi, xml, downloadTarget.resolve(downloadKey));

    try {
      messagePublisher.send(message);
    } catch (IOException e) {
      LOG.error("Failed sending DoiChangeMessage for {} and download {}", doi, downloadKey, e);
    }
  }

  @Override
  public void registerDataPackage(DOI doi, DataCiteMetadata metadata)
      throws InvalidMetadataException {
    checkNotNull(doi, "DOI required");
    checkNotNull(messagePublisher, "No message publisher configured to send DoiChangeMessage");

    String xml = DataCiteValidator.toXml(doi, metadata);
    Message message =
        new ChangeDoiMessage(
            DoiStatus.REGISTERED, doi, xml, dataPackageTarget.resolve(doi.getDoiName()));
    try {
      messagePublisher.send(message);
    } catch (IOException e) {
      LOG.error("Failed sending DoiChangeMessage for DataPackage {}", doi, e);
    }
  }

  @Override
  public void delete(DOI doi) {
    checkNotNull(doi, "DOI required");
    checkNotNull(messagePublisher, "No message publisher configured to send DoiChangeMessage");

    Message message = new ChangeDoiMessage(DoiStatus.DELETED, doi, null, null);

    try {
      messagePublisher.send(message);
    } catch (IOException e) {
      LOG.error("Failed sending DoiChangeMessage for {}", doi, e);
    }
  }
}
