package org.gbif.registry.cli.common.stubs;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.registry.Dataset;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.metadata.datacite.RelationType;
import org.gbif.registry.doi.DataCiteMetadataBuilderService;
import org.gbif.registry.domain.ws.Citation;

import javax.annotation.Nullable;

public class DataCiteMetadataBuilderServiceStub implements DataCiteMetadataBuilderService {
  @Override
  public DataCiteMetadata buildMetadata(Download download, GbifUser user) {
    return null;
  }

  @Override
  public DataCiteMetadata buildMetadata(Dataset dataset) {
    return null;
  }

  @Override
  public DataCiteMetadata buildMetadata(Citation citation) {
    return null;
  }

  @Override
  public DataCiteMetadata buildMetadata(Dataset dataset, @Nullable DOI related, @Nullable RelationType relationType) {
    return null;
  }
}
