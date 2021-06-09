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
package org.gbif.registry.ws.it.collections.resource;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.collections.request.PersonSearchRequest;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.search.collections.PersonSuggestResult;
import org.gbif.api.service.collections.CollectionEntityService;
import org.gbif.api.service.collections.PersonService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.ws.client.collections.PersonClient;
import org.gbif.registry.ws.it.fixtures.RequestTestFixture;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class PersonResourceIT extends BaseCollectionEntityResourceIT<Person> {

  @MockBean private PersonService personService;

  @Autowired
  public PersonResourceIT(
      SimplePrincipalProvider simplePrincipalProvider,
      RequestTestFixture requestTestFixture,
      @LocalServerPort int localServerPort) {
    super(
        PersonClient.class,
        simplePrincipalProvider,
        requestTestFixture,
        Person.class,
        localServerPort);
  }

  @Test
  public void listTest() {
    Person p1 = testData.newEntity();
    Person p2 = testData.newEntity();
    List<Person> persons = Arrays.asList(p1, p2);

    when(personService.list(any(PersonSearchRequest.class)))
        .thenReturn(
            new PagingResponse<>(new PagingRequest(), Long.valueOf(persons.size()), persons));

    PagingResponse<Person> result =
        getClient()
            .list(
                PersonSearchRequest.builder()
                    .query("foo")
                    .primaryInstitution(UUID.randomUUID())
                    .primaryCollection(UUID.randomUUID())
                    .build());
    assertEquals(persons.size(), result.getResults().size());
  }

  @Test
  public void testSuggest() {
    PersonSuggestResult r1 = new PersonSuggestResult(UUID.randomUUID(), "n1", "ln1", "aa@aa.com");
    PersonSuggestResult r2 = new PersonSuggestResult(UUID.randomUUID(), "n2", "ln2", "aa2@aa.com");
    List<PersonSuggestResult> results = Arrays.asList(r1, r2);

    when(personService.suggest(anyString())).thenReturn(results);
    assertEquals(2, getClient().suggest("foo").size());
  }

  @Test
  public void listDeletedTest() {
    Person p1 = testData.newEntity();
    p1.setKey(UUID.randomUUID());
    p1.setFirstName("name");

    Person p2 = testData.newEntity();
    p2.setKey(UUID.randomUUID());
    p2.setFirstName("name2");

    List<Person> persons = Arrays.asList(p1, p2);

    when(personService.listDeleted(any(Pageable.class)))
        .thenReturn(
            new PagingResponse<>(new PagingRequest(), Long.valueOf(persons.size()), persons));

    PagingResponse<Person> result = getClient().listDeleted(new PagingRequest());
    assertEquals(persons.size(), result.getResults().size());
  }

  @Test
  public void getPersonWithAddressTest() {
    Person person = testData.newEntity();
    person.setKey(UUID.randomUUID());

    Address mailingAddress = new Address();
    mailingAddress.setAddress("mailing");
    mailingAddress.setCity("city");
    mailingAddress.setCountry(Country.AFGHANISTAN);
    person.setMailingAddress(mailingAddress);

    Identifier identifier = new Identifier();
    identifier.setIdentifier("id");
    identifier.setType(IdentifierType.IH_IRN);
    person.setIdentifiers(Collections.singletonList(identifier));

    mockGetEntity(person.getKey(), person);
    Person personSaved = getClient().get(person.getKey());

    assertTrue(personSaved.lenientEquals(person));
    assertNotNull(personSaved.getMailingAddress());
    assertEquals("mailing", personSaved.getMailingAddress().getAddress());
    assertEquals("city", personSaved.getMailingAddress().getCity());
    assertEquals(Country.AFGHANISTAN, personSaved.getMailingAddress().getCountry());
    assertThat(1, greaterThanOrEqualTo(personSaved.getIdentifiers().size()));
    assertEquals("id", personSaved.getIdentifiers().get(0).getIdentifier());
    assertEquals(IdentifierType.IH_IRN, personSaved.getIdentifiers().get(0).getType());
  }

  protected PersonClient getClient() {
    return (PersonClient) baseClient;
  }

  @Override
  protected CollectionEntityService<Person> getMockBaseService() {
    return personService;
  }
}
