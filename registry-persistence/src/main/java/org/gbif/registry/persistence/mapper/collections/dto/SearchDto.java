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
package org.gbif.registry.persistence.mapper.collections.dto;

import org.gbif.api.vocabulary.Country;

import java.util.UUID;

public class SearchDto {

  private float score;
  private String type;
  private UUID key;
  private String code;
  private String name;
  private UUID institutionKey;
  private String institutionCode;
  private String institutionName;
  private boolean displayOnNHCPortal;
  private Country country;
  private Country mailCountry;

  private String codeHighlight;
  private String nameHighlight;
  private String descriptionHighlight;
  private String alternativeCodesHighlight;
  private String addressHighlight;
  private String cityHighlight;
  private String provinceHighlight;
  private String countryHighlight;
  private String mailAddressHighlight;
  private String mailCityHighlight;
  private String mailProvinceHighlight;
  private String mailCountryHighlight;

  private boolean similarityMatch;

  public float getScore() {
    return score;
  }

  public void setScore(float score) {
    this.score = score;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public UUID getKey() {
    return key;
  }

  public void setKey(UUID key) {
    this.key = key;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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

  public boolean getDisplayOnNHCPortal() {
    return displayOnNHCPortal;
  }

  public void setDisplayOnNHCPortal(boolean displayOnNHCPortal) {
    this.displayOnNHCPortal = displayOnNHCPortal;
  }

  public Country getCountry() {
    return country;
  }

  public void setCountry(Country country) {
    this.country = country;
  }

  public Country getMailCountry() {
    return mailCountry;
  }

  public void setMailCountry(Country mailCountry) {
    this.mailCountry = mailCountry;
  }

  public String getCodeHighlight() {
    return codeHighlight;
  }

  public void setCodeHighlight(String codeHighlight) {
    this.codeHighlight = codeHighlight;
  }

  public String getNameHighlight() {
    return nameHighlight;
  }

  public void setNameHighlight(String nameHighlight) {
    this.nameHighlight = nameHighlight;
  }

  public String getDescriptionHighlight() {
    return descriptionHighlight;
  }

  public void setDescriptionHighlight(String descriptionHighlight) {
    this.descriptionHighlight = descriptionHighlight;
  }

  public String getAlternativeCodesHighlight() {
    return alternativeCodesHighlight;
  }

  public void setAlternativeCodesHighlight(String alternativeCodesHighlight) {
    this.alternativeCodesHighlight = alternativeCodesHighlight;
  }

  public String getAddressHighlight() {
    return addressHighlight;
  }

  public void setAddressHighlight(String addressHighlight) {
    this.addressHighlight = addressHighlight;
  }

  public String getCityHighlight() {
    return cityHighlight;
  }

  public void setCityHighlight(String cityHighlight) {
    this.cityHighlight = cityHighlight;
  }

  public String getProvinceHighlight() {
    return provinceHighlight;
  }

  public void setProvinceHighlight(String provinceHighlight) {
    this.provinceHighlight = provinceHighlight;
  }

  public String getCountryHighlight() {
    return countryHighlight;
  }

  public void setCountryHighlight(String countryHighlight) {
    this.countryHighlight = countryHighlight;
  }

  public String getMailAddressHighlight() {
    return mailAddressHighlight;
  }

  public void setMailAddressHighlight(String mailAddressHighlight) {
    this.mailAddressHighlight = mailAddressHighlight;
  }

  public String getMailCityHighlight() {
    return mailCityHighlight;
  }

  public void setMailCityHighlight(String mailCityHighlight) {
    this.mailCityHighlight = mailCityHighlight;
  }

  public String getMailProvinceHighlight() {
    return mailProvinceHighlight;
  }

  public void setMailProvinceHighlight(String mailProvinceHighlight) {
    this.mailProvinceHighlight = mailProvinceHighlight;
  }

  public String getMailCountryHighlight() {
    return mailCountryHighlight;
  }

  public void setMailCountryHighlight(String mailCountryHighlight) {
    this.mailCountryHighlight = mailCountryHighlight;
  }

  public boolean isSimilarityMatch() {
    return similarityMatch;
  }

  public void setSimilarityMatch(boolean similarityMatch) {
    this.similarityMatch = similarityMatch;
  }
}
