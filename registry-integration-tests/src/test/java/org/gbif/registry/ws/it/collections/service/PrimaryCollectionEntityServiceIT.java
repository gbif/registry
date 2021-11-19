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
package org.gbif.registry.ws.it.collections.service;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Contactable;
import org.gbif.api.model.collections.OccurrenceMappeable;
import org.gbif.api.model.collections.OccurrenceMapping;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.collections.PrimaryCollectionEntity;
import org.gbif.api.model.collections.duplicates.Duplicate;
import org.gbif.api.model.collections.duplicates.DuplicatesResult;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.Commentable;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.LenientEquals;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.service.collections.PersonService;
import org.gbif.api.service.collections.PrimaryCollectionEntityService;
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
import org.gbif.registry.persistence.mapper.collections.params.DuplicatesSearchParams;
import org.gbif.registry.service.collections.duplicates.DuplicatesService;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class PrimaryCollectionEntityServiceIT<
        T extends
            PrimaryCollectionEntity & Taggable & MachineTaggable & Identifiable & Contactable
                & Commentable & OccurrenceMappeable & LenientEquals<T>>
    extends BaseCollectionEntityServiceIT<T> {

  protected final PersonService personService;
  protected final DatasetService datasetService;
  private final NodeService nodeService;
  protected final OrganizationService organizationService;
  private final InstallationService installationService;
  private final PrimaryCollectionEntityService<T> primaryCollectionEntityService;
  private final DuplicatesService duplicatesService;

  public PrimaryCollectionEntityServiceIT(
      PrimaryCollectionEntityService<T> primaryCollectionEntityService,
      PersonService personService,
      DatasetService datasetService,
      NodeService nodeService,
      OrganizationService organizationService,
      InstallationService installationService,
      SimplePrincipalProvider principalProvider,
      IdentityService identityService,
      DuplicatesService duplicatesService,
      Class<T> paramType) {
    super(primaryCollectionEntityService, principalProvider, identityService, paramType);
    this.personService = personService;
    this.datasetService = datasetService;
    this.nodeService = nodeService;
    this.organizationService = organizationService;
    this.installationService = installationService;
    this.primaryCollectionEntityService = primaryCollectionEntityService;
    this.duplicatesService = duplicatesService;
  }

  public static class MaterializedViewInitializer implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
      Connection connection =
          SpringExtension.getApplicationContext(extensionContext)
              .getBean(DataSource.class)
              .getConnection();
      // create materialized view for testing
      ScriptUtils.executeSqlScript(
          connection, new ClassPathResource("/scripts/create_duplicates_views.sql"));
      connection.close();
    }
  }

  @RegisterExtension
  static MaterializedViewInitializer materializedViewInitializer =
      new MaterializedViewInitializer();

  @Test
  public void contactsTest() {
    // entities
    UUID entityKey1 = primaryCollectionEntityService.create(testData.newEntity());
    UUID entityKey2 = primaryCollectionEntityService.create(testData.newEntity());
    UUID entityKey3 = primaryCollectionEntityService.create(testData.newEntity());

    // contacts
    Person person1 = new Person();
    person1.setFirstName("name1");
    UUID personKey1 = personService.create(person1);

    Person person2 = new Person();
    person2.setFirstName("name2");
    UUID personKey2 = personService.create(person2);

    // add contacts
    primaryCollectionEntityService.addContact(entityKey1, personKey1);
    primaryCollectionEntityService.addContact(entityKey1, personKey2);
    primaryCollectionEntityService.addContact(entityKey2, personKey2);

    // list contacts
    List<Person> contactsEntity1 = primaryCollectionEntityService.listContacts(entityKey1);
    assertEquals(2, contactsEntity1.size());

    List<Person> contactsEntity2 = primaryCollectionEntityService.listContacts(entityKey2);
    assertEquals(1, contactsEntity2.size());
    assertEquals("name2", contactsEntity2.get(0).getFirstName());

    assertEquals(0, primaryCollectionEntityService.listContacts(entityKey3).size());

    // remove contacts
    primaryCollectionEntityService.removeContact(entityKey1, personKey2);
    contactsEntity1 = primaryCollectionEntityService.listContacts(entityKey1);
    assertEquals(1, contactsEntity1.size());
    assertEquals("name1", contactsEntity1.get(0).getFirstName());

    primaryCollectionEntityService.removeContact(entityKey2, personKey2);
    assertEquals(0, primaryCollectionEntityService.listContacts(entityKey2).size());
  }

  @Test
  public void duplicateContactTest() {
    // entities
    UUID entityKey1 = primaryCollectionEntityService.create(testData.newEntity());

    // contacts
    Person person1 = new Person();
    person1.setFirstName("name1");
    UUID personKey1 = personService.create(person1);

    // add one contact
    primaryCollectionEntityService.addContact(entityKey1, personKey1);
    assertThrows(
        RuntimeException.class,
        () -> primaryCollectionEntityService.addContact(entityKey1, personKey1));
  }

  @Test
  public void updateAddressesTest() {
    // entities
    T newEntity = testData.newEntity();
    UUID entityKey = primaryCollectionEntityService.create(newEntity);
    assertNotNull(entityKey);
    T entity = primaryCollectionEntityService.get(entityKey);
    assertTrue(newEntity.lenientEquals(entity));

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

    primaryCollectionEntityService.update(entity);
    entity = primaryCollectionEntityService.get(entityKey);
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

    primaryCollectionEntityService.update(entity);
    entity = primaryCollectionEntityService.get(entityKey);
    assertNotNull(entity.getAddress());
    assertEquals("address2", entity.getAddress().getAddress());
    assertNotNull(entity.getMailingAddress());
    assertEquals("mailing address2", entity.getMailingAddress().getAddress());

    // delete address
    entity.setAddress(null);
    entity.setMailingAddress(null);
    primaryCollectionEntityService.update(entity);
    entity = primaryCollectionEntityService.get(entityKey);
    assertNull(entity.getAddress());
    assertNull(entity.getMailingAddress());
  }

  @Test
  public void occurrenceMappingsTest() {
    T entity = testData.newEntity();
    UUID entityKey = primaryCollectionEntityService.create(entity);

    Dataset dataset = createDataset();
    OccurrenceMapping occurrenceMapping = new OccurrenceMapping();
    occurrenceMapping.setCode("code");
    occurrenceMapping.setDatasetKey(dataset.getKey());
    int occurrenceMappingKey =
        primaryCollectionEntityService.addOccurrenceMapping(entityKey, occurrenceMapping);

    List<OccurrenceMapping> mappings =
        primaryCollectionEntityService.listOccurrenceMappings(entityKey);
    assertEquals(1, mappings.size());

    primaryCollectionEntityService.deleteOccurrenceMapping(entityKey, occurrenceMappingKey);
    mappings = primaryCollectionEntityService.listOccurrenceMappings(entityKey);
    assertTrue(mappings.isEmpty());
  }

  protected Dataset createDataset() {
    Organization org = createOrganization();

    Installation installation = new Installation();
    installation.setTitle("title");
    installation.setOrganizationKey(org.getKey());
    installation.setType(InstallationType.BIOCASE_INSTALLATION);
    installationService.create(installation);

    Dataset dataset = new Dataset();
    dataset.setDoi(new DOI("10.1594/pangaea.94668"));
    dataset.setTitle("title");
    dataset.setInstallationKey(installation.getKey());
    dataset.setPublishingOrganizationKey(org.getKey());
    dataset.setType(DatasetType.CHECKLIST);
    dataset.setLanguage(Language.ABKHAZIAN);
    dataset.setLicense(License.CC0_1_0);
    datasetService.create(dataset);

    return dataset;
  }

  protected Organization createOrganization() {
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

    return org;
  }

  protected void testDuplicatesCommonCases() {
    // same code
    DuplicatesSearchParams params = new DuplicatesSearchParams();
    params.setSameCode(true);

    DuplicatesResult result = duplicatesService.findPossibleDuplicates(params);
    assertEquals(1, result.getDuplicates().size());
    assertEquals(4, result.getDuplicates().get(0).size());

    // same fuzzy name
    params = new DuplicatesSearchParams();
    params.setSameFuzzyName(true);

    result = duplicatesService.findPossibleDuplicates(params);
    assertEquals(2, result.getDuplicates().size());
    assertEquals(3, result.getDuplicates().get(0).size());
    assertEquals(2, result.getDuplicates().get(1).size());

    // same fuzzy name and city
    params = new DuplicatesSearchParams();
    params.setSameFuzzyName(true);
    params.setSameCity(true);

    result = duplicatesService.findPossibleDuplicates(params);
    assertEquals(1, result.getDuplicates().size());
    assertEquals(2, result.getDuplicates().get(0).size());
    Set<UUID> keysFound =
        result.getDuplicates().get(0).stream().map(Duplicate::getKey).collect(Collectors.toSet());
    assertEquals(2, keysFound.size());

    // exclude keys
    params.setExcludeKeys(new ArrayList<>(keysFound));
    result = duplicatesService.findPossibleDuplicates(params);
    assertTrue(result.getDuplicates().isEmpty());

    // same name and city
    params = new DuplicatesSearchParams();
    params.setSameName(true);
    params.setSameCity(true);
    result = duplicatesService.findPossibleDuplicates(params);
    assertTrue(result.getDuplicates().isEmpty());

    // same name and not same city
    params.setSameCity(false);
    result = duplicatesService.findPossibleDuplicates(params);
    assertEquals(1, result.getDuplicates().size());
    assertEquals(2, result.getDuplicates().get(0).size());
    assertEquals(
        2, result.getDuplicates().get(0).stream().map(Duplicate::getKey).distinct().count());

    // filtering by country
    params.setInCountries(Collections.singletonList(Country.DENMARK));
    result = duplicatesService.findPossibleDuplicates(params);
    assertEquals(1, result.getDuplicates().size());
    assertEquals(2, result.getDuplicates().get(0).size());
    assertEquals(
        2, result.getDuplicates().get(0).stream().map(Duplicate::getKey).distinct().count());

    params.setInCountries(Collections.singletonList(Country.SPAIN));
    result = duplicatesService.findPossibleDuplicates(params);
    assertTrue(result.getDuplicates().isEmpty());

    // excluding country
    params.setInCountries(null);
    params.setNotInCountries(Collections.singletonList(Country.DENMARK));
    result = duplicatesService.findPossibleDuplicates(params);
    assertTrue(result.getDuplicates().isEmpty());

    params.setNotInCountries(Collections.singletonList(Country.SPAIN));
    result = duplicatesService.findPossibleDuplicates(params);
    assertEquals(1, result.getDuplicates().size());
    assertEquals(2, result.getDuplicates().get(0).size());
  }
}
