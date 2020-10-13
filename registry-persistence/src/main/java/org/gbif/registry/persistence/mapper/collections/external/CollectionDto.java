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
package org.gbif.registry.persistence.mapper.collections.external;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class CollectionDto {

  private UUID collectionKey;
  private String collectionCode;
  private String collectionName;
  private Map<String, String> collectionAlternativeCodes = new HashMap<>();
  private String homepage;
  private String catalogUrl;
  private String description;
  private int numberSpecimens;
  private String taxonomicCoverage;
  private String geographicRange;

  private String contact;
  private String contactPosition;
  private String contactEmail;

  private String mailingAddress;
  private String mailingCity;
  private String mailingState;
  private String mailingZip;
  private String physicalAddress;
  private String physicalCity;
  private String physicalState;
  private String physicalZip;

  private UUID institutionKey;
  private String institutionCode;
  private String institutionName;
  private Map<String, String> institutionAlternativeCodes = new HashMap<>();
  private BigDecimal latitude;
  private BigDecimal longitude;
  private String uniqueNameUUID;

  public UUID getCollectionKey() {
    return collectionKey;
  }

  public void setCollectionKey(UUID collectionKey) {
    this.collectionKey = collectionKey;
  }

  public String getCollectionCode() {
    return collectionCode;
  }

  public void setCollectionCode(String collectionCode) {
    this.collectionCode = collectionCode;
  }

  public String getCollectionName() {
    return collectionName;
  }

  public void setCollectionName(String collectionName) {
    this.collectionName = collectionName;
  }

  public Map<String, String> getCollectionAlternativeCodes() {
    return collectionAlternativeCodes;
  }

  public void setCollectionAlternativeCodes(Map<String, String> collectionAlternativeCodes) {
    this.collectionAlternativeCodes = collectionAlternativeCodes;
  }

  public String getHomepage() {
    return homepage;
  }

  public void setHomepage(String homepage) {
    this.homepage = homepage;
  }

  public String getCatalogUrl() {
    return catalogUrl;
  }

  public void setCatalogUrl(String catalogUrl) {
    this.catalogUrl = catalogUrl;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public int getNumberSpecimens() {
    return numberSpecimens;
  }

  public void setNumberSpecimens(int numberSpecimens) {
    this.numberSpecimens = numberSpecimens;
  }

  public String getTaxonomicCoverage() {
    return taxonomicCoverage;
  }

  public void setTaxonomicCoverage(String taxonomicCoverage) {
    this.taxonomicCoverage = taxonomicCoverage;
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

  public String getContactPosition() {
    return contactPosition;
  }

  public void setContactPosition(String contactPosition) {
    this.contactPosition = contactPosition;
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

  public UUID getInstitutionKey() {
    return institutionKey;
  }

  public void setInstitutionKey(UUID institutionKey) {
    this.institutionKey = institutionKey;
  }

  public String getInstitutionCode() {
    return institutionCode;
  }

  public void setInstitutionCode(String institutionCode) {
    this.institutionCode = institutionCode;
  }

  public String getInstitutionName() {
    return institutionName;
  }

  public void setInstitutionName(String institutionName) {
    this.institutionName = institutionName;
  }

  public Map<String, String> getInstitutionAlternativeCodes() {
    return institutionAlternativeCodes;
  }

  public void setInstitutionAlternativeCodes(Map<String, String> institutionAlternativeCodes) {
    this.institutionAlternativeCodes = institutionAlternativeCodes;
  }

  public BigDecimal getLatitude() {
    return latitude;
  }

  public void setLatitude(BigDecimal latitude) {
    this.latitude = latitude;
  }

  public BigDecimal getLongitude() {
    return longitude;
  }

  public void setLongitude(BigDecimal longitude) {
    this.longitude = longitude;
  }

  public String getUniqueNameUUID() {
    return uniqueNameUUID;
  }

  public void setUniqueNameUUID(String uniqueNameUUID) {
    this.uniqueNameUUID = uniqueNameUUID;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CollectionDto that = (CollectionDto) o;
    return numberSpecimens == that.numberSpecimens
        && Objects.equals(collectionKey, that.collectionKey)
        && Objects.equals(collectionCode, that.collectionCode)
        && Objects.equals(collectionName, that.collectionName)
        && Objects.equals(collectionAlternativeCodes, that.collectionAlternativeCodes)
        && Objects.equals(homepage, that.homepage)
        && Objects.equals(catalogUrl, that.catalogUrl)
        && Objects.equals(description, that.description)
        && Objects.equals(taxonomicCoverage, that.taxonomicCoverage)
        && Objects.equals(geographicRange, that.geographicRange)
        && Objects.equals(contact, that.contact)
        && Objects.equals(contactPosition, that.contactPosition)
        && Objects.equals(contactEmail, that.contactEmail)
        && Objects.equals(mailingAddress, that.mailingAddress)
        && Objects.equals(mailingCity, that.mailingCity)
        && Objects.equals(mailingState, that.mailingState)
        && Objects.equals(mailingZip, that.mailingZip)
        && Objects.equals(physicalAddress, that.physicalAddress)
        && Objects.equals(physicalCity, that.physicalCity)
        && Objects.equals(physicalState, that.physicalState)
        && Objects.equals(physicalZip, that.physicalZip)
        && Objects.equals(institutionKey, that.institutionKey)
        && Objects.equals(institutionCode, that.institutionCode)
        && Objects.equals(institutionName, that.institutionName)
        && Objects.equals(institutionAlternativeCodes, that.institutionAlternativeCodes)
        && Objects.equals(latitude, that.latitude)
        && Objects.equals(longitude, that.longitude)
        && Objects.equals(uniqueNameUUID, that.uniqueNameUUID);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        collectionKey,
        collectionCode,
        collectionName,
        collectionAlternativeCodes,
        homepage,
        catalogUrl,
        description,
        numberSpecimens,
        taxonomicCoverage,
        geographicRange,
        contact,
        contactPosition,
        contactEmail,
        mailingAddress,
        mailingCity,
        mailingState,
        mailingZip,
        physicalAddress,
        physicalCity,
        physicalState,
        physicalZip,
        institutionKey,
        institutionCode,
        institutionName,
        institutionAlternativeCodes,
        latitude,
        longitude,
        uniqueNameUUID);
  }
}
