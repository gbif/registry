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
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.MasterSourceMetadata;
import org.gbif.api.model.collections.OccurrenceMapping;
import org.gbif.api.model.collections.UserId;
import org.gbif.api.model.collections.duplicates.Duplicate;
import org.gbif.api.model.collections.duplicates.DuplicatesResult;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Commentable;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.LenientEquals;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.service.collections.CollectionEntityService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.InstallationType;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.License;
import org.gbif.api.vocabulary.NodeType;
import org.gbif.api.vocabulary.ParticipationStatus;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.api.vocabulary.collections.IdType;
import org.gbif.api.vocabulary.collections.MasterSourceType;
import org.gbif.api.vocabulary.collections.Source;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.persistence.mapper.collections.params.DuplicatesSearchParams;
import org.gbif.registry.service.collections.duplicates.DuplicatesService;
import org.gbif.registry.ws.it.collections.data.TestData;
import org.gbif.registry.ws.it.collections.data.TestDataFactory;
import org.gbif.ws.NotFoundException;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Base class to test the CRUD operations of {@link CollectionEntity}. */
public abstract class BaseCollectionEntityServiceIT<
        T extends
            CollectionEntity & Identifiable & Taggable & MachineTaggable & Commentable
                & LenientEquals<T>>
    extends BaseServiceIT {

  protected final CollectionEntityService<T> collectionEntityService;
  protected final DatasetService datasetService;
  private final NodeService nodeService;
  protected final OrganizationService organizationService;
  private final InstallationService installationService;
  private final DuplicatesService duplicatesService;
  protected final Class<T> paramType;
  protected final TestData<T> testData;

  public static final Pageable DEFAULT_PAGE = new PagingRequest(0L, 5);

  @RegisterExtension
  protected static CollectionsMaterializedViewsInitializer mvInitializer =
      new CollectionsMaterializedViewsInitializer(PG_CONTAINER);

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule = new TestCaseDatabaseInitializer();

  public BaseCollectionEntityServiceIT(
      CollectionEntityService<T> collectionEntityService,
      DatasetService datasetService,
      NodeService nodeService,
      OrganizationService organizationService,
      InstallationService installationService,
      SimplePrincipalProvider principalProvider,
      DuplicatesService duplicatesService,
      Class<T> paramType) {
    super(principalProvider);
    this.collectionEntityService = collectionEntityService;
    this.datasetService = datasetService;
    this.nodeService = nodeService;
    this.organizationService = organizationService;
    this.installationService = installationService;
    this.duplicatesService = duplicatesService;
    this.paramType = paramType;
    this.testData = TestDataFactory.create(paramType);
  }

  @Test
  public void crudTest() {
    // create
    T entity = testData.newEntity();
    UUID key = collectionEntityService.create(entity);

    assertNotNull(key);

    T entitySaved = collectionEntityService.get(key);
    assertEquals(key, entitySaved.getKey());
    assertTrue(entity.lenientEquals(entitySaved));
    assertNotNull(entitySaved.getCreatedBy());
    assertNotNull(entitySaved.getCreated());
    assertNotNull(entitySaved.getModifiedBy());
    assertNotNull(entitySaved.getModified());

    // update
    entity = testData.updateEntity(entitySaved);
    collectionEntityService.update(entity);

    entitySaved = collectionEntityService.get(key);
    assertTrue(entity.lenientEquals(entitySaved));
    assertNotEquals(entitySaved.getCreated(), entitySaved.getModified());

    // delete
    collectionEntityService.delete(key);
    entitySaved = collectionEntityService.get(key);
    assertNotNull(entitySaved.getDeleted());
  }

  @Test
  public void createInvalidEntityTest() {
    assertThrows(
        ValidationException.class,
        () -> collectionEntityService.create(testData.newInvalidEntity()));
  }

  @Test
  public void deleteMissingEntityTest() {
    assertThrows(
        IllegalArgumentException.class, () -> collectionEntityService.delete(UUID.randomUUID()));
  }

  @Test
  public void updateDeletedEntityTest() {
    T entity = testData.newEntity();
    UUID key = collectionEntityService.create(entity);
    entity.setKey(key);
    collectionEntityService.delete(key);

    T entity2 = collectionEntityService.get(key);
    assertNotNull(entity2.getDeleted());
    assertThrows(IllegalArgumentException.class, () -> collectionEntityService.update(entity2));
  }

  @Test
  public void restoreDeletedEntityTest() {
    T entity = testData.newEntity();
    UUID key = collectionEntityService.create(entity);
    entity.setKey(key);
    collectionEntityService.delete(key);
    entity = collectionEntityService.get(key);
    assertNotNull(entity.getDeleted());

    // restore it
    entity.setDeleted(null);
    collectionEntityService.update(entity);
    entity = collectionEntityService.get(key);
    assertNull(entity.getDeleted());
  }

  @Test
  public void updateInvalidEntityTest() {
    T entity = testData.newEntity();
    UUID key = collectionEntityService.create(entity);

    T newEntity = testData.newInvalidEntity();
    newEntity.setKey(key);
    assertThrows(ValidationException.class, () -> collectionEntityService.update(newEntity));
  }

  @Test
  public void getMissingEntity() {
    try {
      T entity = collectionEntityService.get(UUID.randomUUID());
      assertNull(entity);
    } catch (Exception ex) {
      assertEquals(NotFoundException.class, ex.getClass());
    }
  }

  @Test
  public void tagsTest() {
    UUID key = collectionEntityService.create(testData.newEntity());

    Tag tag = new Tag();
    tag.setValue("value");
    int tagKey = collectionEntityService.addTag(key, tag);

    List<Tag> tags = collectionEntityService.listTags(key, null);
    assertEquals(1, tags.size());
    assertEquals(tagKey, tags.get(0).getKey());
    assertEquals("value", tags.get(0).getValue());

    collectionEntityService.deleteTag(key, tagKey);
    assertEquals(0, collectionEntityService.listTags(key, null).size());
  }

  @Test
  public void machineTagsTest() {
    T entity = testData.newEntity();
    UUID key = collectionEntityService.create(entity);

    MachineTag machineTag = new MachineTag("ns", "name", "value");
    int machineTagKey = collectionEntityService.addMachineTag(key, machineTag);

    List<MachineTag> machineTags = collectionEntityService.listMachineTags(key);
    assertEquals(1, machineTags.size());
    assertEquals(machineTagKey, machineTags.get(0).getKey());
    assertEquals("value", machineTags.get(0).getValue());

    collectionEntityService.deleteMachineTag(key, machineTagKey);
    assertEquals(0, collectionEntityService.listMachineTags(key).size());
  }

  @Test
  public void identifiersTest() {
    T entity = testData.newEntity();
    UUID key = collectionEntityService.create(entity);

    Identifier identifier = new Identifier();
    identifier.setIdentifier("identifier");
    identifier.setType(IdentifierType.LSID);

    int identifierKey = collectionEntityService.addIdentifier(key, identifier);

    List<Identifier> identifiers = collectionEntityService.listIdentifiers(key);
    assertEquals(1, identifiers.size());
    assertEquals(identifierKey, identifiers.get(0).getKey());
    assertEquals("identifier", identifiers.get(0).getIdentifier());
    assertEquals(IdentifierType.LSID, identifiers.get(0).getType());

    collectionEntityService.deleteIdentifier(key, identifierKey);
    assertEquals(0, collectionEntityService.listIdentifiers(key).size());

    // add invalid wikidata identifier
    assertThrows(
        IllegalArgumentException.class,
        () ->
            collectionEntityService.addIdentifier(
                key, new Identifier(IdentifierType.WIKIDATA, "foo")));
  }

  @Test
  public void commentsTest() {
    T entity = testData.newEntity();
    UUID key = collectionEntityService.create(entity);

    Comment comment = new Comment();
    comment.setContent("test comment");

    int commentKey = collectionEntityService.addComment(key, comment);

    List<Comment> comments = collectionEntityService.listComments(key);
    assertEquals(1, comments.size());
    assertEquals(commentKey, comments.get(0).getKey());
    assertEquals(comment.getContent(), comments.get(0).getContent());

    collectionEntityService.deleteComment(key, commentKey);
    assertEquals(0, collectionEntityService.listComments(key).size());
  }

  @Test
  public void updateAddressesTest() {
    // entities
    T newEntity = testData.newEntity();
    UUID entityKey = collectionEntityService.create(newEntity);
    assertNotNull(entityKey);
    T entity = collectionEntityService.get(entityKey);
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

    collectionEntityService.update(entity);
    entity = collectionEntityService.get(entityKey);
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

    collectionEntityService.update(entity);
    entity = collectionEntityService.get(entityKey);
    assertNotNull(entity.getAddress());
    assertEquals("address2", entity.getAddress().getAddress());
    assertNotNull(entity.getMailingAddress());
    assertEquals("mailing address2", entity.getMailingAddress().getAddress());

    // delete address
    entity.setAddress(null);
    entity.setMailingAddress(null);
    collectionEntityService.update(entity);
    entity = collectionEntityService.get(entityKey);
    assertNull(entity.getAddress());
    assertNull(entity.getMailingAddress());
  }

  @Test
  public void occurrenceMappingsTest() {
    T entity = testData.newEntity();
    UUID entityKey = collectionEntityService.create(entity);

    Dataset dataset = createDataset();
    OccurrenceMapping occurrenceMapping = new OccurrenceMapping();
    occurrenceMapping.setCode("code");
    occurrenceMapping.setDatasetKey(dataset.getKey());
    int occurrenceMappingKey =
        collectionEntityService.addOccurrenceMapping(entityKey, occurrenceMapping);

    List<OccurrenceMapping> mappings = collectionEntityService.listOccurrenceMappings(entityKey);
    assertEquals(1, mappings.size());

    collectionEntityService.deleteOccurrenceMapping(entityKey, occurrenceMappingKey);
    mappings = collectionEntityService.listOccurrenceMappings(entityKey);
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
    UUID entityKey1 = collectionEntityService.create(entity);

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

    collectionEntityService.addContactPerson(entityKey1, contact);

    List<Contact> contacts = collectionEntityService.listContactPersons(entityKey1);
    assertEquals(1, contacts.size());

    Contact contactCreated = contacts.get(0);
    assertTrue(contactCreated.lenientEquals(contact));

    contactCreated.setPosition(Collections.singletonList("position"));
    contactCreated.setTaxonomicExpertise(Collections.singletonList("aves"));

    UserId userId = new UserId();
    userId.setId("id");
    userId.setType(IdType.OTHER);
    contactCreated.setUserIds(Collections.singletonList(userId));
    collectionEntityService.updateContactPerson(entityKey1, contactCreated);

    contacts = collectionEntityService.listContactPersons(entityKey1);
    assertEquals(1, contacts.size());

    Contact contactUpdated = contacts.get(0);
    assertTrue(contactUpdated.lenientEquals(contactCreated));

    UserId userId2 = new UserId();
    userId2.setId("id");
    userId2.setType(IdType.HUH);
    contactUpdated.getUserIds().add(userId2);
    assertThrows(
        IllegalArgumentException.class,
        () -> collectionEntityService.updateContactPerson(entityKey1, contactUpdated));

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

    int contactKey = collectionEntityService.addContactPerson(entityKey1, contact2);
    assertTrue(contactKey > 0);
    contacts = collectionEntityService.listContactPersons(entityKey1);
    assertEquals(2, contacts.size());

    collectionEntityService.removeContactPerson(entityKey1, contactCreated.getKey());
    contacts = collectionEntityService.listContactPersons(entityKey1);
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
        () -> collectionEntityService.addContactPerson(entityKey1, contact3));
  }

  @Test
  public void addMasterSourceTest() throws InterruptedException, ExecutionException {
    T entity = testData.newEntity();

    Source rightSource = null;
    Source wrongSource = null;
    UUID rightKey = null;
    UUID wrongKey = null;
    Organization organization = createOrganization();
    Dataset dataset = createDataset(organization);
    if (entity instanceof Institution) {
      rightSource = Source.ORGANIZATION;
      wrongSource = Source.DATASET;
      rightKey = organization.getKey();
      wrongKey = dataset.getKey();
    } else {
      rightSource = Source.DATASET;
      wrongSource = Source.ORGANIZATION;
      rightKey = dataset.getKey();
      wrongKey = organization.getKey();
    }

    UUID entityKey = collectionEntityService.create(entity);
    T entityCreated = collectionEntityService.get(entityKey);
    assertEquals(MasterSourceType.GRSCICOLL, entityCreated.getMasterSource());

    // should fail because the source doesn't exist
    MasterSourceMetadata metadata =
        new MasterSourceMetadata(rightSource, UUID.randomUUID().toString());
    assertThrows(
        IllegalArgumentException.class,
        () -> collectionEntityService.addMasterSourceMetadata(entityKey, metadata));

    // should fail because of wrong source
    MasterSourceMetadata metadata2 = new MasterSourceMetadata(wrongSource, wrongKey.toString());
    assertThrows(
        IllegalArgumentException.class,
        () -> collectionEntityService.addMasterSourceMetadata(entityKey, metadata2));

    MasterSourceMetadata metadata3 = new MasterSourceMetadata(rightSource, "Not a UUID");
    assertThrows(
        IllegalArgumentException.class,
        () -> collectionEntityService.addMasterSourceMetadata(entityKey, metadata3));

    MasterSourceMetadata metadata4 = new MasterSourceMetadata(rightSource, rightKey.toString());
    int metadataKey = collectionEntityService.addMasterSourceMetadata(entityKey, metadata4);
    assertTrue(metadataKey > 0);
    entityCreated = collectionEntityService.get(entityKey);
    assertEquals(MasterSourceType.GBIF_REGISTRY, entityCreated.getMasterSource());
    assertEquals(rightSource, entityCreated.getMasterSourceMetadata().getSource());
    assertEquals(rightKey.toString(), entityCreated.getMasterSourceMetadata().getSourceId());

    // cannot have more than 1 source
    MasterSourceMetadata metadata5 = new MasterSourceMetadata(rightSource, rightKey.toString());
    assertThrows(
        IllegalArgumentException.class,
        () -> collectionEntityService.addMasterSourceMetadata(entityKey, metadata5));

    List<T> entitiesFound =
        collectionEntityService.findByMasterSource(rightSource, rightKey.toString());
    assertFalse(entitiesFound.isEmpty());
    assertEquals(entityKey, entitiesFound.get(0).getKey());

    collectionEntityService.deleteMasterSourceMetadata(entityKey);
    entityCreated = collectionEntityService.get(entityKey);
    assertEquals(MasterSourceType.GRSCICOLL, entityCreated.getMasterSource());
    entitiesFound = collectionEntityService.findByMasterSource(rightSource, rightKey.toString());
    assertTrue(entitiesFound.isEmpty());
  }

  @Test
  public void removeLastIHEntityTest() {
    T entity = testData.newEntity();
    UUID entityKey = collectionEntityService.create(entity);

    final String irn = "123";
    collectionEntityService.addMasterSourceMetadata(
        entityKey, new MasterSourceMetadata(Source.IH_IRN, irn));

    resetSecurityContext("editor", UserRole.GRSCICOLL_EDITOR);

    assertThrows(
        IllegalArgumentException.class,
        () -> collectionEntityService.deleteMasterSourceMetadata(entityKey));

    resetSecurityContext("admin", UserRole.GRSCICOLL_ADMIN);
    assertDoesNotThrow(() -> collectionEntityService.deleteMasterSourceMetadata(entityKey));

    // we add the metadata again and create another entity
    collectionEntityService.addMasterSourceMetadata(
        entityKey, new MasterSourceMetadata(Source.IH_IRN, irn));

    T entity2 = testData.newEntity();
    UUID entityKey2 = collectionEntityService.create(entity2);
    collectionEntityService.addMasterSourceMetadata(
        entityKey2, new MasterSourceMetadata(Source.IH_IRN, irn));

    resetSecurityContext("editor", UserRole.GRSCICOLL_EDITOR);
    assertDoesNotThrow(() -> collectionEntityService.deleteMasterSourceMetadata(entityKey));
  }
}
