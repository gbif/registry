/*
 * Copyright 2020-2021 Global Biodiversity Information Facility (GBIF)
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
package org.gbif.registry.ws.it.collections.service;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.AlternativeCode;
import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.collections.IdType;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.collections.UserId;
import org.gbif.api.model.collections.request.InstitutionSearchRequest;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.service.collections.PersonService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.collections.Discipline;
import org.gbif.api.vocabulary.collections.InstitutionGovernance;
import org.gbif.api.vocabulary.collections.InstitutionType;
import org.gbif.registry.identity.service.IdentityService;
import org.gbif.registry.service.collections.duplicates.InstitutionDuplicatesService;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the {@link InstitutionService}. */
public class InstitutionServiceIT extends PrimaryCollectionEntityServiceIT<Institution> {

  private final InstitutionService institutionService;

  @Autowired
  public InstitutionServiceIT(
      InstitutionService institutionService,
      PersonService personService,
      DatasetService datasetService,
      NodeService nodeService,
      OrganizationService organizationService,
      InstallationService installationService,
      SimplePrincipalProvider principalProvider,
      IdentityService identityService,
      InstitutionDuplicatesService duplicatesService) {
    super(
        institutionService,
        personService,
        datasetService,
        nodeService,
        organizationService,
        installationService,
        principalProvider,
        identityService,
        duplicatesService,
        Institution.class);
    this.institutionService = institutionService;
  }

