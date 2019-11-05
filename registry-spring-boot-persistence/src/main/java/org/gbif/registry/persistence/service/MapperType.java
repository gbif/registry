package org.gbif.registry.persistence.service;

public enum MapperType {
  ORGANIZATION("organizationMapper"),
  CONTACT("contactMapper"),
  ENDPOINT("endpointMapper"),
  MACHINE_TAG("machineTagMapper"),
  TAG("tagMapper"),
  IDENTIFIER("identifierMapper"),
  COMMENT("commentMapper"),
  DATASET("datasetMapper"),
  INSTALLATION("installationMapper"),
  NODE("nodeMapper"),
  NETWORK("networkMapper"),
  METADATA("metadataMapper"),
  DATASET_PROCESS_STATUS("datasetProcessStatusMapper"),
  METASYNC_HISTORY_MAPPER("metasyncHistoryMapper");

  private String name;

  MapperType(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
