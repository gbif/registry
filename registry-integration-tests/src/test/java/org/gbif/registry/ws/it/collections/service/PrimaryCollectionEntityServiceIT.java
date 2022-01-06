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
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.collections.Contactable;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.MasterSourceMetadata;
import org.gbif.api.model.collections.OccurrenceMappeable;
import org.gbif.api.model.collections.OccurrenceMapping;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.collections.PrimaryCollectionEntity;
import org.gbif.api.model.collections.UserId;
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
import org.gbif.api.vocabulary.UserRole;
import org.gbif.api.vocabulary.collections.IdType;
import org.gbif.api.vocabulary.collections.MasterSourceType;
import org.gbif.api.vocabulary.collections.Source;
import org.gbif.registry.identity.service.IdentityService;
import org.gbif.registry.persistence.mapper.collections.params.DuplicatesSearchParams;
import org.gbif.registry.service.collections.converters.CollectionConverter;
import org.gbif.registry.service.collections.converters.InstitutionConverter;
import org.gbif.registry.service.collections.duplicates.DuplicatesService;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.sql.DataSource;
import javax.validation.ConstraintViolationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    return createDataset(createOrganization());
  }

  protected Dataset createDataset(Organization org) {
    Installation installation = new Installation();
    installation.setTitle("title");
    installation.setOrganizationKey(org.getKey());
    installation.setType(InstallationType.BIOCASE_INSTALLATION);
    installationService.create(installation);

    Dataset dataset = new Dataset();
    dataset.setDoi(new DOI("10.1594/pangaea.94668"));
    dataset.setTitle("title");
    dataset.setDescription("description dataset");
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
    org.setDescription("Description organization");
    org.setLanguage(Language.ABKHAZIAN);
    org.setPassword("testtttt");
    org.setEmail(Collections.singletonList("aa@aa.com"));
    org.setPhone(Collections.singletonList("123"));
    org.setCountry(Country.AFGHANISTAN);
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

  @Test
  public void contactPersonsTest() {
    T entity = testData.newEntity();
    UUID entityKey1 = primaryCollectionEntityService.create(entity);

    Contact contact = new Contact();
    contact.setFirstName("First name");
    contact.setLastName("last");
    contact.setCountry(Country.AFGHANISTAN);
    contact.setAddress(Collections.singletonList("address"));
    contact.setCity("City");
    contact.setEmail(Collections.singletonList("aa@aa.com"));
    contact.setFax(Collections.singletonList("fdsgds"));
    contact.setPhone(Collections.singletonList("fdsgds"));
    contact.setPrimary(true);
    contact.setNotes("notes");

    primaryCollectionEntityService.addContactPerson(entityKey1, contact);

    List<Contact> contacts = primaryCollectionEntityService.listContactPersons(entityKey1);
    assertEquals(1, contacts.size());

    Contact contactCreated = contacts.get(0);
    assertTrue(contactCreated.lenientEquals(contact));

    contactCreated.setPosition(Collections.singletonList("position"));
    contactCreated.setTaxonomicExpertise(Collections.singletonList("aves"));

    UserId userId = new UserId();
    userId.setId("id");
    userId.setType(IdType.OTHER);
    contactCreated.setUserIds(Collections.singletonList(userId));
    primaryCollectionEntityService.updateContactPerson(entityKey1, contactCreated);

    contacts = primaryCollectionEntityService.listContactPersons(entityKey1);
    assertEquals(1, contacts.size());

    Contact contactUpdated = contacts.get(0);
    assertTrue(contactUpdated.lenientEquals(contactCreated));

    UserId userId2 = new UserId();
    userId2.setId("id");
    userId2.setType(IdType.HUH);
    contactUpdated.getUserIds().add(userId2);
    assertThrows(
        IllegalArgumentException.class,
        () -> primaryCollectionEntityService.updateContactPerson(entityKey1, contactUpdated));

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

    int contactKey = primaryCollectionEntityService.addContactPerson(entityKey1, contact2);
    assertTrue(contactKey > 0);
    contacts = primaryCollectionEntityService.listContactPersons(entityKey1);
    assertEquals(2, contacts.size());

    primaryCollectionEntityService.removeContactPerson(entityKey1, contactCreated.getKey());
    contacts = primaryCollectionEntityService.listContactPersons(entityKey1);
    assertEquals(1, contacts.size());

    contact = contacts.get(0);
    assertTrue(contact.lenientEquals(contact2));

    Contact contact3 = new Contact();
    contact3.setFirstName("Another name 3");
    contact3.setTaxonomicExpertise(Arrays.asList("aves", "funghi"));
    contact3.setPosition(Collections.singletonList("Curator"));

    // we leave the type as null and it should fail
    UserId userId5 = new UserId();
    userId5.setId("id");
    contact3.setUserIds(Collections.singletonList(userId5));

    assertThrows(
        ConstraintViolationException.class,
        () -> primaryCollectionEntityService.addContactPerson(entityKey1, contact3));
  }

  @Test
  public void addMasterSourceTest() throws InterruptedException {
    T entity = testData.newEntity();

    Source rightSource = null;
    Source wrongSource = null;
    UUID rightKey = null;
    UUID wrongKey = null;
    Organization organization = createOrganization();
    Dataset dataset = createDataset(organization);
    Function<T, T> expectedSyncedEntityFn = null;
    if (entity instanceof Institution) {
      rightSource = Source.ORGANIZATION;
      wrongSource = Source.DATASET;
      rightKey = organization.getKey();
      wrongKey = dataset.getKey();
      expectedSyncedEntityFn =
          (e) -> (T) InstitutionConverter.convertFromOrganization(organization, (Institution) e);
    } else {
      rightSource = Source.DATASET;
      wrongSource = Source.ORGANIZATION;
      rightKey = dataset.getKey();
      wrongKey = organization.getKey();
      expectedSyncedEntityFn =
          (e) -> (T) CollectionConverter.convertFromDataset(dataset, organization, (Collection) e);
    }

    UUID entityKey = primaryCollectionEntityService.create(entity);
    T entityCreated = primaryCollectionEntityService.get(entityKey);
    assertEquals(MasterSourceType.GRSCICOLL, entityCreated.getMasterSource());

    // should fail because the source doesn't exist
    MasterSourceMetadata metadata =
        new MasterSourceMetadata(rightSource, UUID.randomUUID().toString());
    assertThrows(
        IllegalArgumentException.class,
        () -> primaryCollectionEntityService.addMasterSourceMetadata(entityKey, metadata));

    // should fail because of wrong source
    MasterSourceMetadata metadata2 = new MasterSourceMetadata(wrongSource, wrongKey.toString());
    assertThrows(
        IllegalArgumentException.class,
        () -> primaryCollectionEntityService.addMasterSourceMetadata(entityKey, metadata2));

    MasterSourceMetadata metadata3 = new MasterSourceMetadata(rightSource, "Not a UUID");
    assertThrows(
        IllegalArgumentException.class,
        () -> primaryCollectionEntityService.addMasterSourceMetadata(entityKey, metadata3));

    MasterSourceMetadata metadata4 = new MasterSourceMetadata(rightSource, rightKey.toString());
    int metadataKey = primaryCollectionEntityService.addMasterSourceMetadata(entityKey, metadata4);
    assertTrue(metadataKey > 0);
    entityCreated = primaryCollectionEntityService.get(entityKey);
    assertEquals(MasterSourceType.GBIF_REGISTRY, entityCreated.getMasterSource());
    assertEquals(rightSource, entityCreated.getMasterSourceMetadata().getSource());
    assertEquals(rightKey.toString(), entityCreated.getMasterSourceMetadata().getSourceId());

    // assert that the fields from the master source were synced
    // sleep the thread a little so the master source synchronizer updates the entity
    Thread.sleep(300);
    T syncedEntity = primaryCollectionEntityService.get(entityKey);
    T expectedSyncedEntity = expectedSyncedEntityFn.apply(entity);
    expectedSyncedEntity.setMasterSource(MasterSourceType.GBIF_REGISTRY);
    expectedSyncedEntity.setMasterSourceMetadata(syncedEntity.getMasterSourceMetadata());
    assertTrue(expectedSyncedEntity.lenientEquals(syncedEntity));

    if (entity instanceof Institution) {
      organization.setProvince("sfdsgdsg");
      organizationService.update(organization);

      // sleep the thread a little so the master source synchronizer updates the entity
      Thread.sleep(300);

      T updatedEntity = primaryCollectionEntityService.get(entityKey);
      assertEquals(organization.getProvince(), updatedEntity.getAddress().getProvince());
    } else if (entity instanceof Collection) {
      organization.setProvince("sfdsgdsg");
      organizationService.update(organization);
      dataset.setDescription("sfdsgdsg");
      datasetService.update(dataset);

      // sleep the thread a little so the master source synchronizer updates the entity
      Thread.sleep(300);

      T updatedEntity = primaryCollectionEntityService.get(entityKey);
      assertEquals(organization.getProvince(), updatedEntity.getAddress().getProvince());
      assertEquals(dataset.getDescription(), updatedEntity.getDescription());
    }


    // cannot have more than 1 source
    MasterSourceMetadata metadata5 = new MasterSourceMetadata(rightSource, rightKey.toString());
    assertThrows(
        IllegalArgumentException.class,
        () -> primaryCollectionEntityService.addMasterSourceMetadata(entityKey, metadata5));

    List<T> entitiesFound =
        primaryCollectionEntityService.findByMasterSource(rightSource, rightKey.toString());
    assertFalse(entitiesFound.isEmpty());
    assertEquals(entityKey, entitiesFound.get(0).getKey());

    primaryCollectionEntityService.deleteMasterSourceMetadata(entityKey);
    entityCreated = primaryCollectionEntityService.get(entityKey);
    assertEquals(MasterSourceType.GRSCICOLL, entityCreated.getMasterSource());
    entitiesFound =
      primaryCollectionEntityService.findByMasterSource(rightSource, rightKey.toString());
    assertTrue(entitiesFound.isEmpty());
  }

  @Test
  public void removeLastIHEntityTest() {
    T entity = testData.newEntity();
    UUID entityKey = primaryCollectionEntityService.create(entity);

    final String irn = "123";
    primaryCollectionEntityService.addMasterSourceMetadata(
        entityKey, new MasterSourceMetadata(Source.IH_IRN, irn));

    resetSecurityContext("editor", UserRole.GRSCICOLL_EDITOR);

    assertThrows(
        IllegalArgumentException.class,
        () -> primaryCollectionEntityService.deleteMasterSourceMetadata(entityKey));

    resetSecurityContext("admin", UserRole.GRSCICOLL_ADMIN);
    assertDoesNotThrow(() -> primaryCollectionEntityService.deleteMasterSourceMetadata(entityKey));

    // we add the metadata again and create another entity
    primaryCollectionEntityService.addMasterSourceMetadata(
        entityKey, new MasterSourceMetadata(Source.IH_IRN, irn));

    T entity2 = testData.newEntity();
    UUID entityKey2 = primaryCollectionEntityService.create(entity2);
    primaryCollectionEntityService.addMasterSourceMetadata(
        entityKey2, new MasterSourceMetadata(Source.IH_IRN, irn));

    resetSecurityContext("editor", UserRole.GRSCICOLL_EDITOR);
    assertDoesNotThrow(() -> primaryCollectionEntityService.deleteMasterSourceMetadata(entityKey));
  }
}
