package org.gbif.registry.doi.registration;

import org.gbif.api.model.common.DOI;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.registry.doi.DoiType;

import javax.annotation.Nullable;

public class DoiRegistration {

  @Nullable
  private DOI doi;

  @Nullable
  String key;

  DataCiteMetadata metadata;

  DoiType type;

  String user;

  public DOI getDoi() {
    return doi;
  }

  public void setDoi(DOI doi) {
    this.doi = doi;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public DataCiteMetadata getMetadata() {
    return metadata;
  }

  public void setMetadata(DataCiteMetadata metadata) {
    this.metadata = metadata;
  }

  public DoiType getType() {
    return type;
  }

  public void setType(DoiType type) {
    this.type = type;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }
}
