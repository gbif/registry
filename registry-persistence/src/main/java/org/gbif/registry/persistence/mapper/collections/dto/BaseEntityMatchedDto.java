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

import lombok.Setter;

@Setter
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

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getCode() {
    return code;
  }

  @Override
  public Country getAddressCountry() {
    return addressCountry;
  }

  @Override
  public Country getMailingAddressCountry() {
    return mailingAddressCountry;
  }

  @Override
  public boolean isActive() {
    return active;
  }

  @Override
  public boolean isKeyMatch() {
    return keyMatch;
  }

  @Override
  public boolean isCodeMatch() {
    return codeMatch;
  }

  @Override
  public boolean isIdentifierMatch() {
    return identifierMatch;
  }

  @Override
  public boolean isAlternativeCodeMatch() {
    return alternativeCodeMatch;
  }

  @Override
  public boolean isNameMatchWithCode() {
    return nameMatchWithCode;
  }

  @Override
  public boolean isNameMatchWithIdentifier() {
    return nameMatchWithIdentifier;
  }

  @Override
  public boolean isExplicitMapping() {
    return explicitMapping;
  }
}
