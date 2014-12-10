package org.gbif.registry.doi;

import org.gbif.api.model.common.DOI;
import org.gbif.common.messaging.api.Message;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.service.DoiStatus;
import org.gbif.registry.events.MessageSendingEventListener;
import org.gbif.registry.persistence.mapper.DoiMapper;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.ibatis.exceptions.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class DoiGeneratorMQ implements DoiGenerator {
  private static final Logger LOG = LoggerFactory.getLogger(MessageSendingEventListener.class);

  private final DoiMapper mapper;
  private final URI datasetTarget;
  private final URI downloadTarget;
  private final String prefix;
  private final static int RANDOM_LENGTH = 6;
  /**
   * The messagePublisher can be optional, and optional is not supported in constructor injection.
   */
  @Inject(optional = true)
  private final MessagePublisher messagePublisher = null;

  @Inject
  public DoiGeneratorMQ(DoiMapper mapper, @Named("portal.url") URI portal, @Named("doi.prefix") String prefix) {
    checkArgument(prefix.startsWith("10."), "DOI prefix must begin with 10.");
    this.prefix = prefix;
    this.mapper = mapper;
    checkNotNull(portal, "portal base URL can't be null");
    checkArgument(portal.isAbsolute(), "portal base URL must be absolute");
    datasetTarget = portal.resolve("dataset/");
    downloadTarget = portal.resolve("occurrence/download/detail/");
  }

  private DOI newDOI(final String shoulder, DoiType type) {
    // only try for hundred times then fail
    for (int x=0; x<100; x++) {
      DOI doi = random(shoulder);
      try {
        mapper.create(doi, type);
        return doi;
      } catch (PersistenceException e) {
        // might have hit a unique constraint, try another doi
        LOG.debug("Random {} DOI {} existed. Try another one", type, doi, e.getMessage());
      }
    }
    throw new IllegalStateException("Tried 100 random DOIs and none worked, Giving up");
  }

  @Override
  public DOI newDatasetDOI() {
    return newDOI("", DoiType.DATASET);
  }

  @Override
  public DOI newDownloadDOI() {
    return newDOI("", DoiType.DATASET);
  }

  @Override
  public boolean isGbif(DOI doi) {
    return doi.getPrefix().equalsIgnoreCase(prefix);
  }

  /**
   * @return a random DOI with the given prefix. It is not guaranteed to be unique and might exist already
   */
  private DOI random(String shoulder) {
    String suffix = Strings.nullToEmpty(shoulder) + RandomStringUtils.randomAlphanumeric(RANDOM_LENGTH);
    return new DOI(prefix, suffix);
  }

  private void validate(DataCiteMetadata m) {
    Preconditions.checkArgument(!m.getCreators().getCreator().isEmpty(), "Creator required");
    Preconditions.checkArgument(!m.getTitles().getTitle().isEmpty(), "Title required");
    Preconditions.checkNotNull(m.getPublicationYear(), "Publication year required");
    Preconditions.checkNotNull(m.getPublisher(), "Publisher required");
  }

  @Override
  public void registerDataset(DOI doi, DataCiteMetadata metadata, UUID datasetKey) throws IllegalArgumentException {
    Preconditions.checkNotNull(doi, "DOI required");
    Preconditions.checkNotNull(datasetKey, "Dataset key required");
    validate(metadata);

    Message message = new DoiChangeMessage(DoiStatus.Status.REGISTERED, doi, metadata, datasetTarget.resolve(datasetKey.toString()));

    if (messagePublisher == null) {
      LOG.warn("No message publisher configured to send DoiChangeMessage for {} and dataset {}", doi, datasetKey);
    } else {
      try {
        messagePublisher.send(message);
      } catch (IOException e) {
        LOG.warn("Failed sending DoiChangeMessage for {} and dataset {}", doi, datasetKey, e);
      }
    }
  }

  @Override
  public void registerDownload(DOI doi, DataCiteMetadata metadata, String downloadKey) throws IllegalArgumentException {
    Preconditions.checkNotNull(doi, "DOI required");
    Preconditions.checkNotNull(downloadKey, "Download key required");
    validate(metadata);

    Message message = new DoiChangeMessage(DoiStatus.Status.REGISTERED, doi, metadata, downloadTarget.resolve(downloadKey));

    if (messagePublisher == null) {
      LOG.warn("No message publisher configured to send DoiChangeMessage for {} and download {}", doi, downloadKey);
    } else {
      try {
        messagePublisher.send(message);
      } catch (IOException e) {
        LOG.warn("Failed sending DoiChangeMessage for {} and download {}", doi, downloadKey, e);
      }
    }
  }
}
