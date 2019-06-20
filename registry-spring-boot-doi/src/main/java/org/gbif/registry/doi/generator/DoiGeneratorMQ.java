package org.gbif.registry.doi.generator;

import com.google.common.base.Strings;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.api.model.common.DoiStatus;
import org.gbif.common.messaging.api.Message;
import org.gbif.common.messaging.api.messages.ChangeDoiMessage;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.service.InvalidMetadataException;
import org.gbif.doi.service.datacite.DataCiteValidator;
import org.gbif.registry.doi.DoiPersistenceService;
import org.gbif.registry.doi.DoiType;
import org.gbif.service.exception.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Service
public class DoiGeneratorMQ implements DoiGenerator {

  private static final Logger LOG = LoggerFactory.getLogger(DoiGeneratorMQ.class);

  private final DoiPersistenceService doiPersistenceService;
  private final MessageSender messageSender;

  private final URI datasetTarget;
  private final URI downloadTarget;
  private final URI dataPackageTarget;
  private static final int RANDOM_LENGTH = 6;

  private final URI portal;
  private final String prefix;

  public DoiGeneratorMQ(
      @Value("${portal.url}") URI portal,
      @Value("${doi.prefix}") String prefix,
      DoiPersistenceService doiPersistenceService,
      MessageSender messageSender) {
    checkArgument(prefix.startsWith("10."), "DOI prefix must begin with '10.'");
    this.prefix = prefix;
    this.doiPersistenceService = doiPersistenceService;
    checkNotNull(portal, "portal base URL can't be null");
    checkArgument(portal.isAbsolute(), "portal base URL must be absolute");
    this.portal = portal;
    datasetTarget = portal.resolve("dataset/");
    downloadTarget = portal.resolve("occurrence/download/");
    dataPackageTarget = portal.resolve("data_package/");
    this.messageSender = messageSender;
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
    // only try for hundred times then fail
    for (int x = 0; x < 100; x++) {
      DOI doi = random(shoulder);
      try {
        doiPersistenceService.create(doi, type);
        return doi;
      } catch (PersistenceException e) {
        // might have hit a unique constraint, try another doi
        LOG.debug("Exception: {}. Random {} DOI {} existed. Try another one", type, doi, e.getMessage());
      }
    }
    throw new IllegalStateException("Tried 100 random DOIs and none worked, Giving up");
  }

  /**
   * @return a random DOI with the given prefix. It is not guaranteed to be unique and might exist already
   */
  private DOI random(@Nullable String shoulder) {
    String suffix = Strings.nullToEmpty(shoulder) + RandomStringUtils.randomAlphanumeric(RANDOM_LENGTH);
    return new DOI(prefix, suffix);
  }

  @Override
  public boolean isGbif(DOI doi) {
    return doi != null && doi.getPrefix().equalsIgnoreCase(prefix);
  }

  @Override
  public void failed(DOI doi, InvalidMetadataException e) {
    // Updates the doi table to FAILED status and uses the error stacktrace as the xml for debugging.
    doiPersistenceService.update(doi, new DoiData(DoiStatus.FAILED, null), ExceptionUtils.getStackTrace(e));
  }

  @Override
  public void registerDataset(DOI doi, DataCiteMetadata metadata, UUID datasetKey) throws InvalidMetadataException {
    checkNotNull(doi, "DOI required");
    checkNotNull(datasetKey, "Dataset key required");
    checkNotNull(messageSender, "No message publisher configured to send DoiChangeMessage");

    String xml = DataCiteValidator.toXml(doi, metadata);
    Message message = new ChangeDoiMessage(DoiStatus.REGISTERED, doi, xml, datasetTarget.resolve(datasetKey.toString()));

    try {
      messageSender.send(message);
    } catch (IOException e) {
      LOG.error("Failed sending DoiChangeMessage for {} and dataset {}", doi, datasetKey, e);
    }
  }

  @Override
  public void registerDownload(DOI doi, DataCiteMetadata metadata, String downloadKey) throws InvalidMetadataException {
    checkNotNull(doi, "DOI required");
    checkNotNull(downloadKey, "Download key required");
    checkNotNull(messageSender, "No message publisher configured to send DoiChangeMessage");

    String xml = DataCiteValidator.toXml(doi, metadata);
    Message message = new ChangeDoiMessage(DoiStatus.REGISTERED, doi, xml, downloadTarget.resolve(downloadKey));

    try {
      messageSender.send(message);
    } catch (IOException e) {
      LOG.error("Failed sending DoiChangeMessage for {} and download {}", doi, downloadKey, e);
    }
  }

  @Override
  public void registerDataPackage(DOI doi, DataCiteMetadata metadata) throws InvalidMetadataException {
    checkNotNull(doi, "DOI required");
    checkNotNull(messageSender, "No message publisher configured to send DoiChangeMessage");

    String xml = DataCiteValidator.toXml(doi, metadata);
    Message message = new ChangeDoiMessage(DoiStatus.REGISTERED, doi, xml, dataPackageTarget.resolve(doi.getDoiName()));
    try {
      messageSender.send(message);
    } catch (IOException e) {
      LOG.error("Failed sending DoiChangeMessage for DataPackage {}", doi, e);
    }
  }

  @Override
  public void delete(DOI doi) {
    checkNotNull(doi, "DOI required");
    checkNotNull(messageSender, "No message publisher configured to send DoiChangeMessage");

    Message message = new ChangeDoiMessage(DoiStatus.DELETED, doi, null, null);

    try {
      messageSender.send(message);
    } catch (IOException e) {
      LOG.error("Failed sending DoiChangeMessage for {}", doi, e);
    }
  }
}
