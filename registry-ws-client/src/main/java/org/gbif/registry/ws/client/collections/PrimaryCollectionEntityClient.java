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
import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.collections.Contactable;
import org.gbif.api.model.collections.OccurrenceMapping;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.collections.duplicates.DuplicatesRequest;
import org.gbif.api.model.collections.duplicates.DuplicatesResult;
import org.gbif.api.model.collections.merge.MergeParams;
import org.gbif.api.model.collections.suggestions.ApplySuggestionResult;
import org.gbif.api.model.collections.suggestions.ChangeSuggestion;
import org.gbif.api.model.collections.suggestions.Status;
import org.gbif.api.model.collections.suggestions.Type;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.Taggable;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

public interface PrimaryCollectionEntityClient<
        T extends CollectionEntity & Taggable & Identifiable & MachineTaggable & Contactable,
        R extends ChangeSuggestion<T>>
    extends BaseCollectionEntityClient<T> {

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}/contact",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  List<Person> listContacts(@PathVariable("key") UUID key);

  @RequestMapping(
      method = RequestMethod.POST,
      value = "{key}/contact",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  void addContact(@PathVariable("key") UUID key, @RequestBody UUID personKey);

  @RequestMapping(method = RequestMethod.DELETE, value = "{key}/contact/{personKey}")
  void removeContact(@PathVariable("key") UUID key, @PathVariable("personKey") UUID personKey);

  @RequestMapping(
      method = RequestMethod.POST,
      value = "{key}/contactPerson",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  void addContactPerson(@PathVariable("key") UUID entityKey, @RequestBody Contact contact);

  @RequestMapping(
      method = RequestMethod.PUT,
      value = "{key}/contactPerson",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  void updateContactPerson(@PathVariable("key") UUID entityKey, @RequestBody Contact contact);

  @RequestMapping(method = RequestMethod.DELETE, value = "{key}/contactPerson/{contactKey}")
  void removeContactPerson(
      @PathVariable("key") UUID entityKey, @PathVariable("contactKey") int contactKey);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}/contactPerson",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  List<Contact> listContactPersons(@PathVariable("key") UUID key);

  @RequestMapping(
      method = RequestMethod.POST,
      value = "{key}/occurrenceMapping",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  int addOccurrenceMapping(
      @PathVariable("key") UUID key, @RequestBody OccurrenceMapping occurrenceMapping);

  @RequestMapping(
      method = RequestMethod.DELETE,
      value = "{key}/occurrenceMapping/{occurrenceMappingKey}")
  void deleteOccurrenceMapping(
      @PathVariable("key") UUID key,
      @PathVariable("occurrenceMappingKey") int occurrenceMappingKey);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}/occurrenceMapping",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  List<OccurrenceMapping> listOccurrenceMappings(@PathVariable("key") UUID key);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "possibleDuplicates",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  DuplicatesResult findPossibleDuplicates(@SpringQueryMap DuplicatesRequest request);

  @RequestMapping(
      method = RequestMethod.POST,
      value = "{key}/merge",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  void merge(@PathVariable("key") UUID entityKey, @RequestBody MergeParams params);

  @RequestMapping(
      method = RequestMethod.POST,
      value = "changeSuggestion",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  int createChangeSuggestion(@RequestBody R createSuggestion);

  @RequestMapping(
      method = RequestMethod.PUT,
      value = "changeSuggestion/{key}",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  void updateChangeSuggestion(@PathVariable("key") int key, @RequestBody R suggestion);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "changeSuggestion/{key}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  R getChangeSuggestion(@PathVariable("key") int key);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "changeSuggestion",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  PagingResponse<R> listChangeSuggestion(
      @RequestParam(value = "status", required = false) Status status,
      @RequestParam(value = "type", required = false) Type type,
      @RequestParam(value = "proposerEmail", required = false) String proposerEmail,
      @RequestParam(value = "entityKey", required = false) UUID entityKey,
      @SpringQueryMap Pageable page);

  @RequestMapping(method = RequestMethod.PUT, value = "changeSuggestion/{key}/discard")
  void discardChangeSuggestion(@PathVariable("key") int key);

  @RequestMapping(
      method = RequestMethod.PUT,
      value = "changeSuggestion/{key}/apply",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  ApplySuggestionResult applyChangeSuggestion(@PathVariable("key") int key);
}
