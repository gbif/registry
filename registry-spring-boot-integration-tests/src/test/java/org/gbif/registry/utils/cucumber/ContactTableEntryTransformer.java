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
package org.gbif.registry.utils.cucumber;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.Country;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import io.cucumber.datatable.TableEntryTransformer;

public class ContactTableEntryTransformer implements TableEntryTransformer<Contact> {

  @Override
  public Contact transform(Map<String, String> entry) {
    Contact contact = new Contact();
    Optional.ofNullable(entry.get("key")).map(Integer::parseInt).ifPresent(contact::setKey);
    Optional.ofNullable(entry.get("primary"))
        .map(Boolean::parseBoolean)
        .ifPresent(contact::setPrimary);
    Optional.ofNullable(entry.get("position"))
        .map(Collections::singletonList)
        .ifPresent(contact::setPosition);
    contact.setDescription(entry.get("description"));
    contact.setOrganization(entry.get("organization"));
    contact.setCity(entry.get("city"));
    contact.setProvince(entry.get("province"));
    Optional.ofNullable(entry.get("country")).map(Country::valueOf).ifPresent(contact::setCountry);
    contact.setPostalCode(entry.get("postalCode"));
    contact.setCreatedBy(entry.get("createdBy"));
    contact.setModifiedBy(entry.get("modifiedBy"));
    Optional.ofNullable(entry.get("type")).map(ContactType::inferType).ifPresent(contact::setType);
    Optional.ofNullable(entry.get("email"))
        .map(Collections::singletonList)
        .ifPresent(contact::setEmail);
    contact.setFirstName(entry.get("firstName"));
    contact.setLastName(entry.get("lastName"));
    Optional.ofNullable(entry.get("address"))
        .map(Collections::singletonList)
        .ifPresent(contact::setAddress);
    Optional.ofNullable(entry.get("phone"))
        .map(Collections::singletonList)
        .ifPresent(contact::setPhone);
    Optional.ofNullable(entry.get("primary"))
        .map(Boolean::parseBoolean)
        .ifPresent(contact::setPrimary);

    return contact;
  }
}
