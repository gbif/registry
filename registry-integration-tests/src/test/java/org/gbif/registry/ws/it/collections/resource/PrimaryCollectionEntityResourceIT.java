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
import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.collections.Contactable;
import org.gbif.api.model.collections.OccurrenceMappeable;
import org.gbif.api.model.collections.OccurrenceMapping;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.collections.PrimaryCollectionEntity;
import org.gbif.api.model.collections.duplicates.Duplicate;
import org.gbif.api.model.collections.duplicates.DuplicatesRequest;
import org.gbif.api.model.collections.duplicates.DuplicatesResult;
import org.gbif.api.model.collections.merge.MergeParams;
import org.gbif.api.model.collections.suggestions.ApplySuggestionResult;
import org.gbif.api.model.collections.suggestions.ChangeSuggestion;
import org.gbif.api.model.collections.suggestions.ChangeSuggestionService;
import org.gbif.api.model.collections.suggestions.Status;
import org.gbif.api.model.collections.suggestions.Type;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Commentable;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.LenientEquals;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.service.collections.PrimaryCollectionEntityService;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.persistence.mapper.collections.params.DuplicatesSearchParams;
import org.gbif.registry.service.collections.duplicates.DuplicatesService;
import org.gbif.registry.service.collections.merge.MergeService;
import org.gbif.registry.ws.client.collections.BaseCollectionEntityClient;
import org.gbif.registry.ws.client.collections.PrimaryCollectionEntityClient;
import org.gbif.registry.ws.it.fixtures.RequestTestFixture;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public abstract class PrimaryCollectionEntityResourceIT<
        T extends
            PrimaryCollectionEntity & Taggable & MachineTaggable & Identifiable & Contactable
                & Commentable & OccurrenceMappeable & LenientEquals<T>,
        R extends ChangeSuggestion<T>>
    extends BaseCollectionEntityResourceIT<T> {

  public PrimaryCollectionEntityResourceIT(
      Class<? extends BaseCollectionEntityClient<T>> cls,
      SimplePrincipalProvider principalProvider,
      RequestTestFixture requestTestFixture,
      Class<T> paramType,
      int localServerPort) {
    super(cls, principalProvider, requestTestFixture, paramType, localServerPort);
  }

  @Test
  public void contactsTest() {
    // contacts
    Person person1 = new Person();
    person1.setFirstName("name1");
    person1.setKey(UUID.randomUUID());

    Person person2 = new Person();
    person2.setFirstName("name2");
    person2.setKey(UUID.randomUUID());

    // add contact
    doNothing().when(getMockPrimaryEntityService()).addContact(any(UUID.class), any(UUID.class));
    assertDoesNotThrow(
        () -> getPrimaryCollectionEntityClient().addContact(UUID.randomUUID(), UUID.randomUUID()));

    // list contacts
    when(getMockPrimaryEntityService().listContacts(any(UUID.class)))
        .thenReturn(Arrays.asList(person1, person2));
    List<Person> contactsEntity1 =
        getPrimaryCollectionEntityClient().listContacts(UUID.randomUUID());
    assertEquals(2, contactsEntity1.size());

    // remove contacts
    doNothing().when(getMockPrimaryEntityService()).removeContact(any(UUID.class), any(UUID.class));
    assertDoesNotThrow(
        () ->
            getPrimaryCollectionEntityClient().removeContact(UUID.randomUUID(), UUID.randomUUID()));
  }

  @Test
  public void contactPersonsTest() {
    // contacts
    Contact contact = new Contact();
    contact.setFirstName("name1");
    contact.setKey(1);

    Contact contact2 = new Contact();
    contact2.setFirstName("name2");
    contact2.setKey(2);

    // add contact
    doNothing().when(getMockPrimaryEntityService()).addContactPerson(any(UUID.class), any(Contact.class));
    assertDoesNotThrow(
      () -> getPrimaryCollectionEntityClient().addContactPerson(UUID.randomUUID(), contact));

    // list contacts
    when(getMockPrimaryEntityService().listContactPersons(any(UUID.class)))
      .thenReturn(Arrays.asList(contact, contact2));
    List<Contact> contactsEntity1 =
      getPrimaryCollectionEntityClient().listContactPersons(UUID.randomUUID());
    assertEquals(2, contactsEntity1.size());

    // update contact
    doNothing().when(getMockPrimaryEntityService()).updateContactPerson(any(UUID.class), any(Contact.class));
    assertDoesNotThrow(
      () ->
        getPrimaryCollectionEntityClient().updateContactPerson(UUID.randomUUID(), contact));

    // remove contacts
    doNothing().when(getMockPrimaryEntityService()).removeContactPerson(any(UUID.class), anyInt());
    assertDoesNotThrow(
      () ->
        getPrimaryCollectionEntityClient().removeContactPerson(UUID.randomUUID(), 1));
  }

  @Test
  public void getWithAddressTest() {
    // entities
    T entity = testData.newEntity();
    entity.setKey(UUID.randomUUID());

    // update adding address
    Address address = new Address();
    address.setAddress("address");
    address.setCountry(Country.AFGHANISTAN);
    address.setCity("city");
    entity.setAddress(address);

    Address mailingAddress = new Address();
    mailingAddress.setAddress("mailing address");
    mailingAddress.setCountry(Country.AFGHANISTAN);
    mailingAddress.setCity("city mailing");
    entity.setMailingAddress(mailingAddress);

    mockGetEntity(entity.getKey(), entity);
    T entityReturned = getPrimaryCollectionEntityClient().get(entity.getKey());

    assertNotNull(entityReturned.getAddress());
    assertEquals("address", entityReturned.getAddress().getAddress());
    assertEquals(Country.AFGHANISTAN, entityReturned.getAddress().getCountry());
    assertEquals("city", entityReturned.getAddress().getCity());
    assertNotNull(entityReturned.getMailingAddress());
    assertEquals("mailing address", entityReturned.getMailingAddress().getAddress());
    assertEquals(Country.AFGHANISTAN, entityReturned.getMailingAddress().getCountry());
    assertEquals("city mailing", entityReturned.getMailingAddress().getCity());
  }

  @Test
  public void occurrenceMappingsTest() {
    OccurrenceMapping occurrenceMapping = new OccurrenceMapping();
    occurrenceMapping.setCode("code");
    occurrenceMapping.setDatasetKey(UUID.randomUUID());

    int key = 1;
    when(getMockPrimaryEntityService().addOccurrenceMapping(any(UUID.class), eq(occurrenceMapping)))
        .thenReturn(key);
    int occurrenceMappingKey =
        getPrimaryCollectionEntityClient()
            .addOccurrenceMapping(UUID.randomUUID(), occurrenceMapping);
    assertEquals(key, occurrenceMappingKey);
    occurrenceMapping.setKey(occurrenceMappingKey);

    when(getMockPrimaryEntityService().listOccurrenceMappings(any(UUID.class)))
        .thenReturn(Collections.singletonList(occurrenceMapping));

    List<OccurrenceMapping> mappings =
        getPrimaryCollectionEntityClient().listOccurrenceMappings(UUID.randomUUID());
    assertEquals(1, mappings.size());

    doNothing()
        .when(getMockPrimaryEntityService())
        .deleteOccurrenceMapping(any(UUID.class), eq(occurrenceMappingKey));
    assertDoesNotThrow(
        () ->
            getPrimaryCollectionEntityClient()
                .deleteOccurrenceMapping(UUID.randomUUID(), occurrenceMappingKey));
  }

  @Test
  public void possibleDuplicatesTest() {
    DuplicatesResult result = new DuplicatesResult();

    Duplicate duplicate = new Duplicate();
    duplicate.setActive(true);
    duplicate.setInstitutionKey(UUID.randomUUID());
    duplicate.setMailingCountry(Country.DENMARK);
    result.setDuplicates(Collections.singletonList(Collections.singleton(duplicate)));
    result.setGenerationDate(LocalDateTime.now());

    when(getMockDuplicatesService().findPossibleDuplicates(any(DuplicatesSearchParams.class)))
        .thenReturn(result);

    DuplicatesRequest req = new DuplicatesRequest();
    req.setInInstitutions(Collections.singletonList(UUID.randomUUID()));
    req.setInCountries(
        Arrays.asList(Country.DENMARK.getIso2LetterCode(), Country.SPAIN.getIso2LetterCode()));
    req.setSameCode(true);
    DuplicatesResult clientResult = getPrimaryCollectionEntityClient().findPossibleDuplicates(req);
    assertEquals(result.getDuplicates().size(), clientResult.getDuplicates().size());
  }

  @Test
  public void mergeTest() {
    doNothing().when(getMockMergeService()).merge(any(UUID.class), any(UUID.class));

    MergeParams mergeParams = new MergeParams();
    mergeParams.setReplacementEntityKey(UUID.randomUUID());
    assertDoesNotThrow(
        () -> getPrimaryCollectionEntityClient().merge(UUID.randomUUID(), mergeParams));
  }

  @Test
  public void createChangeSuggestionTest() {
    int key = 1;
    when(getMockChangeSuggestionService().createChangeSuggestion(any())).thenReturn(key);

    assertEquals(
        key, getPrimaryCollectionEntityClient().createChangeSuggestion(newChangeSuggestion()));
  }

  @Test
  public void updateChangeSuggestionTest() {
    doNothing().when(getMockChangeSuggestionService()).updateChangeSuggestion(any());

    R changeSuggestion = newChangeSuggestion();
    changeSuggestion.setKey(1);
    assertDoesNotThrow(
        () -> getPrimaryCollectionEntityClient().updateChangeSuggestion(1, changeSuggestion));

    assertThrows(
        IllegalArgumentException.class,
        () -> getPrimaryCollectionEntityClient().updateChangeSuggestion(2, changeSuggestion));
  }

  protected PrimaryCollectionEntityClient<T, R> getPrimaryCollectionEntityClient() {
    return (PrimaryCollectionEntityClient<T, R>) baseClient;
  }

  @Test
  public void getChangeSuggestionTest() {
    R changeSuggestion = newChangeSuggestion();
    changeSuggestion.setKey(1);
    when(getMockChangeSuggestionService().getChangeSuggestion(anyInt()))
        .thenReturn(changeSuggestion);

    R changeSuggestionFetch =
        getPrimaryCollectionEntityClient().getChangeSuggestion(changeSuggestion.getKey());
    assertEquals(changeSuggestion, changeSuggestionFetch);
  }

  @Test
  public void listChangeSuggestionTest() {
    R changeSuggestion = newChangeSuggestion();
    changeSuggestion.setKey(1);
    Status status = Status.PENDING;
    Type type = Type.CREATE;
    String proposerEmail = "aa@aa.com";
    UUID entityKey = UUID.randomUUID();
    Pageable page = new PagingRequest();

    when(getMockChangeSuggestionService().list(status, type, proposerEmail, entityKey, page))
        .thenReturn(
            new PagingResponse<>(
                new PagingRequest(), 1L, Collections.singletonList(changeSuggestion)));

    PagingResponse<R> result =
        getPrimaryCollectionEntityClient()
            .listChangeSuggestion(status, type, proposerEmail, entityKey, page);
    assertEquals(1, result.getResults().size());
  }

  @Test
  public void applyChangeSuggestionTest() {
    UUID createdKey = UUID.randomUUID();
    when(getMockChangeSuggestionService().applyChangeSuggestion(anyInt())).thenReturn(createdKey);

    ApplySuggestionResult result = getPrimaryCollectionEntityClient().applyChangeSuggestion(1);
    assertEquals(createdKey, result.getEntityCreatedKey());
  }

  @Test
  public void discardChangeSuggestionTest() {
    doNothing().when(getMockChangeSuggestionService()).discardChangeSuggestion(anyInt());
    assertDoesNotThrow(() -> getPrimaryCollectionEntityClient().discardChangeSuggestion(1));
  }

  protected abstract PrimaryCollectionEntityService<T> getMockPrimaryEntityService();

  protected abstract DuplicatesService getMockDuplicatesService();

  protected abstract MergeService<T> getMockMergeService();

  protected abstract ChangeSuggestionService<T, R> getMockChangeSuggestionService();

  protected abstract R newChangeSuggestion();
}
