package org.gbif.registry.doi.registration;

import org.gbif.api.model.common.DOI;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.registry.doi.DoiType;

import javax.annotation.Nullable;

import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * Encapsulates a DOI registration request. Some fields are optional and its values can trigger a different behaviour in
 * the DoiRegistrationService.
 */
public class DoiRegistration {

  @JsonSerialize(using = DOI.Serializer.class)
  @JsonDeserialize(using = DOI.Deserializer.class)
  @Nullable
  private DOI doi;

  @Nullable
  String key;

  DataCiteMetadata metadata;

  DoiType type;

  String user;

  /**
   * If the DOI existed prior the registration, maybe it was reserved previously, this field must be provided.
   */
  public DOI getDoi() {
    return doi;
  }

  public void setDoi(DOI doi) {
    this.doi = doi;
  }

  /**
   * Key, as a String, of the element to be linked to the DOI registration.
   * This can be a download key, a dataset UUID key or any other value.
   */
  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  /**
   * DataCite metadata object that will be provided to DOI register.
   */
  public DataCiteMetadata getMetadata() {
    return metadata;
  }

  public void setMetadata(DataCiteMetadata metadata) {
    this.metadata = metadata;
  }

  /**
   * Type of DOI requested.
   */
  public DoiType getType() {
    return type;
  }

  public void setType(DoiType type) {
    this.type = type;
  }

  /**
   * GIBF user who requested the DOI or created the associated element: download, dataset or data package.
   */
  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }
}
