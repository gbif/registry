package org.gbif.registry.ws.util;

import org.gbif.api.model.registry.Dataset;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;

public class DataCiteConverter {

  public static DataCiteMetadata convert(Dataset dataset) {
    DataCiteMetadata dc = DataCiteMetadata.builder()
      .withRelatedIdentifiers().end()
      .build();
    return dc;
  }
}
