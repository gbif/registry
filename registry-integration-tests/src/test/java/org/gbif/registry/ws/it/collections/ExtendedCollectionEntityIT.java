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
package org.gbif.registry.ws.it.collections;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Contactable;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.api.service.collections.ContactService;
import org.gbif.api.service.collections.CrudService;
import org.gbif.api.service.collections.PersonService;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.identity.service.IdentityService;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.client.collections.PersonClient;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.gbif.ws.security.KeyStore;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JavaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public abstract class ExtendedCollectionEntityIT<
        T extends CollectionEntity & Taggable & MachineTaggable & Identifiable & Contactable>
    extends BaseCollectionEntityIT<T> {

  private final PersonService personResource;
  private final PersonService personClient;

  // query params
  private static final String CONTACT_PARAM = "contact";

  private static final JavaType LIST_KEY_CODE_NAME_TYPE =
      OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, KeyCodeNameResult.class);

  public ExtendedCollectionEntityIT(
      CrudService<T> resource,
      Class<? extends CrudService<T>> cls,
      PersonService personResource,
      MockMvc mockMvc,
      SimplePrincipalProvider principalProvider,
      EsManageServer esServer,
      IdentityService identityService,
      Class<T> clazz,
      int localServerPort,
      KeyStore keyStore) {
    super(
        resource,
        cls,
        mockMvc,
        principalProvider,
        esServer,
        identityService,
        clazz,
        localServerPort,
        keyStore);
    this.personResource = personResource;
    this.personClient = prepareClient(localServerPort, keyStore, PersonClient.class);
  }

  // TODO: 10/05/2020 move createWithAddressTest to PersonIT
  // TODO: 10/05/2020 move listByContactTest to PersonIT and InstitutionIT

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void contactsTest(ServiceType serviceType) {
    CrudService<T> service = getService(serviceType);
    ContactService contactService = (ContactService) service;
    PersonService personService = getService(serviceType, personResource, personClient);

    // entities
    UUID entityKey1 = service.create(newEntity());
    UUID entityKey2 = service.create(newEntity());
    UUID entityKey3 = service.create(newEntity());

    // contacts
    Person person1 = new Person();
    person1.setFirstName("name1");
    UUID personKey1 = personService.create(person1);

    Person person2 = new Person();
    person2.setFirstName("name2");
    UUID personKey2 = personService.create(person2);

    // add contacts
    contactService.addContact(entityKey1, personKey1);
    contactService.addContact(entityKey1, personKey2);
    contactService.addContact(entityKey2, personKey2);

    // list contacts
    List<Person> contactsEntity1 = contactService.listContacts(entityKey1);
    assertEquals(2, contactsEntity1.size());

    List<Person> contactsEntity2 = contactService.listContacts(entityKey2);
    assertEquals(1, contactsEntity2.size());
    assertEquals("name2", contactsEntity2.get(0).getFirstName());

    assertEquals(0, contactService.listContacts(entityKey3).size());

    // remove contacts
    contactService.removeContact(entityKey1, personKey2);
    contactsEntity1 = contactService.listContacts(entityKey1);
    assertEquals(1, contactsEntity1.size());
    assertEquals("name1", contactsEntity1.get(0).getFirstName());

    contactService.removeContact(entityKey2, personKey2);
    assertEquals(0, contactService.listContacts(entityKey2).size());
  }

  // TODO: 10/05/2020 client exception
  @ParameterizedTest
  @EnumSource(
      value = ServiceType.class,
      names = {"RESOURCE"})
  public void duplicateContactTest(ServiceType serviceType) {
    CrudService<T> service = getService(serviceType);
    ContactService contactService = (ContactService) service;
    PersonService personService = getService(serviceType, personResource, personClient);

    // entities
    UUID entityKey1 = service.create(newEntity());

    // contacts
    Person person1 = new Person();
    person1.setFirstName("name1");
    UUID personKey1 = personService.create(person1);

    // add one contact
    contactService.addContact(entityKey1, personKey1);
    assertThrows(RuntimeException.class, () -> contactService.addContact(entityKey1, personKey1));
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void updateAddressesTest(ServiceType serviceType) {
    CrudService<T> service = getService(serviceType);

    // entities
    T entity = newEntity();
    UUID entityKey = service.create(entity);
    assertNewEntity(entity);
    entity = service.get(entityKey);

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

    service.update(entity);
    entity = service.get(entityKey);
    address = entity.getAddress();
    mailingAddress = entity.getMailingAddress();

    assertNotNull(entity.getAddress());
    assertNotNull(entity.getAddress().getKey());
    assertEquals("address", entity.getAddress().getAddress());
    assertEquals(Country.AFGHANISTAN, entity.getAddress().getCountry());
    assertEquals("city", entity.getAddress().getCity());
    assertNotNull(entity.getMailingAddress());
    assertNotNull(entity.getMailingAddress().getKey());
    assertEquals("mailing address", entity.getMailingAddress().getAddress());
    assertEquals(Country.AFGHANISTAN, entity.getMailingAddress().getCountry());
    assertEquals("city mailing", entity.getMailingAddress().getCity());
    assertNotNull(address);
    assertNotNull(mailingAddress);

    // update address
    address.setAddress("address2");
    mailingAddress.setAddress("mailing address2");

    service.update(entity);
    entity = service.get(entityKey);
    assertNotNull(entity.getAddress());
    assertEquals("address2", entity.getAddress().getAddress());
    assertNotNull(entity.getMailingAddress());
    assertEquals("mailing address2", entity.getMailingAddress().getAddress());

    // delete address
    entity.setAddress(null);
    entity.setMailingAddress(null);
    service.update(entity);
    entity = service.get(entityKey);
    assertNull(entity.getAddress());
    assertNull(entity.getMailingAddress());
  }
}
