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
package org.gbif.registry.persistence.mapper.collections.dto;

import org.gbif.api.vocabulary.Country;

import java.time.LocalDateTime;
import java.util.StringJoiner;
import java.util.UUID;

public class DuplicateDto {

  private UUID key1;
  private String code1;
  private String name1;
  private Country physicalCountry1;
  private String physicalCity1;
  private Country mailingCountry1;
  private String mailingCity1;
  private LocalDateTime created1;
  private LocalDateTime modified1;
  private UUID key2;
  private String code2;
  private String name2;
  private Country physicalCountry2;
  private String physicalCity2;
  private Country mailingCountry2;
  private String mailingCity2;
  private LocalDateTime created2;
  private LocalDateTime modified2;
  private LocalDateTime generatedDate;
  private boolean codeMatch;
  private boolean nameMatch;
  private boolean cityMatch;
  private boolean countryMatch;
  private boolean fuzzyNameMatch;

  // only for collections
  private UUID institutionKey1;
  private UUID institutionKey2;
  private boolean institutionKeyMatch;

  public UUID getKey1() {
    return key1;
  }

  public void setKey1(UUID key1) {
    this.key1 = key1;
  }

  public String getCode1() {
    return code1;
  }

  public void setCode1(String code1) {
    this.code1 = code1;
  }

  public String getName1() {
    return name1;
  }

  public void setName1(String name1) {
    this.name1 = name1;
  }

  public Country getPhysicalCountry1() {
    return physicalCountry1;
  }

  public void setPhysicalCountry1(Country physicalCountry1) {
    this.physicalCountry1 = physicalCountry1;
  }

  public String getPhysicalCity1() {
    return physicalCity1;
  }

  public void setPhysicalCity1(String physicalCity1) {
    this.physicalCity1 = physicalCity1;
  }

  public Country getMailingCountry1() {
    return mailingCountry1;
  }

  public void setMailingCountry1(Country mailingCountry1) {
    this.mailingCountry1 = mailingCountry1;
  }

  public String getMailingCity1() {
    return mailingCity1;
  }

  public void setMailingCity1(String mailingCity1) {
    this.mailingCity1 = mailingCity1;
  }

  public LocalDateTime getCreated1() {
    return created1;
  }

  public void setCreated1(LocalDateTime created1) {
    this.created1 = created1;
  }

  public LocalDateTime getModified1() {
    return modified1;
  }

  public void setModified1(LocalDateTime modified1) {
    this.modified1 = modified1;
  }

  public UUID getKey2() {
    return key2;
  }

  public void setKey2(UUID key2) {
    this.key2 = key2;
  }

  public String getCode2() {
    return code2;
  }

  public void setCode2(String code2) {
    this.code2 = code2;
  }

  public String getName2() {
    return name2;
  }

  public void setName2(String name2) {
    this.name2 = name2;
  }

  public Country getPhysicalCountry2() {
    return physicalCountry2;
  }

  public void setPhysicalCountry2(Country physicalCountry2) {
    this.physicalCountry2 = physicalCountry2;
  }

  public String getPhysicalCity2() {
    return physicalCity2;
  }

  public void setPhysicalCity2(String physicalCity2) {
    this.physicalCity2 = physicalCity2;
  }

  public Country getMailingCountry2() {
    return mailingCountry2;
  }

  public void setMailingCountry2(Country mailingCountry2) {
    this.mailingCountry2 = mailingCountry2;
  }

  public String getMailingCity2() {
    return mailingCity2;
  }

  public void setMailingCity2(String mailingCity2) {
    this.mailingCity2 = mailingCity2;
  }

  public LocalDateTime getCreated2() {
    return created2;
  }

  public void setCreated2(LocalDateTime created2) {
    this.created2 = created2;
  }

  public LocalDateTime getModified2() {
    return modified2;
  }

  public void setModified2(LocalDateTime modified2) {
    this.modified2 = modified2;
  }

  public LocalDateTime getGeneratedDate() {
    return generatedDate;
  }

  public void setGeneratedDate(LocalDateTime generatedDate) {
    this.generatedDate = generatedDate;
  }

  public boolean isCodeMatch() {
    return codeMatch;
  }

  public void setCodeMatch(boolean codeMatch) {
    this.codeMatch = codeMatch;
  }

  public boolean isNameMatch() {
    return nameMatch;
  }

  public void setNameMatch(boolean nameMatch) {
    this.nameMatch = nameMatch;
  }

  public boolean isCityMatch() {
    return cityMatch;
  }

  public void setCityMatch(boolean cityMatch) {
    this.cityMatch = cityMatch;
  }

  public boolean isCountryMatch() {
    return countryMatch;
  }

  public void setCountryMatch(boolean countryMatch) {
    this.countryMatch = countryMatch;
  }

  public boolean isFuzzyNameMatch() {
    return fuzzyNameMatch;
  }

  public void setFuzzyNameMatch(boolean fuzzyNameMatch) {
    this.fuzzyNameMatch = fuzzyNameMatch;
  }

  public UUID getInstitutionKey1() {
    return institutionKey1;
  }

  public void setInstitutionKey1(UUID institutionKey1) {
    this.institutionKey1 = institutionKey1;
  }

  public UUID getInstitutionKey2() {
    return institutionKey2;
  }

  public void setInstitutionKey2(UUID institutionKey2) {
    this.institutionKey2 = institutionKey2;
  }

  public boolean isInstitutionKeyMatch() {
    return institutionKeyMatch;
  }

  public void setInstitutionKeyMatch(boolean institutionKeyMatch) {
    this.institutionKeyMatch = institutionKeyMatch;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", DuplicateDto.class.getSimpleName() + "[", "]")
        .add("key1=" + key1)
        .add("code1='" + code1 + "'")
        .add("name1='" + name1 + "'")
        .add("physicalCountry1=" + physicalCountry1)
        .add("physicalCity1='" + physicalCity1 + "'")
        .add("mailingCountry1=" + mailingCountry1)
        .add("mailingCity1='" + mailingCity1 + "'")
        .add("created1=" + created1)
        .add("modified1=" + modified1)
        .add("key2=" + key2)
        .add("code2='" + code2 + "'")
        .add("name2='" + name2 + "'")
        .add("physicalCountry2=" + physicalCountry2)
        .add("physicalCity2='" + physicalCity2 + "'")
        .add("mailingCountry2=" + mailingCountry2)
        .add("mailingCity2='" + mailingCity2 + "'")
        .add("created2=" + created2)
        .add("modified2=" + modified2)
        .add("generatedDate=" + generatedDate)
        .add("codeMatch=" + codeMatch)
        .add("nameMatch=" + nameMatch)
        .add("cityMatch=" + cityMatch)
        .add("countryMatch=" + countryMatch)
        .add("fuzzyNameMatch=" + fuzzyNameMatch)
        .add("institutionKey1=" + institutionKey1)
        .add("institutionKey2=" + institutionKey2)
        .add("institutionKeyMatch=" + institutionKeyMatch)
        .toString();
  }
}
