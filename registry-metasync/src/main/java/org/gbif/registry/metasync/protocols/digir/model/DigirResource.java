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
package org.gbif.registry.metasync.protocols.digir.model;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.digester3.annotations.rules.BeanPropertySetter;
import org.apache.commons.digester3.annotations.rules.CallMethod;
import org.apache.commons.digester3.annotations.rules.CallParam;
import org.apache.commons.digester3.annotations.rules.ObjectCreate;
import org.apache.commons.digester3.annotations.rules.SetNext;
import org.joda.time.DateTime;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

@ObjectCreate(pattern = "response/content/metadata/provider/resource")
public class DigirResource {

  private final Set<URI> relatedInformation = Sets.newHashSet();
  private final List<DigirContact> contacts = Lists.newArrayList();

  @BeanPropertySetter(pattern = "response/content/metadata/provider/resource/name")
  private String name;

  @BeanPropertySetter(pattern = "response/content/metadata/provider/resource/code")
  private String code;
  /** Called {@code abstract} in DiGIR, which is a reserved word in Java. */
  @BeanPropertySetter(pattern = "response/content/metadata/provider/resource/abstract")
  private String description;

  @BeanPropertySetter(pattern = "response/content/metadata/provider/resource/keywords")
  private String keywords;

  @BeanPropertySetter(pattern = "response/content/metadata/provider/resource/citation")
  private String citation;

  @BeanPropertySetter(pattern = "response/content/metadata/provider/resource/useRestrictions")
  private String useRestrictions;
  /** Maps from namespace to schema location. */
  private Map<String, URI> conceptualSchemas = Maps.newHashMap();

  @BeanPropertySetter(pattern = "response/content/metadata/provider/resource/recordIdentifier")
  private String recordIdentifier;

  @BeanPropertySetter(pattern = "response/content/metadata/provider/resource/recordBasis")
  private String recordBasis;

  @BeanPropertySetter(pattern = "response/content/metadata/provider/resource/numberOfRecords")
  private int numberOfRecords;

  @BeanPropertySetter(pattern = "response/content/metadata/provider/resource/dateLastUpdated")
  private DateTime dateLastUpdated;

  @BeanPropertySetter(pattern = "response/content/metadata/provider/resource/minQueryTermLength")
  private int minQueryTermLength;

  @BeanPropertySetter(
      pattern = "response/content/metadata/provider/resource/maxSearchResponseRecords")
  private int maxSearchResponseRecords;

  @BeanPropertySetter(
      pattern = "response/content/metadata/provider/resource/maxInventoryResponseRecords")
  private int maxInventoryResponseRecords;

  @CallMethod(pattern = "response/content/metadata/provider/resource/relatedInformation")
  public void addRelatedInformation(
      @CallParam(pattern = "response/content/metadata/provider/resource/relatedInformation")
          URI relatedInformation) {
    this.relatedInformation.add(relatedInformation);
  }

  @SetNext
  public void addContact(DigirResourceContact contact) {
    contacts.add(contact);
  }

  @CallMethod(pattern = "response/content/metadata/provider/resource/conceptualSchema")
  public void addConceptualSchema(
      @CallParam(pattern = "response/content/metadata/provider/resource/conceptualSchema")
          String namespace,
      @CallParam(
              pattern = "response/content/metadata/provider/resource/conceptualSchema",
              attributeName = "schemaLocation")
          URI schemaLocation) {
    conceptualSchemas.put(namespace, schemaLocation);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public Set<URI> getRelatedInformation() {
    return relatedInformation;
  }

  public List<DigirContact> getContacts() {
    return contacts;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getKeywords() {
    return keywords;
  }

  public void setKeywords(String keywords) {
    this.keywords = keywords;
  }

  public String getCitation() {
    return citation;
  }

  public void setCitation(String citation) {
    this.citation = citation;
  }

  public String getUseRestrictions() {
    return useRestrictions;
  }

  public void setUseRestrictions(String useRestrictions) {
    this.useRestrictions = useRestrictions;
  }

  public Map<String, URI> getConceptualSchemas() {
    return conceptualSchemas;
  }

  public void setConceptualSchemas(Map<String, URI> conceptualSchemas) {
    this.conceptualSchemas = conceptualSchemas;
  }

  public String getRecordIdentifier() {
    return recordIdentifier;
  }

  public void setRecordIdentifier(String recordIdentifier) {
    this.recordIdentifier = recordIdentifier;
  }

  public String getRecordBasis() {
    return recordBasis;
  }

  public void setRecordBasis(String recordBasis) {
    this.recordBasis = recordBasis;
  }

  public int getNumberOfRecords() {
    return numberOfRecords;
  }

  public void setNumberOfRecords(int numberOfRecords) {
    this.numberOfRecords = numberOfRecords;
  }

  public DateTime getDateLastUpdated() {
    return dateLastUpdated;
  }

  public void setDateLastUpdated(DateTime dateLastUpdated) {
    this.dateLastUpdated = dateLastUpdated;
  }

  public int getMinQueryTermLength() {
    return minQueryTermLength;
  }

  public void setMinQueryTermLength(int minQueryTermLength) {
    this.minQueryTermLength = minQueryTermLength;
  }

  public int getMaxSearchResponseRecords() {
    return maxSearchResponseRecords;
  }

  public void setMaxSearchResponseRecords(int maxSearchResponseRecords) {
    this.maxSearchResponseRecords = maxSearchResponseRecords;
  }

  public int getMaxInventoryResponseRecords() {
    return maxInventoryResponseRecords;
  }

  public void setMaxInventoryResponseRecords(int maxInventoryResponseRecords) {
    this.maxInventoryResponseRecords = maxInventoryResponseRecords;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("name", name)
        .add("code", code)
        .add("relatedInformation", relatedInformation)
        .add("contacts", contacts)
        .add("description", description)
        .add("keywords", keywords)
        .add("citation", citation)
        .add("useRestrictions", useRestrictions)
        .add("conceptualSchemas", conceptualSchemas)
        .add("recordIdentifier", recordIdentifier)
        .add("recordBasis", recordBasis)
        .add("numberOfRecords", numberOfRecords)
        .add("dateLastUpdated", dateLastUpdated)
        .add("minQueryTermLength", minQueryTermLength)
        .add("maxSearchResponseRecords", maxSearchResponseRecords)
        .add("maxInventoryResponseRecords", maxInventoryResponseRecords)
        .toString();
  }
}
