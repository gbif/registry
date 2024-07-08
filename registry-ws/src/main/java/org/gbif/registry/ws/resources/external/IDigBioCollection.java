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
package org.gbif.registry.ws.resources.external;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public class IDigBioCollection implements Serializable {

  private UUID institutionKey;
  private UUID collectionKey;
  private String institution;
  private String collection;
  private String recordsets;
  private String recordsetQuery;

  @JsonProperty("institution_code")
  private String institutionCode;

  @JsonProperty("collection_code")
  private String collectionCode;

  @JsonProperty("collection_uuid")
  private String collectionUuid;

  @JsonProperty("collection_lsid")
  private String collectionLsid;

  @JsonProperty("collection_url")
  private String collectionUrl;

  // use collectionCatalogUrls instead
  @Deprecated
  @JsonProperty("collection_catalog_url")
  private String collectionCatalogUrl;

  @JsonProperty("collection_catalog_urls")
  private List<String> collectionCatalogUrls = new ArrayList<>();

  private String description;
  private int cataloguedSpecimens;
  private String taxonCoverage;

  @JsonProperty("geographic_range")
  private String geographicRange;

  private String contact;

  @JsonProperty("contact_role")
  private String contactRole;

  @JsonProperty("contact_email")
  private String contactEmail;

  @JsonProperty("mailing_address")
  private String mailingAddress;

  @JsonProperty("mailing_city")
  private String mailingCity;

  @JsonProperty("mailing_state")
  private String mailingState;

  @JsonProperty("mailing_zip")
  private String mailingZip;

  @JsonProperty("physical_address")
  private String physicalAddress;

  @JsonProperty("physical_city")
  private String physicalCity;

  @JsonProperty("physical_state")
  private String physicalState;

  @JsonProperty("physical_zip")
  private String physicalZip;

  @JsonProperty("UniqueNameUUID")
  private String uniqueNameUUID;

  private String sameAs;
  private BigDecimal lat;
  private BigDecimal lon;

  public UUID getInstitutionKey() {
    return institutionKey;
  }

  public void setInstitutionKey(UUID institutionKey) {
    this.institutionKey = institutionKey;
  }

  public UUID getCollectionKey() {
    return collectionKey;
  }

  public void setCollectionKey(UUID collectionKey) {
    this.collectionKey = collectionKey;
  }

  public String getInstitution() {
    return institution;
  }

  public void setInstitution(String institution) {
    this.institution = institution;
  }

  public String getCollection() {
    return collection;
  }

  public void setCollection(String collection) {
    this.collection = collection;
  }

  public String getRecordsets() {
    return recordsets;
  }

  public void setRecordsets(String recordsets) {
    this.recordsets = recordsets;
  }

  public String getRecordsetQuery() {
    return recordsetQuery;
  }

  public void setRecordsetQuery(String recordsetQuery) {
    this.recordsetQuery = recordsetQuery;
  }

  public String getInstitutionCode() {
    return institutionCode;
  }

  public void setInstitutionCode(String institutionCode) {
    this.institutionCode = institutionCode;
  }

  public String getCollectionCode() {
    return collectionCode;
  }

  public void setCollectionCode(String collectionCode) {
    this.collectionCode = collectionCode;
  }

  public String getCollectionUuid() {
    return collectionUuid;
  }

  public void setCollectionUuid(String collectionUuid) {
    this.collectionUuid = collectionUuid;
  }

  public String getCollectionLsid() {
    return collectionLsid;
  }

  public void setCollectionLsid(String collectionLsid) {
    this.collectionLsid = collectionLsid;
  }

  public String getCollectionUrl() {
    return collectionUrl;
  }

  public void setCollectionUrl(String collectionUrl) {
    this.collectionUrl = collectionUrl;
  }

  public String getCollectionCatalogUrl() {
    return collectionCatalogUrl;
  }

  public void setCollectionCatalogUrl(String collectionCatalogUrl) {
    this.collectionCatalogUrl = collectionCatalogUrl;
  }

  public List<String> getCollectionCatalogUrls() {
    return collectionCatalogUrls;
  }

  public void setCollectionCatalogUrls(List<String> collectionCatalogUrls) {
    this.collectionCatalogUrls = collectionCatalogUrls;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public int getCataloguedSpecimens() {
    return cataloguedSpecimens;
  }

  public void setCataloguedSpecimens(int cataloguedSpecimens) {
    this.cataloguedSpecimens = cataloguedSpecimens;
  }

  public String getTaxonCoverage() {
    return taxonCoverage;
  }

  public void setTaxonCoverage(String taxonCoverage) {
    this.taxonCoverage = taxonCoverage;
  }

  public String getGeographicRange() {
    return geographicRange;
  }

  public void setGeographicRange(String geographicRange) {
    this.geographicRange = geographicRange;
  }

  public String getContact() {
    return contact;
  }

  public void setContact(String contact) {
    this.contact = contact;
  }

  public String getContactRole() {
    return contactRole;
  }

  public void setContactRole(String contactRole) {
    this.contactRole = contactRole;
  }

  public String getContactEmail() {
    return contactEmail;
  }

  public void setContactEmail(String contactEmail) {
    this.contactEmail = contactEmail;
  }

  public String getMailingAddress() {
    return mailingAddress;
  }

  public void setMailingAddress(String mailingAddress) {
    this.mailingAddress = mailingAddress;
  }

  public String getMailingCity() {
    return mailingCity;
  }

  public void setMailingCity(String mailingCity) {
    this.mailingCity = mailingCity;
  }

  public String getMailingState() {
    return mailingState;
  }

  public void setMailingState(String mailingState) {
    this.mailingState = mailingState;
  }

  public String getMailingZip() {
    return mailingZip;
  }

  public void setMailingZip(String mailingZip) {
    this.mailingZip = mailingZip;
  }

  public String getPhysicalAddress() {
    return physicalAddress;
  }

  public void setPhysicalAddress(String physicalAddress) {
    this.physicalAddress = physicalAddress;
  }

  public String getPhysicalCity() {
    return physicalCity;
  }

  public void setPhysicalCity(String physicalCity) {
    this.physicalCity = physicalCity;
  }

  public String getPhysicalState() {
    return physicalState;
  }

  public void setPhysicalState(String physicalState) {
    this.physicalState = physicalState;
  }

  public String getPhysicalZip() {
    return physicalZip;
  }

  public void setPhysicalZip(String physicalZip) {
    this.physicalZip = physicalZip;
  }

  public String getUniqueNameUUID() {
    return uniqueNameUUID;
  }

  public void setUniqueNameUUID(String uniqueNameUUID) {
    this.uniqueNameUUID = uniqueNameUUID;
  }

  public String getSameAs() {
    return sameAs;
  }

  public void setSameAs(String sameAs) {
    this.sameAs = sameAs;
  }

  public BigDecimal getLat() {
    return lat;
  }

  public void setLat(BigDecimal lat) {
    this.lat = lat;
  }

  public BigDecimal getLon() {
    return lon;
  }

  public void setLon(BigDecimal lon) {
    this.lon = lon;
  }
}
