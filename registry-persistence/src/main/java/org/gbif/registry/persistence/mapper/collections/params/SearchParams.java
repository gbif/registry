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
package org.gbif.registry.persistence.mapper.collections.params;

import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;

import java.util.UUID;

import javax.annotation.Nullable;

public abstract class SearchParams {

  @Nullable UUID contactKey;
  @Nullable String query;
  @Nullable String code;
  @Nullable String name;
  @Nullable String alternativeCode;
  @Nullable String machineTagNamespace;
  @Nullable String machineTagName;
  @Nullable String machineTagValue;
  @Nullable IdentifierType identifierType;
  @Nullable String identifier;
  @Nullable Country country;

  @Nullable
  public UUID getContactKey() {
    return contactKey;
  }

  public void setContactKey(@Nullable UUID contactKey) {
    this.contactKey = contactKey;
  }

  @Nullable
  public String getQuery() {
    return query;
  }

  public void setQuery(@Nullable String query) {
    this.query = query;
  }

  @Nullable
  public String getCode() {
    return code;
  }

  public void setCode(@Nullable String code) {
    this.code = code;
  }

  @Nullable
  public String getName() {
    return name;
  }

  public void setName(@Nullable String name) {
    this.name = name;
  }

  @Nullable
  public String getAlternativeCode() {
    return alternativeCode;
  }

  public void setAlternativeCode(@Nullable String alternativeCode) {
    this.alternativeCode = alternativeCode;
  }

  @Nullable
  public String getMachineTagNamespace() {
    return machineTagNamespace;
  }

  public void setMachineTagNamespace(@Nullable String machineTagNamespace) {
    this.machineTagNamespace = machineTagNamespace;
  }

  @Nullable
  public String getMachineTagName() {
    return machineTagName;
  }

  public void setMachineTagName(@Nullable String machineTagName) {
    this.machineTagName = machineTagName;
  }

  @Nullable
  public String getMachineTagValue() {
    return machineTagValue;
  }

  public void setMachineTagValue(@Nullable String machineTagValue) {
    this.machineTagValue = machineTagValue;
  }

  @Nullable
  public IdentifierType getIdentifierType() {
    return identifierType;
  }

  public void setIdentifierType(@Nullable IdentifierType identifierType) {
    this.identifierType = identifierType;
  }

  @Nullable
  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(@Nullable String identifier) {
    this.identifier = identifier;
  }

  @Nullable
  public Country getCountry() {
    return country;
  }

  public void setCountry(@Nullable Country country) {
    this.country = country;
  }
}
