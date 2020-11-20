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
package org.gbif.registry.ws.client.collections;

import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Contactable;
import org.gbif.api.model.collections.OccurrenceMapping;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.service.collections.ContactService;
import org.gbif.api.service.collections.OccurrenceMappingService;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

public interface ExtendedBaseCollectionEntityClient<
        T extends CollectionEntity & Taggable & Identifiable & MachineTaggable & Contactable>
    extends BaseCollectionEntityClient<T>, ContactService, OccurrenceMappingService {

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}/contact",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  List<Person> listContacts(@PathVariable("key") UUID key);

  @RequestMapping(
      method = RequestMethod.POST,
      value = "{key}/contact",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  void addContact(@PathVariable("key") UUID key, @RequestBody UUID personKey);

  @RequestMapping(method = RequestMethod.DELETE, value = "{key}/contact/{personKey}")
  @Override
  void removeContact(@PathVariable("key") UUID key, @PathVariable("personKey") UUID personKey);

  @RequestMapping(
      method = RequestMethod.POST,
      value = "{key}/occurrenceMapping",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  int addOccurrenceMapping(
      @PathVariable("key") UUID key, @RequestBody OccurrenceMapping occurrenceMapping);

  @RequestMapping(
      method = RequestMethod.DELETE,
      value = "{key}/occurrenceMapping/{occurrenceMappingKey}")
  @Override
  void deleteOccurrenceMapping(
      @PathVariable("key") UUID key,
      @PathVariable("occurrenceMappingKey") int occurrenceMappingKey);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}/occurrenceMapping",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  List<OccurrenceMapping> listOccurrenceMappings(@PathVariable("key") UUID key);
}
