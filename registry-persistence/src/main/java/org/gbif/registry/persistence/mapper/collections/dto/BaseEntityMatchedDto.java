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

import java.util.UUID;

public class BaseEntityMatchedDto implements EntityMatchedDto {

  private UUID key;
  private String name;
  private String code;
  private Country addressCountry;
  private Country mailingAddressCountry;
  private boolean keyMatch;
  private boolean codeMatch;
  private boolean identifierMatch;
  private boolean alternativeCodeMatch;
  private boolean nameMatchWithCode;
  private boolean nameMatchWithIdentifier;
  private boolean explicitMapping;
  private boolean active;

  @Override
  public UUID getKey() {
    return key;
  }

  public void setKey(UUID key) {
    this.key = key;
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  @Override
  public Country getAddressCountry() {
    return addressCountry;
  }

  public void setAddressCountry(Country addressCountry) {
    this.addressCountry = addressCountry;
  }

  @Override
  public Country getMailingAddressCountry() {
    return mailingAddressCountry;
  }

  public void setMailingAddressCountry(Country mailingAddressCountry) {
    this.mailingAddressCountry = mailingAddressCountry;
  }

  @Override
  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  @Override
  public boolean isKeyMatch() {
    return keyMatch;
  }

  public void setKeyMatch(boolean keyMatch) {
    this.keyMatch = keyMatch;
  }

  @Override
  public boolean isCodeMatch() {
    return codeMatch;
  }

  public void setCodeMatch(boolean codeMatch) {
    this.codeMatch = codeMatch;
  }

  @Override
  public boolean isIdentifierMatch() {
    return identifierMatch;
  }

  public void setIdentifierMatch(boolean identifierMatch) {
    this.identifierMatch = identifierMatch;
  }

  @Override
  public boolean isAlternativeCodeMatch() {
    return alternativeCodeMatch;
  }

  public void setAlternativeCodeMatch(boolean alternativeCodeMatch) {
    this.alternativeCodeMatch = alternativeCodeMatch;
  }

  @Override
  public boolean isNameMatchWithCode() {
    return nameMatchWithCode;
  }

  public void setNameMatchWithCode(boolean nameMatchWithCode) {
    this.nameMatchWithCode = nameMatchWithCode;
  }

  @Override
  public boolean isNameMatchWithIdentifier() {
    return nameMatchWithIdentifier;
  }

  public void setNameMatchWithIdentifier(boolean nameMatchWithIdentifier) {
    this.nameMatchWithIdentifier = nameMatchWithIdentifier;
  }

  @Override
  public boolean isExplicitMapping() {
    return explicitMapping;
  }

  public void setExplicitMapping(boolean explicitMapping) {
    this.explicitMapping = explicitMapping;
  }
}
