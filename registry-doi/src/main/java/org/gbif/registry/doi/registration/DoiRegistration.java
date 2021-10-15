/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.doi.registration;

import org.gbif.api.annotation.Generated;
import org.gbif.api.model.common.DOI;
import org.gbif.registry.domain.doi.DoiType;

import javax.annotation.Nullable;

import com.google.common.base.Objects;

/**
 * Encapsulates a DOI registration request. Some fields are optional and its values can trigger a
 * different behaviour in the DoiRegistrationService.
 */
public class DoiRegistration {

  @Nullable private DOI doi;

  @Nullable private String key;

  private String metadata;

  private DoiType type;

  private String user;

  /** Default constructor. */
  public DoiRegistration() {}

  /** Full constructor. */
  private DoiRegistration(DOI doi, String key, String metadata, DoiType type, String user) {
    this.doi = doi;
    this.key = key;
    this.metadata = metadata;
    this.type = type;
    this.user = user;
  }

  /**
   * If the DOI existed prior the registration, maybe it was reserved previously, this field must be
   * provided.
   */
  public DOI getDoi() {
    return doi;
  }

  public void setDoi(DOI doi) {
    this.doi = doi;
  }

  /**
   * Key, as a String, of the element to be linked to the DOI registration. This can be a download
   * key, a dataset UUID key or any other value.
   */
  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  /** DataCite metadata object that will be provided to DOI register. */
  public String getMetadata() {
    return metadata;
  }

  public void setMetadata(String metadata) {
    this.metadata = metadata;
  }

  /** Type of DOI requested. */
  public DoiType getType() {
    return type;
  }

  public void setType(DoiType type) {
    this.type = type;
  }

  /**
   * GIBF user who requested the DOI or created the associated element: download, dataset or data
   * package.
   */
  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  /** Creates a new DoiRegistration.Builder. */
  public static Builder builder() {
    return new Builder();
  }

  @Generated
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof DoiRegistration)) {
      return false;
    }

    DoiRegistration that = (DoiRegistration) obj;
    return Objects.equal(key, that.key)
        && Objects.equal(doi, that.doi)
        && Objects.equal(metadata, that.metadata)
        && Objects.equal(type, that.type)
        && Objects.equal(user, that.user);
  }

  @Generated
  @Override
  public int hashCode() {
    return Objects.hashCode(key, doi, metadata, type, user);
  }

  @Generated
  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("key", key)
        .add("doi", doi)
        .add("metadata", metadata)
        .add("type", type)
        .add("user", user)
        .toString();
  }

  /** Builder class to simplify the construction of DoiRegistration instances. */
  public static class Builder {

    private DOI doi;

    private String key;

    private String metadata;

    private DoiType type;

    private String user;

    /** Sets the optional DOI value. */
    public Builder withDoi(DOI doi) {
      this.doi = doi;
      return this;
    }

    /** Sets the optional key value. */
    public Builder withKey(String key) {
      this.key = key;
      return this;
    }

    /** Set the DataCite metadata. */
    public Builder withMetadata(String metadata) {
      this.metadata = metadata;
      return this;
    }

    /** Sets the DoiType. */
    public Builder withType(DoiType type) {
      this.type = type;
      return this;
    }

    /** Set the user name. */
    public Builder withUser(String user) {
      this.user = user;
      return this;
    }

    /** Builds a new DoiRegistration instance. */
    public DoiRegistration build() {
      return new DoiRegistration(doi, key, metadata, type, user);
    }
  }
}
