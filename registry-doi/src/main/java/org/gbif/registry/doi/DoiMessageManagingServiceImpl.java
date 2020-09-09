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
package org.gbif.registry.doi;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiStatus;
import org.gbif.common.messaging.api.Message;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.ChangeDoiMessage;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.service.InvalidMetadataException;
import org.gbif.doi.service.datacite.DataCiteValidator;

import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Service
public class DoiMessageManagingServiceImpl implements DoiMessageManagingService {

  private static final Logger LOG = LoggerFactory.getLogger(DoiMessageManagingServiceImpl.class);

  private final MessagePublisher messagePublisher;

  private final URI datasetTarget;
  private final URI downloadTarget;
  private final URI dataPackageTarget;

  public DoiMessageManagingServiceImpl(
      @Value("${portal.url}") URI portal, @Lazy MessagePublisher messagePublisher) {
    checkNotNull(portal, "portal base URL can't be null");
    checkArgument(portal.isAbsolute(), "portal base URL must be absolute");
    this.datasetTarget = portal.resolve("dataset/");
    this.downloadTarget = portal.resolve("occurrence/download/");
    this.dataPackageTarget = portal.resolve("data_package/");
    this.messagePublisher = messagePublisher;
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
  public void registerDerivedDataset(
      DOI doi, DataCiteMetadata metadata, URI target, Date registrationDate)
      throws InvalidMetadataException {
    checkNotNull(doi, "DOI required");
    checkNotNull(messagePublisher, "No message publisher configured to send DoiChangeMessage");

    Date now = new Date();
    DoiStatus registrationStatus =
        (registrationDate == null
                || DateUtils.isSameDay(now, registrationDate)
                || registrationDate.before(now))
            ? DoiStatus.REGISTERED
            : DoiStatus.RESERVED;
    LOG.debug("Registering derived dataset DOI {} with status {}", doi, registrationStatus);

    String xml = DataCiteValidator.toXml(doi, metadata);
    Message message = new ChangeDoiMessage(registrationStatus, doi, xml, target);

    try {
      messagePublisher.send(message);
    } catch (IOException e) {
      LOG.error("Failed sending DoiChangeMessage for {} and derived dataset", doi, e);
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
