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
import org.gbif.api.model.collections.OccurrenceMappeable;
import org.gbif.api.model.collections.OccurrenceMapping;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.collections.duplicates.Duplicate;
import org.gbif.api.model.collections.duplicates.DuplicatesRequest;
import org.gbif.api.model.collections.duplicates.DuplicatesResult;
import org.gbif.api.model.registry.Commentable;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.service.collections.ContactService;
import org.gbif.api.service.collections.CrudService;
import org.gbif.api.service.collections.OccurrenceMappingService;
import org.gbif.api.service.collections.PersonService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.InstallationType;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.License;
import org.gbif.api.vocabulary.NodeType;
import org.gbif.api.vocabulary.ParticipationStatus;
import org.gbif.registry.identity.service.IdentityService;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.client.collections.ExtendedBaseCollectionEntityClient;
import org.gbif.registry.ws.client.collections.PersonClient;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.gbif.ws.security.KeyStore;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class ExtendedCollectionEntityIT<
        T extends
            CollectionEntity & Taggable & MachineTaggable & Identifiable & Contactable & Commentable
                & OccurrenceMappeable>
    extends BaseCollectionEntityIT<T> {

  protected final PersonService personResource;
  protected final PersonService personClient;
  protected final DatasetService datasetService;
  private final NodeService nodeService;
  private final OrganizationService organizationService;
  private final InstallationService installationService;

  public ExtendedCollectionEntityIT(
      CrudService<T> resource,
      Class<? extends CrudService<T>> cls,
      PersonService personResource,
      DatasetService datasetService,
      NodeService nodeService,
      OrganizationService organizationService,
      InstallationService installationService,
      SimplePrincipalProvider principalProvider,
      EsManageServer esServer,
      IdentityService identityService,
      int localServerPort,
      KeyStore keyStore) {
    super(resource, cls, principalProvider, esServer, identityService, localServerPort, keyStore);
    this.personResource = personResource;
    this.personClient = prepareClient(localServerPort, keyStore, PersonClient.class);
    this.datasetService = datasetService;
    this.nodeService = nodeService;
    this.organizationService = organizationService;
    this.installationService = installationService;
  }

  @BeforeAll
  public static void createMaterializedViews() throws SQLException {
    Connection connection = database.getTestDatabase().getConnection();
    // create materialized view for testing
    ScriptUtils.executeSqlScript(
        connection, new ClassPathResource("/scripts/create_duplicates_views.sql"));
    connection.close();
  }

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

  @ParameterizedTest
  @EnumSource(ServiceType.class)
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

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void occurrenceMappingsTest(ServiceType serviceType) {
    CrudService<T> service = getService(serviceType);
    OccurrenceMappingService occurrenceMappingService = (OccurrenceMappingService) service;

    T entity = newEntity();
    UUID entityKey = service.create(entity);

    Dataset dataset = createDataset();
    OccurrenceMapping occurrenceMapping = new OccurrenceMapping();
    occurrenceMapping.setCode("code");
    occurrenceMapping.setDatasetKey(dataset.getKey());
    int occurrenceMappingKey =
        occurrenceMappingService.addOccurrenceMapping(entityKey, occurrenceMapping);

    List<OccurrenceMapping> mappings = occurrenceMappingService.listOccurrenceMappings(entityKey);
    assertEquals(1, mappings.size());

    occurrenceMappingService.deleteOccurrenceMapping(entityKey, occurrenceMappingKey);
    mappings = occurrenceMappingService.listOccurrenceMappings(entityKey);
    assertTrue(mappings.isEmpty());
  }

  private Dataset createDataset() {
    Node node = new Node();
    node.setTitle("node");
    node.setType(NodeType.COUNTRY);
    node.setParticipationStatus(ParticipationStatus.AFFILIATE);
    nodeService.create(node);

    Organization org = new Organization();
    org.setEndorsingNodeKey(node.getKey());
    org.setTitle("organization");
    org.setLanguage(Language.ABKHAZIAN);
    org.setPassword("testtttt");
    organizationService.create(org);

    Installation installation = new Installation();
    installation.setTitle("title");
    installation.setOrganizationKey(org.getKey());
    installation.setType(InstallationType.BIOCASE_INSTALLATION);
    installationService.create(installation);

    Dataset dataset = new Dataset();
    dataset.setTitle("title");
    dataset.setInstallationKey(installation.getKey());
    dataset.setPublishingOrganizationKey(org.getKey());
    dataset.setType(DatasetType.CHECKLIST);
    dataset.setLanguage(Language.ABKHAZIAN);
    dataset.setLicense(License.CC0_1_0);
    datasetService.create(dataset);

    return dataset;
  }

  protected void testDuplicatesCommonCases() {
    ExtendedBaseCollectionEntityClient<T> wsClient = (ExtendedBaseCollectionEntityClient<T>) client;

    // same code
    DuplicatesRequest request = new DuplicatesRequest();
    request.setSameCode(true);

    DuplicatesResult result = wsClient.findPossibleDuplicates(request);
    assertEquals(1, result.getDuplicates().size());
    assertEquals(4, result.getDuplicates().get(0).size());

    // same fuzzy name
    request = new DuplicatesRequest();
    request.setSameFuzzyName(true);

    result = wsClient.findPossibleDuplicates(request);
    assertEquals(2, result.getDuplicates().size());
    assertEquals(3, result.getDuplicates().get(0).size());
    assertEquals(2, result.getDuplicates().get(1).size());

    // same fuzzy name and city
    request = new DuplicatesRequest();
    request.setSameFuzzyName(true);
    request.setSameCity(true);

    result = wsClient.findPossibleDuplicates(request);
    assertEquals(1, result.getDuplicates().size());
    assertEquals(2, result.getDuplicates().get(0).size());
    Set<UUID> keysFound =
        result.getDuplicates().get(0).stream().map(Duplicate::getKey).collect(Collectors.toSet());
    assertEquals(2, keysFound.size());

    // exclude keys
    request.setExcludeKeys(new ArrayList<>(keysFound));
    result = wsClient.findPossibleDuplicates(request);
    assertTrue(result.getDuplicates().isEmpty());

    // same name and city
    request = new DuplicatesRequest();
    request.setSameName(true);
    request.setSameCity(true);
    result = wsClient.findPossibleDuplicates(request);
    assertTrue(result.getDuplicates().isEmpty());

    // same name and not same city
    request.setSameCity(false);
    result = wsClient.findPossibleDuplicates(request);
    assertEquals(1, result.getDuplicates().size());
    assertEquals(2, result.getDuplicates().get(0).size());
    assertEquals(
        2, result.getDuplicates().get(0).stream().map(Duplicate::getKey).distinct().count());

    // filtering by country
    request.setInCountries(Collections.singletonList(Country.DENMARK.getIso2LetterCode()));
    result = wsClient.findPossibleDuplicates(request);
    assertEquals(1, result.getDuplicates().size());
    assertEquals(2, result.getDuplicates().get(0).size());
    assertEquals(
        2, result.getDuplicates().get(0).stream().map(Duplicate::getKey).distinct().count());

    request.setInCountries(Collections.singletonList(Country.SPAIN.getIso2LetterCode()));
    result = wsClient.findPossibleDuplicates(request);
    assertTrue(result.getDuplicates().isEmpty());

    // excluding country
    request.setInCountries(null);
    request.setNotInCountries(Collections.singletonList(Country.DENMARK.getIso2LetterCode()));
    result = wsClient.findPossibleDuplicates(request);
    assertTrue(result.getDuplicates().isEmpty());

    request.setNotInCountries(Collections.singletonList(Country.SPAIN.getIso2LetterCode()));
    result = wsClient.findPossibleDuplicates(request);
    assertEquals(1, result.getDuplicates().size());
    assertEquals(2, result.getDuplicates().get(0).size());
  }
}