  @Test
  public void listTest() {
    Institution institution1 = testData.newEntity();
    institution1.setCode("c1");
    institution1.setName("n1");
    institution1.setActive(true);
    institution1.setType(InstitutionType.HERBARIUM);
    institution1.setInstitutionalGovernance(InstitutionGovernance.ACADEMIC_FEDERAL);
    institution1.setDisciplines(Collections.singletonList(Discipline.OCEAN));
    Address address = new Address();
    address.setAddress("dummy address");
    address.setCity("city");
    address.setCountry(Country.DENMARK);
    institution1.setAddress(address);
    institution1.setAlternativeCodes(Collections.singletonList(new AlternativeCode("alt", "test")));
    UUID key1 = institutionService.create(institution1);

    Institution institution2 = testData.newEntity();
    institution2.setCode("c2");
    institution2.setName("n2");
    institution2.setActive(false);
    institution2.setDisciplines(Arrays.asList(Discipline.OCEAN, Discipline.AGRICULTURAL));
    Address address2 = new Address();
    address2.setAddress("dummy address2");
    address2.setCity("city2");
    address2.setCountry(Country.SPAIN);
    institution2.setAddress(address2);
    UUID key2 = institutionService.create(institution2);

    PagingResponse<Institution> response =
        institutionService.list(
            InstitutionSearchRequest.builder().query("dummy").page(DEFAULT_PAGE).build());
    assertEquals(2, response.getResults().size());

    // empty queries are ignored and return all elements
    response =
        institutionService.list(
            InstitutionSearchRequest.builder().query("").page(DEFAULT_PAGE).build());
    assertEquals(2, response.getResults().size());

    response =
        institutionService.list(
            InstitutionSearchRequest.builder().query("city").page(DEFAULT_PAGE).build());
    assertEquals(1, response.getResults().size());
    assertEquals(key1, response.getResults().get(0).getKey());

    response =
        institutionService.list(
            InstitutionSearchRequest.builder().query("city2").page(DEFAULT_PAGE).build());
    assertEquals(1, response.getResults().size());
    assertEquals(key2, response.getResults().get(0).getKey());

    // code and name params
    assertEquals(
        1,
        institutionService
            .list(InstitutionSearchRequest.builder().code("c1").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        1,
        institutionService
            .list(InstitutionSearchRequest.builder().name("n2").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        1,
        institutionService
            .list(
                InstitutionSearchRequest.builder().code("c1").name("n1").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        0,
        institutionService
            .list(
                InstitutionSearchRequest.builder().code("c2").name("n1").page(DEFAULT_PAGE).build())
            .getResults()
            .size());

    // query param
    assertEquals(
        2,
        institutionService
            .list(InstitutionSearchRequest.builder().query("c").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        2,
        institutionService
            .list(InstitutionSearchRequest.builder().query("dum add").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        0,
        institutionService
            .list(InstitutionSearchRequest.builder().query("<").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        0,
        institutionService
            .list(InstitutionSearchRequest.builder().query("\"<\"").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        2,
        institutionService
            .list(InstitutionSearchRequest.builder().page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        2,
        institutionService
            .list(InstitutionSearchRequest.builder().query("  ").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        1,
        institutionService
            .list(InstitutionSearchRequest.builder().active(true).page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        1,
        institutionService
            .list(
                InstitutionSearchRequest.builder()
                    .type(InstitutionType.HERBARIUM)
                    .page(DEFAULT_PAGE)
                    .build())
            .getResults()
            .size());
    assertEquals(
        1,
        institutionService
            .list(
                InstitutionSearchRequest.builder()
                    .institutionalGovernance(InstitutionGovernance.ACADEMIC_FEDERAL)
                    .page(DEFAULT_PAGE)
                    .build())
            .getResults()
            .size());
    assertEquals(
        2,
        institutionService
            .list(
                InstitutionSearchRequest.builder()
                    .disciplines(Collections.singletonList(Discipline.OCEAN))
                    .page(DEFAULT_PAGE)
                    .build())
            .getResults()
            .size());
    assertEquals(
        1,
        institutionService
            .list(
                InstitutionSearchRequest.builder()
                    .disciplines(Arrays.asList(Discipline.OCEAN, Discipline.AGRICULTURAL))
                    .page(DEFAULT_PAGE)
                    .build())
            .getResults()
            .size());

    // alternative code
    response =
        institutionService.list(
            InstitutionSearchRequest.builder().alternativeCode("alt").page(DEFAULT_PAGE).build());
    assertEquals(1, response.getResults().size());

    response =
        institutionService.list(
            InstitutionSearchRequest.builder().alternativeCode("foo").page(DEFAULT_PAGE).build());
    assertEquals(0, response.getResults().size());

    response =
        institutionService.list(
            InstitutionSearchRequest.builder().country(Country.SPAIN).page(DEFAULT_PAGE).build());
    assertEquals(1, response.getResults().size());
    assertEquals(key2, response.getResults().get(0).getKey());
    response =
        institutionService.list(
            InstitutionSearchRequest.builder()
                .country(Country.AFGHANISTAN)
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(0, response.getResults().size());

    response =
        institutionService.list(
            InstitutionSearchRequest.builder().city("city2").page(DEFAULT_PAGE).build());
    assertEquals(1, response.getResults().size());
    assertEquals(key2, response.getResults().get(0).getKey());
    response =
        institutionService.list(
            InstitutionSearchRequest.builder().city("foo").page(DEFAULT_PAGE).build());
    assertEquals(0, response.getResults().size());

    // update address
    institution2 = institutionService.get(key2);
    assertNotNull(institution2.getAddress());
    institution2.getAddress().setCity("city3");
    institutionService.update(institution2);
    assertEquals(
        1,
        institutionService
            .list(InstitutionSearchRequest.builder().query("city3").page(DEFAULT_PAGE).build())
            .getResults()
            .size());

    institutionService.delete(key2);
    assertEquals(
        0,
        institutionService
            .list(InstitutionSearchRequest.builder().query("city3").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
  }

  @Test
  public void testSuggest() {
    Institution institution1 = testData.newEntity();
    institution1.setCode("II");
    institution1.setName("Institution name");
    institutionService.create(institution1);

    Institution institution2 = testData.newEntity();
    institution2.setCode("II2");
    institution2.setName("Institution name2");
    institutionService.create(institution2);

    assertEquals(2, institutionService.suggest("institution").size());
    assertEquals(2, institutionService.suggest("II").size());
    assertEquals(1, institutionService.suggest("II2").size());
    assertEquals(1, institutionService.suggest("name2").size());
  }

  @Test
  public void listDeletedTest() {
    Institution institution1 = testData.newEntity();
    institution1.setCode("code1");
    institution1.setName("Institution name");
    UUID key1 = institutionService.create(institution1);

    Institution institution2 = testData.newEntity();
    institution2.setCode("code2");
    institution2.setName("Institution name2");
    UUID key2 = institutionService.create(institution2);

    assertEquals(0, institutionService.listDeleted(null, DEFAULT_PAGE).getResults().size());

    institutionService.delete(key1);
    assertEquals(1, institutionService.listDeleted(null, DEFAULT_PAGE).getResults().size());

    institutionService.delete(key2);
    assertEquals(2, institutionService.listDeleted(null, DEFAULT_PAGE).getResults().size());

    Institution institution3 = testData.newEntity();
    institution3.setCode("code3");
    institution3.setName("Institution name");
    UUID key3 = institutionService.create(institution3);

    Institution institution4 = testData.newEntity();
    institution4.setCode("code4");
    institution4.setName("Institution name4");
    UUID key4 = institutionService.create(institution4);

    assertEquals(0, institutionService.listDeleted(key4, DEFAULT_PAGE).getResults().size());
    institutionService.replace(key3, key4);
    assertEquals(1, institutionService.listDeleted(key4, DEFAULT_PAGE).getResults().size());
  }

  @Test
  public void listWithoutParametersTest() {
    Institution institution1 = testData.newEntity();
    institutionService.create(institution1);

    Institution institution2 = testData.newEntity();
    institutionService.create(institution2);

    Institution institution3 = testData.newEntity();
    UUID key3 = institutionService.create(institution3);

    PagingResponse<Institution> response =
        institutionService.list(InstitutionSearchRequest.builder().page(DEFAULT_PAGE).build());
    assertEquals(3, response.getResults().size());

    institutionService.delete(key3);

    response =
        institutionService.list(InstitutionSearchRequest.builder().page(DEFAULT_PAGE).build());
    assertEquals(2, response.getResults().size());

    response =
        institutionService.list(
            InstitutionSearchRequest.builder().page(new PagingRequest(0L, 1)).build());
    assertEquals(1, response.getResults().size());

    response =
        institutionService.list(
            InstitutionSearchRequest.builder().page(new PagingRequest(0L, 0)).build());
    assertEquals(0, response.getResults().size());
  }

  @Test
  public void listByContactTest() {
    // persons
    Person person1 = new Person();
    person1.setFirstName("first name");
    UUID personKey1 = personService.create(person1);

    Person person2 = new Person();
    person2.setFirstName("first name2");
    UUID personKey2 = personService.create(person2);

    // institutions
    Institution institution1 = testData.newEntity();
    UUID instutionKey1 = institutionService.create(institution1);

    Institution institution2 = testData.newEntity();
    UUID instutionKey2 = institutionService.create(institution2);

    // add contacts
    institutionService.addContact(instutionKey1, personKey1);
    institutionService.addContact(instutionKey1, personKey2);
    institutionService.addContact(instutionKey2, personKey2);

    assertEquals(
        1,
        institutionService
            .list(InstitutionSearchRequest.builder().contact(personKey1).page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        2,
        institutionService
            .list(InstitutionSearchRequest.builder().contact(personKey2).page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        0,
        institutionService
            .list(
                InstitutionSearchRequest.builder()
                    .contact(UUID.randomUUID())
                    .page(DEFAULT_PAGE)
                    .build())
            .getResults()
            .size());

    institutionService.removeContact(instutionKey1, personKey2);
    assertEquals(
        1,
        institutionService
            .list(InstitutionSearchRequest.builder().contact(personKey2).page(DEFAULT_PAGE).build())
            .getResults()
            .size());
  }

  @Test
  public void contactPersonsTest() {
    Institution institution1 = testData.newEntity();
    UUID institutionKey1 = institutionService.create(institution1);

    Contact contact = new Contact();
    contact.setFirstName("First name");
    contact.setLastName("last");
    contact.setCountry(Country.AFGHANISTAN);
    contact.setAddress(Collections.singletonList("address"));
    contact.setCity("City");
    contact.setEmail(Collections.singletonList("aa@aa.com"));
    contact.setFax(Collections.singletonList("fdsgds"));
    contact.setPhone(Collections.singletonList("fdsgds"));
    contact.setNotes("notes");

    institutionService.addContactPerson(institutionKey1, contact);

    List<Contact> contacts = institutionService.listContactPersons(institutionKey1);
    assertEquals(1, contacts.size());

    Contact contactCreated = contacts.get(0);
    assertTrue(contactCreated.lenientEquals(contact));

    contactCreated.setPosition(Collections.singletonList("position"));
    contactCreated.setTaxonomicExpertise(Collections.singletonList("aves"));

    UserId userId = new UserId();
    userId.setId("id");
    userId.setType(IdType.OTHER);
    contactCreated.setUserIds(Collections.singletonList(userId));
    institutionService.updateContactPerson(institutionKey1, contactCreated);

    contacts = institutionService.listContactPersons(institutionKey1);
    assertEquals(1, contacts.size());

    Contact contactUpdated = contacts.get(0);
    assertTrue(contactUpdated.lenientEquals(contactCreated));

    UserId userId2 = new UserId();
    userId2.setId("id");
    userId2.setType(IdType.HUH);
    contactUpdated.getUserIds().add(userId2);
    assertThrows(
      IllegalArgumentException.class,
      () -> institutionService.updateContactPerson(institutionKey1, contactUpdated));

    Contact contact2 = new Contact();
    contact2.setFirstName("Another name");
    contact2.setTaxonomicExpertise(Arrays.asList("aves", "funghi"));
    contact2.setPosition(Collections.singletonList("Curator"));

    UserId userId3 = new UserId();
    userId3.setId("id");
    userId3.setType(IdType.OTHER);

    UserId userId4 = new UserId();
    userId4.setId("12426");
    userId4.setType(IdType.IH_IRN);
    contact2.setUserIds(Arrays.asList(userId3, userId4));

    institutionService.addContactPerson(institutionKey1, contact2);
    contacts = institutionService.listContactPersons(institutionKey1);
    assertEquals(2, contacts.size());

    institutionService.removeContactPerson(institutionKey1, contactCreated.getKey());
    contacts = institutionService.listContactPersons(institutionKey1);
    assertEquals(1, contacts.size());

    contact = contacts.get(0);
    assertTrue(contact.lenientEquals(contact2));
  }

  @Test
  public void createInstitutionWithoutCodeTest() {
    Institution i = testData.newEntity();
    i.setCode(null);
    assertThrows(ValidationException.class, () -> institutionService.create(i));
  }

  @Test
  public void updateInstitutionWithoutCodeTest() {
    Institution i = testData.newEntity();
    UUID key = institutionService.create(i);

    Institution created = institutionService.get(key);
    created.setCode(null);
    assertThrows(IllegalArgumentException.class, () -> institutionService.update(created));
  }

  @Test
  public void updateAndReplaceTest() {
    Institution i = testData.newEntity();
    UUID key = institutionService.create(i);

    Institution created = institutionService.get(key);
    created.setReplacedBy(UUID.randomUUID());
    assertThrows(IllegalArgumentException.class, () -> institutionService.update(created));

    created.setReplacedBy(null);
    created.setConvertedToCollection(UUID.randomUUID());
    assertThrows(IllegalArgumentException.class, () -> institutionService.update(created));
  }

  @Test
  public void possibleDuplicatesTest() {
    testDuplicatesCommonCases();
  }

  @Test
  public void invalidEmailsTest() {
    Institution institution = new Institution();
    institution.setCode("cc");
    institution.setName("n1");
    institution.setEmail(Collections.singletonList("asfs"));

    assertThrows(ConstraintViolationException.class, () -> institutionService.create(institution));

    institution.setEmail(Collections.singletonList("aa@aa.com"));
    UUID key = institutionService.create(institution);
    Institution institutionCreated = institutionService.get(key);

    institutionCreated.getEmail().add("asfs");
    assertThrows(
        ConstraintViolationException.class, () -> institutionService.update(institutionCreated));
  }
}
