package org.gbif.registry.cli.datasetupdater.stubs;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.User;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.registry.Dataset;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.metadata.datacite.RelationType;
import org.gbif.registry.doi.handler.DataCiteDoiHandlerStrategy;

import java.util.UUID;
import javax.annotation.Nullable;

/**
 * Stub class used to simplify Guice binding, e.g. when this class must be bound but isn't used by CLI.
 */
public class DoiHandlerStrategyStub implements DataCiteDoiHandlerStrategy {

  @Override
  public boolean isUsingMyPrefix(DOI doi) {
    return false;
  }

  @Override
  public DataCiteMetadata buildMetadata(Download download, User user) {
    return null;
  }

  @Override
  public DataCiteMetadata buildMetadata(Dataset dataset) {
    return null;
  }

  @Override
  public DataCiteMetadata buildMetadata(Dataset dataset, @Nullable DOI related, @Nullable RelationType relationType) {
    return null;
  }

  @Override
  public void datasetChanged(Dataset dataset, @Nullable DOI previousDoi) {

  }

  @Override
  public void downloadChanged(Download download, Download previousDownload, User user) {

  }

  @Override
  public void scheduleDatasetRegistration(DOI doi, DataCiteMetadata metadata, UUID datasetKey) {

  }
}
