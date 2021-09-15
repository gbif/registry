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
package org.gbif.registry.ws.resources.collections;

import org.gbif.api.annotation.NullToNotFound;
import org.gbif.api.annotation.Trim;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.collections.request.PersonSearchRequest;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.search.collections.PersonSuggestResult;
import org.gbif.api.service.collections.PersonService;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.google.common.base.Preconditions.checkArgument;

@Deprecated
@RestController
@RequestMapping(value = "grscicoll/person", produces = MediaType.APPLICATION_JSON_VALUE)
public class PersonResource extends BaseCollectionEntityResource<Person> {

  private final PersonService personService;

  public PersonResource(PersonService personService) {
    super(Person.class, personService);
    this.personService = personService;
  }

  @GetMapping("{key}")
  @NullToNotFound("/grscicoll/person/{key}")
  public Person get(@PathVariable UUID key) {
    return personService.get(key);
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  public UUID create(@RequestBody @Trim Person person) {
    return personService.create(person);
  }

  @PutMapping(value = "{key}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  public void update(@PathVariable("key") UUID key, @RequestBody @Trim Person person) {
    checkArgument(key.equals(person.getKey()));
    personService.update(person);
  }

  @GetMapping
  public PagingResponse<Person> list(PersonSearchRequest searchRequest) {
    return personService.list(searchRequest);
  }

  @GetMapping("deleted")
  public PagingResponse<Person> listDeleted(Pageable page) {
    return personService.listDeleted(page);
  }

  @GetMapping("suggest")
  public List<PersonSuggestResult> suggest(@RequestParam(value = "q", required = false) String q) {
    return personService.suggest(q);
  }
}
