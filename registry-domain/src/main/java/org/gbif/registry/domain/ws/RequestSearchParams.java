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

import org.gbif.api.vocabulary.IdentifierType;

import javax.annotation.Nullable;

public abstract class RequestSearchParams {

  private IdentifierType identifierType;
  private String identifier;
  private String machineTagNamespace; // namespace
  private String machineTagName; // name
  private String machineTagValue; // value
  private String q; // query

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
  public String getQ() {
    return q;
  }

  public void setQ(@Nullable String q) {
    this.q = q;
  }
}
