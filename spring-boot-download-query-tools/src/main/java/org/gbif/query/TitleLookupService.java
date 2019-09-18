package org.gbif.query;

public interface TitleLookupService {

  String getDatasetTitle(String datasetKey);

  String getSpeciesName(String usageKey);
}
