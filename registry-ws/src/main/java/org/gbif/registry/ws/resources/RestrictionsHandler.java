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
package org.gbif.registry.ws.resources;

import org.gbif.api.model.registry.Organization;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.ws.WebApplicationException;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Utility class to restrict updates of data.
 */
@Component
public class RestrictionsHandler {

  private final List<String> denyCountries;

  private final OrganizationMapper organizationMapper;

  @Autowired
  public RestrictionsHandler(
    @Value("${registry.denyCountries}") List<String> denyCountries, OrganizationMapper organizationMapper
  ) {
    this.denyCountries = denyCountries;
    this.organizationMapper = organizationMapper;
  }

  /**
   * Is the country in the list of denials.
   */
  public void checkCountryDenied(Country country) {
    if (country != null && denyCountries != null && (denyCountries.contains(country.getIso2LetterCode()) || denyCountries.contains(country.getIso3LetterCode()))) {
      throw new WebApplicationException("Illegal entity data", HttpStatus.FORBIDDEN);
    }
  }

  /**
   * Is the organization is the list of denials.
   */
  public void checkDenyPublisher(UUID organizationKey) {
    if (organizationKey != null) {
      Organization organization = organizationMapper.get(organizationKey);
      if (organization != null) {
        checkCountryDenied(organization.getCountry());
      }
    }
  }

  /**
   * Is the organization is the list of denials.
   */
  public void checkDenyPublisher(String organizationKey) {
    if (organizationKey != null) {
      checkDenyPublisher(UUID.fromString(organizationKey));
    }
  }
}
