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
package org.gbif.registry.ws.it.collections;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.collections.Contactable;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.OccurrenceMapping;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.collections.PrimaryCollectionEntity;
import org.gbif.api.model.collections.merge.ConvertToCollectionParams;
import org.gbif.api.model.collections.merge.MergeParams;
import org.gbif.api.model.collections.suggestions.ChangeSuggestion;
import org.gbif.api.model.collections.suggestions.CollectionChangeSuggestion;
import org.gbif.api.model.collections.suggestions.InstitutionChangeSuggestion;
import org.gbif.api.model.collections.suggestions.Type;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.InstallationType;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.License;
import org.gbif.api.vocabulary.NodeType;
import org.gbif.api.vocabulary.ParticipationStatus;
import org.gbif.registry.domain.collections.AuditLog;
import org.gbif.registry.events.collections.EventType;
import org.gbif.registry.identity.service.IdentityService;
import org.gbif.registry.persistence.mapper.collections.AuditLogMapper;
import org.gbif.registry.persistence.mapper.collections.PersonMapper;
import org.gbif.registry.persistence.mapper.collections.dto.ChangeSuggestionDto;
import org.gbif.registry.persistence.mapper.collections.params.AuditLogListParams;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.client.collections.BaseCollectionEntityClient;
import org.gbif.registry.ws.client.collections.CollectionClient;
import org.gbif.registry.ws.client.collections.InstitutionClient;
import org.gbif.registry.ws.client.collections.PersonClient;
import org.gbif.registry.ws.client.collections.PrimaryCollectionEntityClient;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.registry.ws.it.collections.service.CollectionsDatabaseInitializer;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.gbif.ws.security.KeyStore;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuditLogIT extends BaseItTest {

  private final AuditLogMapper auditLogMapper;

  private static UUID datasetKey;
  private static UUID contactKey;

  private final InstitutionClient institutionClient;
  private final CollectionClient collectionClient;
  private final PersonClient personClient;

  @RegisterExtension protected CollectionsDatabaseInitializer collectionsDatabaseInitializer;

  @Autowired
  public AuditLogIT(
      AuditLogMapper auditLogMapper,
      IdentityService identityService,
      SimplePrincipalProvider simplePrincipalProvider,
      EsManageServer esServer,
      KeyStore keyStore,
      @LocalServerPort int localServerPort) {
    super(simplePrincipalProvider, esServer);
    this.auditLogMapper = auditLogMapper;
    collectionsDatabaseInitializer = new CollectionsDatabaseInitializer(identityService);
    this.institutionClient = prepareClient(localServerPort, keyStore, InstitutionClient.class);
    this.collectionClient = prepareClient(localServerPort, keyStore, CollectionClient.class);
    this.personClient = prepareClient(localServerPort, keyStore, PersonClient.class);
  }

  @BeforeAll
  public static void loadData(
      @Autowired NodeService nodeService,
      @Autowired OrganizationService organizationService,
      @Autowired InstallationService installationService,
      @Autowired DatasetService datasetService,
      @Autowired PersonMapper personMapper) {
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
    datasetKey = datasetService.create(dataset);

    Person person = new Person();
    person.setKey(UUID.randomUUID());
    person.setFirstName("first name");
    person.setEmail("aa@aa.com");
    person.setCreatedBy("test");
    person.setModifiedBy("test");
    personMapper.create(person);
    contactKey = person.getKey();
  }

  @Test
  public void institutionLogsTest() {
    Institution i = new Institution();
    i.setCode("c1");
    i.setName("n1");
    UUID key = institutionClient.create(i);
    long traceId = assertCreationCollectionEntity(key, CollectionEntityType.INSTITUTION);

    i = institutionClient.get(key);
    i.setName("n2");
    institutionClient.update(i);

    traceId = assertUpdateCollectionEntity(key, traceId);

    testSubEntities(key, traceId, institutionClient);
    testPrimaryEntityOperations(key, traceId, institutionClient);

    // change suggestions
    InstitutionChangeSuggestion institutionChangeSuggestion = new InstitutionChangeSuggestion();
    institutionChangeSuggestion.setEntityKey(key);
    institutionChangeSuggestion.setProposerEmail("aa@aa.com");
    institutionChangeSuggestion.setType(Type.UPDATE);
    institutionChangeSuggestion.setComments(Collections.singletonList("comment"));
    i = institutionClient.get(key);
    i.setName("n3");
    institutionChangeSuggestion.setSuggestedEntity(i);
    testChangeSuggestions(i, institutionChangeSuggestion, institutionClient);

    // merge
    Institution i2 = new Institution();
    i2.setCode("c2");
    i2.setName("n2");
    UUID key2 = institutionClient.create(i2);

    MergeParams mergeParams = new MergeParams();
    mergeParams.setReplacementEntityKey(key2);
    institutionClient.merge(key, mergeParams);
    assertMerge(key, key2, CollectionEntityType.INSTITUTION);

    // conversion to collection
    ConvertToCollectionParams conversionParams = new ConvertToCollectionParams();
    conversionParams.setNameForNewInstitution("newInst");
    UUID convertedCollectionKey = institutionClient.convertToCollection(key2, conversionParams);
    List<AuditLog> logs =
        auditLogMapper.list(
            AuditLogListParams.builder()
                .operation(EventType.CONVERSION_TO_COLLECTION.name())
                .build(),
            new PagingRequest());
    assertEquals(1, logs.size());
    assertEquals(convertedCollectionKey, logs.get(0).getReplacementKey());

    long conversionTraceId = logs.get(0).getTraceId();
    logs =
        auditLogMapper.list(
            AuditLogListParams.builder().traceId(conversionTraceId).build(), new PagingRequest());
    assertTrue(logs.size() > 1);

    // the conversion operation is set on the old entity, a new instituion is created and the rest
    // of the operations to the replacement
    assertEquals(3, logs.stream().map(AuditLog::getCollectionEntityKey).distinct().count());
    assertEquals(2, logs.stream().map(AuditLog::getCollectionEntityType).distinct().count());
    assertEquals(
        logs.size() - 2,
        logs.stream()
            .filter(l -> l.getCollectionEntityKey().equals(convertedCollectionKey))
            .count());

    // delete
    Institution i3 = new Institution();
    i3.setCode("c3");
    i3.setName("n3");
    UUID key3 = institutionClient.create(i3);
    institutionClient.delete(key3);
    assertDeletionCollectionEntity(key3);
  }

  @Test
  public void collectionLogsTest() {
    Collection c = new Collection();
    c.setCode("c1");
    c.setName("n1");
    UUID key = collectionClient.create(c);
    long traceId = assertCreationCollectionEntity(key, CollectionEntityType.COLLECTION);

    c = collectionClient.get(key);
    c.setName("n2");
    collectionClient.update(c);

    traceId = assertUpdateCollectionEntity(key, traceId);

    testSubEntities(key, traceId, collectionClient);
    testPrimaryEntityOperations(key, traceId, collectionClient);

    // change suggestions
    CollectionChangeSuggestion collectionChangeSuggestion = new CollectionChangeSuggestion();
    collectionChangeSuggestion.setEntityKey(key);
    collectionChangeSuggestion.setProposerEmail("aa@aa.com");
    collectionChangeSuggestion.setType(Type.UPDATE);
    collectionChangeSuggestion.setComments(Collections.singletonList("comment"));
    c = collectionClient.get(key);
    c.setName("n3");
    collectionChangeSuggestion.setSuggestedEntity(c);
    testChangeSuggestions(c, collectionChangeSuggestion, collectionClient);

    // merge
    Collection c2 = new Collection();
    c2.setCode("c2");
    c2.setName("n2");
    UUID key2 = collectionClient.create(c2);

    MergeParams mergeParams = new MergeParams();
    mergeParams.setReplacementEntityKey(key2);
    collectionClient.merge(key, mergeParams);
    assertMerge(key, key2, CollectionEntityType.COLLECTION);

    // delete
    Collection c3 = new Collection();
    c3.setCode("c3");
    c3.setName("n3");
    UUID key3 = collectionClient.create(c3);
    collectionClient.delete(key3);
    assertDeletionCollectionEntity(key3);
  }

  @Test
  public void personLogsTest() {
    Person p = new Person();
    p.setFirstName("fn");
    p.setEmail("aa@aa.com");
    UUID key = personClient.create(p);
    long traceId = assertCreationCollectionEntity(key, CollectionEntityType.PERSON);

    p = personClient.get(key);
    p.setFirstName("fn2");
    personClient.update(p);

    traceId = assertUpdateCollectionEntity(key, traceId);
    testSubEntities(key, traceId, personClient);

    // delete
    Person p2 = new Person();
    p2.setFirstName("fn2");
    p2.setEmail("aa@aa.com");
    UUID key2 = personClient.create(p2);
    personClient.delete(key2);
    assertDeletionCollectionEntity(key2);
  }

  private <T extends CollectionEntity & Taggable & Identifiable & MachineTaggable>
      void testSubEntities(
          UUID entityKey, long previousTraceId, BaseCollectionEntityClient<T> client) {
    int identifierKey = client.addIdentifier(entityKey, new Identifier(IdentifierType.LSID, "foo"));
    long traceId =
        assertSubEntityCreation(
            entityKey,
            previousTraceId,
            identifierKey,
            Identifier.class.getSimpleName(),
            EventType.CREATE.name());

    client.deleteIdentifier(entityKey, identifierKey);
    traceId =
        assertSubEntityCreation(
            entityKey,
            traceId,
            identifierKey,
            Identifier.class.getSimpleName(),
            EventType.DELETE.name());

    int mtKey = client.addMachineTag(entityKey, new MachineTag("ns", "name", "value"));
    traceId =
        assertSubEntityCreation(
            entityKey, traceId, mtKey, MachineTag.class.getSimpleName(), EventType.CREATE.name());

    client.deleteMachineTag(entityKey, mtKey);
    traceId =
        assertSubEntityCreation(
            entityKey, traceId, mtKey, MachineTag.class.getSimpleName(), EventType.DELETE.name());

    Comment comment = new Comment();
    comment.setContent("test");
    int commentKey = client.addComment(entityKey, comment);
    traceId =
        assertSubEntityCreation(
            entityKey, traceId, commentKey, Comment.class.getSimpleName(), EventType.CREATE.name());

    client.deleteComment(entityKey, commentKey);
    traceId =
        assertSubEntityCreation(
            entityKey, traceId, commentKey, Comment.class.getSimpleName(), EventType.DELETE.name());

    int tagKey = client.addTag(entityKey, new Tag("value"));
    traceId =
        assertSubEntityCreation(
            entityKey, traceId, tagKey, Tag.class.getSimpleName(), EventType.CREATE.name());

    client.deleteTag(entityKey, tagKey);
    assertSubEntityCreation(
        entityKey, traceId, tagKey, Tag.class.getSimpleName(), EventType.DELETE.name());
  }

  private <
          T extends
              PrimaryCollectionEntity & Taggable & Identifiable & MachineTaggable & Contactable,
          R extends ChangeSuggestion<T>>
      void testChangeSuggestions(
          T entity, R changeSuggestion, PrimaryCollectionEntityClient<T, R> client) {

    // create change suggestion
    int suggKey = client.createChangeSuggestion(changeSuggestion);
    List<AuditLog> logs =
        auditLogMapper.list(
            AuditLogListParams.builder()
                .operation(EventType.CREATE.name())
                .subEntityType(ChangeSuggestionDto.class.getSimpleName())
                .subEntityKey(String.valueOf(suggKey))
                .build(),
            new PagingRequest());
    assertEquals(1, logs.size());
    assertNull(logs.get(0).getPreState());
    assertNotNull(logs.get(0).getPostState());

    changeSuggestion = client.getChangeSuggestion(suggKey);
    entity.setName("n4");
    changeSuggestion.getComments().add("other");
    client.updateChangeSuggestion(suggKey, changeSuggestion);
    logs =
        auditLogMapper.list(
            AuditLogListParams.builder()
                .operation(EventType.UPDATE.name())
                .subEntityType(ChangeSuggestionDto.class.getSimpleName())
                .subEntityKey(String.valueOf(suggKey))
                .build(),
            new PagingRequest());
    assertEquals(1, logs.size());
    assertNotNull(logs.get(0).getPreState());
    assertNotNull(logs.get(0).getPostState());

    client.applyChangeSuggestion(suggKey);
    logs =
        auditLogMapper.list(
            AuditLogListParams.builder()
                .operation(EventType.APPLY_SUGGESTION.name())
                .subEntityType(ChangeSuggestionDto.class.getSimpleName())
                .subEntityKey(String.valueOf(suggKey))
                .build(),
            new PagingRequest());
    assertEquals(1, logs.size());
    assertNotNull(logs.get(0).getPreState());
    assertNotNull(logs.get(0).getPostState());

    // create another sugg to discard it
    changeSuggestion.setKey(null);
    int suggKey2 = client.createChangeSuggestion(changeSuggestion);
    logs =
        auditLogMapper.list(
            AuditLogListParams.builder()
                .operation(EventType.CREATE.name())
                .subEntityType(ChangeSuggestionDto.class.getSimpleName())
                .subEntityKey(String.valueOf(suggKey2))
                .build(),
            new PagingRequest());
    assertEquals(1, logs.size());

    client.discardChangeSuggestion(suggKey2);
    logs =
        auditLogMapper.list(
            AuditLogListParams.builder()
                .operation(EventType.DISCARD_SUGGESTION.name())
                .subEntityType(ChangeSuggestionDto.class.getSimpleName())
                .subEntityKey(String.valueOf(suggKey2))
                .build(),
            new PagingRequest());
    assertEquals(1, logs.size());
    assertNotNull(logs.get(0).getPreState());
    assertNotNull(logs.get(0).getPostState());
  }

  private <
          T extends CollectionEntity & Taggable & Identifiable & MachineTaggable & Contactable,
          R extends ChangeSuggestion<T>>
      void testPrimaryEntityOperations(
          UUID entityKey, long previousTraceId, PrimaryCollectionEntityClient<T, R> client) {
    int omKey = client.addOccurrenceMapping(entityKey, new OccurrenceMapping("c", "i", datasetKey));
    long traceId =
        assertSubEntityCreation(
            entityKey,
            previousTraceId,
            omKey,
            OccurrenceMapping.class.getSimpleName(),
            EventType.CREATE.name());

    client.deleteOccurrenceMapping(entityKey, omKey);
    traceId =
        assertSubEntityCreation(
            entityKey,
            traceId,
            omKey,
            OccurrenceMapping.class.getSimpleName(),
            EventType.DELETE.name());

    client.addContact(entityKey, contactKey);
    traceId =
        assertSubEntityCreation(
            entityKey, traceId, contactKey, Person.class.getSimpleName(), EventType.LINK.name());

    client.removeContact(entityKey, contactKey);
    assertSubEntityCreation(
        entityKey, traceId, contactKey, Person.class.getSimpleName(), EventType.UNLINK.name());
  }

  private void assertDeletionCollectionEntity(UUID collectionEntityKey) {
    List<AuditLog> logs =
        auditLogMapper.list(
            AuditLogListParams.builder()
                .collectionEntityKey(collectionEntityKey)
                .operation(EventType.DELETE.name())
                .build(),
            new PagingRequest());
    assertEquals(1, logs.size());
    assertNotNull(logs.get(0).getPreState());
    assertNotNull(logs.get(0).getPostState());

    // the delete updates the entity to set the modifiedBy
    logs =
        auditLogMapper.list(
            AuditLogListParams.builder()
                .collectionEntityKey(collectionEntityKey)
                .operation(EventType.UPDATE.name())
                .build(),
            new PagingRequest());
    assertEquals(1, logs.size());
  }

  private long assertCreationCollectionEntity(
      UUID collectionEntityKey, CollectionEntityType collectionEntityType) {
    List<AuditLog> logs =
        auditLogMapper.list(
            AuditLogListParams.builder().collectionEntityKey(collectionEntityKey).build(),
            new PagingRequest());
    assertEquals(1, logs.size());
    AuditLog auditLog = logs.get(0);
    assertEquals(EventType.CREATE.name(), auditLog.getOperation());
    assertEquals(collectionEntityType, auditLog.getCollectionEntityType());
    assertNull(logs.get(0).getPreState());
    assertNotNull(logs.get(0).getPostState());
    return auditLog.getTraceId();
  }

  private long assertUpdateCollectionEntity(UUID collectionEntityKey, long previousTraceId) {
    List<AuditLog> logs =
        auditLogMapper.list(
            AuditLogListParams.builder()
                .collectionEntityKey(collectionEntityKey)
                .operation(EventType.UPDATE.name())
                .build(),
            new PagingRequest());
    assertEquals(1, logs.size());
    logs =
        auditLogMapper.list(
            AuditLogListParams.builder()
                .collectionEntityKey(collectionEntityKey)
                .operation(EventType.UPDATE.name())
                .build(),
            new PagingRequest());
    assertEquals(1, logs.size());
    assertNotNull(logs.get(0).getPreState());
    assertNotNull(logs.get(0).getPostState());
    assertNotEquals(logs.get(0).getPreState(), logs.get(0).getPostState());
    assertNotEquals(previousTraceId, logs.get(0).getTraceId());
    return logs.get(0).getTraceId();
  }

  private long assertSubEntityCreation(
      UUID collectionEntityKey,
      long previousTraceId,
      Object subEntityKey,
      String subEntity,
      String operation) {
    List<AuditLog> logs =
        auditLogMapper.list(
            AuditLogListParams.builder()
                .collectionEntityKey(collectionEntityKey)
                .subEntityType(subEntity)
                .operation(operation)
                .build(),
            new PagingRequest());
    assertEquals(1, logs.size());
    assertNotEquals(previousTraceId, logs.get(0).getTraceId());
    assertEquals(String.valueOf(subEntityKey), logs.get(0).getSubEntityKey());

    if (operation.equals(EventType.CREATE.name())) {
      assertNull(logs.get(0).getPreState());
      assertNotNull(logs.get(0).getPostState());
    } else if (operation.equals(EventType.DELETE.name())) {
      assertNotNull(logs.get(0).getPreState());
      assertNull(logs.get(0).getPostState());
    }

    return logs.get(0).getTraceId();
  }

  private void assertMerge(
      UUID collectionEntityKey, UUID replacementKey, CollectionEntityType collectionEntityType) {
    List<AuditLog> logs =
        auditLogMapper.list(
            AuditLogListParams.builder()
                .collectionEntityKey(collectionEntityKey)
                .operation(EventType.REPLACE.name())
                .build(),
            new PagingRequest());
    assertEquals(1, logs.size());
    assertEquals(replacementKey, logs.get(0).getReplacementKey());
    long mergeTraceId = logs.get(0).getTraceId();
    logs =
        auditLogMapper.list(
            AuditLogListParams.builder().traceId(mergeTraceId).build(), new PagingRequest());
    assertTrue(logs.size() > 1);
    // the replace operation is set on the old entity and the rest of the operations to the
    // replacement
    assertEquals(
        logs.size() - 1,
        logs.stream().filter(l -> l.getCollectionEntityKey().equals(replacementKey)).count());
    assertTrue(logs.stream().allMatch(l -> l.getCollectionEntityType() == collectionEntityType));
  }
}
