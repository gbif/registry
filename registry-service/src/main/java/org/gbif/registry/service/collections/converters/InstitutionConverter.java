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
package org.gbif.registry.service.collections.converters;

import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.vocabulary.ContactType;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import static org.gbif.registry.service.collections.converters.ConverterUtils.convertAddress;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InstitutionConverter {

  public static Institution convertFromOrganization(
      Organization organization, String institutionCode) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(institutionCode));
    Institution institution = new Institution();
    institution.setCode(institutionCode);
    return convertFromOrganization(organization, institution);
  }

  public static Institution convertFromOrganization(
      Organization organization, Institution existingInstitution) {
    Objects.requireNonNull(organization);
    Objects.requireNonNull(existingInstitution);

    existingInstitution.setName(organization.getTitle());
    existingInstitution.setDescription(organization.getDescription());

    if (organization.getHomepage() != null && !organization.getHomepage().isEmpty()) {
      existingInstitution.setHomepage(organization.getHomepage().get(0));
    }

    existingInstitution.setPhone(organization.getPhone());
    existingInstitution.setEmail(organization.getEmail());
    existingInstitution.setLatitude(organization.getLatitude());
    existingInstitution.setLongitude(organization.getLongitude());
    existingInstitution.setLogoUrl(organization.getLogoUrl());
    existingInstitution.setActive(true);

    existingInstitution.setAddress(convertAddress(organization, existingInstitution.getAddress()));

    // contacts
    List<Contact> collectionContacts =
        organization.getContacts().stream()
            .filter(
                c ->
                    c.getType() != ContactType.PROGRAMMER
                        && c.getType() != ContactType.METADATA_AUTHOR)
            .map(ConverterUtils::datasetContactToCollectionsContact)
            .collect(Collectors.toList());
    existingInstitution.setContactPersons(collectionContacts);

    return existingInstitution;
  }
}
