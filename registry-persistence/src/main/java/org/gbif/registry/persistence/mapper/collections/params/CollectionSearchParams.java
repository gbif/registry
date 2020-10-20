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

import org.gbif.api.vocabulary.IdentifierType;

import java.util.UUID;

import javax.annotation.Nullable;

public class CollectionSearchParams extends SearchParams {

  @Nullable UUID institutionKey;

  @Nullable
  public UUID getInstitutionKey() {
    return institutionKey;
  }

  public void setInstitutionKey(@Nullable UUID institutionKey) {
    this.institutionKey = institutionKey;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    UUID institutionKey;
    UUID contactKey;
    String query;
    String code;
    String name;
    String alternativeCode;
    String machineTagNamespace;
    String machineTagName;
    String machineTagValue;
    IdentifierType identifierType;
    String identifier;

    public Builder institutionKey(UUID institutionKey) {
      this.institutionKey = institutionKey;
      return this;
    }

    public Builder contactKey(UUID contactKey) {
      this.contactKey = contactKey;
      return this;
    }

    public Builder query(String query) {
      this.query = query;
      return this;
    }

    public Builder code(String code) {
      this.code = code;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder alternativeCode(String alternativeCode) {
      this.alternativeCode = alternativeCode;
      return this;
    }

    public Builder machineTagNamespace(String machineTagNamespace) {
      this.machineTagNamespace = machineTagNamespace;
      return this;
    }

    public Builder machineTagName(String machineTagName) {
      this.machineTagName = machineTagName;
      return this;
    }

    public Builder machineTagValue(String machineTagValue) {
      this.machineTagValue = machineTagValue;
      return this;
    }

    public Builder identifierType(IdentifierType identifierType) {
      this.identifierType = identifierType;
      return this;
    }

    public Builder identifier(String identifier) {
      this.identifier = identifier;
      return this;
    }

    public CollectionSearchParams build() {
      CollectionSearchParams params = new CollectionSearchParams();
      params.setInstitutionKey(institutionKey);
      params.setContactKey(contactKey);
      params.setQuery(query);
      params.setCode(code);
      params.setName(name);
      params.setAlternativeCode(alternativeCode);
      params.setMachineTagNamespace(machineTagNamespace);
      params.setMachineTagName(machineTagName);
      params.setMachineTagValue(machineTagValue);
      params.setIdentifier(identifier);
      params.setIdentifierType(identifierType);
      return params;
    }
  }
}
