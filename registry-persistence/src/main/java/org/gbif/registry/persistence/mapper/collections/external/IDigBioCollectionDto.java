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
package org.gbif.registry.persistence.mapper.collections.external;

import org.gbif.api.model.collections.AlternativeCode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IDigBioCollectionDto {

  private UUID collectionKey;
  private String collectionCode;
  private String collectionName;
  private List<AlternativeCode> collectionAlternativeCodes = new ArrayList<>();
  private String homepage;
  private List<String> catalogUrls;
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
  private List<AlternativeCode> institutionAlternativeCodes = new ArrayList<>();
  private BigDecimal latitude;
  private BigDecimal longitude;
  private String uniqueNameUUID;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    IDigBioCollectionDto that = (IDigBioCollectionDto) o;
    return numberSpecimens == that.numberSpecimens
        && Objects.equals(collectionKey, that.collectionKey)
        && Objects.equals(collectionCode, that.collectionCode)
        && Objects.equals(collectionName, that.collectionName)
        && Objects.equals(collectionAlternativeCodes, that.collectionAlternativeCodes)
        && Objects.equals(homepage, that.homepage)
        && Objects.equals(catalogUrls, that.catalogUrls)
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
        catalogUrls,
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
