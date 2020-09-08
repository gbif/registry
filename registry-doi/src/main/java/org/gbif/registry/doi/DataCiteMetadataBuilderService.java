package org.gbif.registry.doi;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.registry.Dataset;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.metadata.datacite.RelationType;
import org.gbif.registry.domain.ws.Citation;

import javax.annotation.Nullable;

public interface DataCiteMetadataBuilderService {

  /** Build the DataCiteMetadata for a Download. */
  DataCiteMetadata buildMetadata(Download download, GbifUser user);

  /** Build the DataCiteMetadata for a Dataset. */
  DataCiteMetadata buildMetadata(Dataset dataset);

  /** Build the DataCiteMetadata for a Citation. */
  DataCiteMetadata buildMetadata(Citation citation);

  /** Build the DataCiteMetadata for a Dataset that includes a relation to another DOI. */
  DataCiteMetadata buildMetadata(
      Dataset dataset, @Nullable DOI related, @Nullable RelationType relationType);
}
