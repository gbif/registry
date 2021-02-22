/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.registry.domain.ws;

import org.gbif.api.model.common.DOI;
import org.gbif.api.util.HttpURI;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

public class DerivedDatasetCreationRequest implements Serializable {

  private DOI originalDownloadDOI;
  private String title;
  private String description;
  private URI sourceUrl;
  private Date registrationDate;
  private Map<String, Long> relatedDatasets = new HashMap<>();

  public DOI getOriginalDownloadDOI() {
    return originalDownloadDOI;
  }

  public void setOriginalDownloadDOI(DOI originalDownloadDOI) {
    this.originalDownloadDOI = originalDownloadDOI;
  }

  @NotNull
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @NotNull
  @HttpURI
  public URI getSourceUrl() {
    return sourceUrl;
  }

  public void setSourceUrl(URI sourceUrl) {
    this.sourceUrl = sourceUrl;
  }

  public Date getRegistrationDate() {
    return registrationDate;
  }

  public void setRegistrationDate(Date registrationDate) {
    this.registrationDate = registrationDate;
  }

  @JsonDeserialize(using = UniqueKeyRelatedDatasetsDeserializer.class)
  public Map<String, Long> getRelatedDatasets() {
    return relatedDatasets;
  }

  public void setRelatedDatasets(Map<String, Long> relatedDatasets) {
    this.relatedDatasets = relatedDatasets != null ? relatedDatasets : new HashMap<>();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DerivedDatasetCreationRequest that = (DerivedDatasetCreationRequest) o;
    return Objects.equals(originalDownloadDOI, that.originalDownloadDOI)
        && Objects.equals(title, that.title)
        && Objects.equals(description, that.description)
        && Objects.equals(sourceUrl, that.sourceUrl)
        && Objects.equals(registrationDate, that.registrationDate)
        && Objects.equals(relatedDatasets, that.relatedDatasets);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        originalDownloadDOI, title, description, sourceUrl, registrationDate, relatedDatasets);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", DerivedDatasetCreationRequest.class.getSimpleName() + "[", "]")
        .add("originalDownloadDOI=" + originalDownloadDOI)
        .add("title='" + title + "'")
        .add("description='" + description + "'")
        .add("sourceUrl=" + sourceUrl)
        .add("registrationDate=" + registrationDate)
        .add("relatedDatasets=" + relatedDatasets)
        .toString();
  }

  private static class UniqueKeyRelatedDatasetsDeserializer
      extends JsonDeserializer<Map<String, Long>> {

    @Override
    public Map<String, Long> deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException {
      ObjectCodec objectCodec = ctxt.getParser().getCodec();
      Map<String, Long> result;

      try {
        Map<String, Long> deserialized =
            objectCodec.readValue(jp, new TypeReference<UniqueKeyHashMap<String, Long>>() {});

        result = deserialized != null ? deserialized : new HashMap<>();
      } catch (IllegalArgumentException | MismatchedInputException e) {
        throw JsonMappingException.from(jp, e.getMessage(), e);
      }

      return result;
    }
  }

  // Throws exception if key is already present
  private static class UniqueKeyHashMap<K, V> extends HashMap<K, V> {

    @Override
    public V put(K key, V value) {
      if (containsKey(key)) {
        throw new IllegalArgumentException("Duplicate field " + key);
      }
      return super.put(key, value);
    }
  }
}
