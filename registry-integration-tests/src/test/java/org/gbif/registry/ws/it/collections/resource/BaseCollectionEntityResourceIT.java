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
package org.gbif.registry.ws.it.collections.resource;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Batch;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.collections.MasterSourceMetadata;
import org.gbif.api.model.collections.OccurrenceMapping;
import org.gbif.api.model.collections.duplicates.Duplicate;
import org.gbif.api.model.collections.duplicates.DuplicatesRequest;
import org.gbif.api.model.collections.duplicates.DuplicatesResult;
import org.gbif.api.model.collections.merge.MergeParams;
import org.gbif.api.model.collections.suggestions.ApplySuggestionResult;
import org.gbif.api.model.collections.suggestions.ChangeSuggestion;
import org.gbif.api.model.collections.suggestions.Status;
import org.gbif.api.model.collections.suggestions.Type;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Commentable;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.LenientEquals;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.service.collections.BatchService;
import org.gbif.api.service.collections.ChangeSuggestionService;
import org.gbif.api.service.collections.CollectionEntityService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.api.vocabulary.collections.Source;
import org.gbif.registry.persistence.mapper.collections.params.DuplicatesSearchParams;
import org.gbif.registry.security.ResourceNotFoundService;
import org.gbif.registry.service.collections.duplicates.DuplicatesService;
import org.gbif.registry.service.collections.merge.MergeService;
import org.gbif.registry.ws.client.collections.BaseCollectionEntityClient;
import org.gbif.registry.ws.it.collections.data.TestData;
import org.gbif.registry.ws.it.collections.data.TestDataFactory;
import org.gbif.registry.ws.it.fixtures.RequestTestFixture;
import org.gbif.registry.ws.it.fixtures.TestConstants;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.hamcrest.core.StringContains;
import org.hamcrest.core.StringEndsWith;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

abstract class BaseCollectionEntityResourceIT<
        T extends
            CollectionEntity & Identifiable & Taggable & MachineTaggable & Commentable
                & LenientEquals<T>,
        R extends ChangeSuggestion<T>>
    extends BaseResourceIT {

  protected final BaseCollectionEntityClient<T, R> baseClient;
  protected final TestData<T> testData;
  protected final Class<T> paramType;

  @Autowired protected ObjectMapper objectMapper;
  @Autowired protected MockMvc mockMvc;

  @MockBean private ResourceNotFoundService resourceNotFoundService;
  @MockBean private BatchService batchService;

  public BaseCollectionEntityResourceIT(
      Class<? extends BaseCollectionEntityClient<T, R>> cls,
      SimplePrincipalProvider simplePrincipalProvider,
      RequestTestFixture requestTestFixture,
      Class<T> paramType,
      int localServerPort) {
    super(simplePrincipalProvider, requestTestFixture);
    this.baseClient = prepareClient(TestConstants.TEST_GRSCICOLL_ADMIN, localServerPort, cls);
    this.testData = TestDataFactory.create(paramType);
    this.paramType = paramType;
  }

  @Test
  public void crudTest() {
    // create
    T entity = testData.newEntity();
    UUID entityKey = UUID.randomUUID();

    when(getMockCollectionEntityService().create(entity)).thenReturn(entityKey);
    UUID key = baseClient.create(entity);
    assertEquals(entityKey, key);

    mockGetEntity(entityKey, entity);
    T entitySaved = baseClient.get(key);
    assertTrue(entity.lenientEquals(entitySaved));
    entitySaved.setKey(key);

    // update
    entity = testData.updateEntity(entitySaved);
    doNothing().when(getMockCollectionEntityService()).update(entity);
    baseClient.update(entity);

    mockGetEntity(entityKey, entity);
    entitySaved = baseClient.get(key);
    assertTrue(entity.lenientEquals(entitySaved));

    // delete
    doNothing().when(getMockCollectionEntityService()).delete(key);
    baseClient.delete(key);

    entity.setDeleted(new Date());
    mockGetEntity(entityKey, entity);
    entitySaved = baseClient.get(key);
    assertTrue(entity.lenientEquals(entitySaved));
  }

  @Test
  public void updateKeysMismatchTest() throws Exception {
    T entity = testData.newEntity();
    entity.setKey(UUID.randomUUID());

    String path =
        "/grscicoll/"
            + paramType.getSimpleName().toLowerCase()
            + "/"
            + UUID.randomUUID().toString();

    requestTestFixture
        .putRequest(
            TestConstants.TEST_GRSCICOLL_ADMIN, TestConstants.TEST_GRSCICOLL_ADMIN, entity, path)
        .andExpect(status().isBadRequest());
  }

  @Test
  public void emptyOffsetTest() throws Exception {
    mockMvc
        .perform(
            get("/grscicoll/" + paramType.getSimpleName().toLowerCase()).queryParam("offset", ""))
        .andExpect(status().is2xxSuccessful());
  }

  @Test
  public void getMissingEntity() throws Exception {
    UUID key = UUID.randomUUID();
    mockGetEntity(key, null);

    requestTestFixture
        .getRequest(
            "/grscicoll/"
                + paramType.getSimpleName().toLowerCase()
                + "/"
                + UUID.randomUUID().toString())
        .andExpect(status().isNotFound());
  }

  @Test
  public void getFullEntityTest() {
    T entity = testData.newEntity();
    entity.setKey(UUID.randomUUID());

    MachineTag machineTag = new MachineTag("ns", "name", "value");
    entity.setMachineTags(Collections.singletonList(machineTag));

    Tag tag = new Tag();
    tag.setValue("value");
    entity.setTags(Collections.singletonList(tag));

    Identifier identifier = new Identifier();
    identifier.setIdentifier("id");
    identifier.setType(IdentifierType.LSID);
    entity.setIdentifiers(Collections.singletonList(identifier));

    mockGetEntity(entity.getKey(), entity);
    T entitySaved = baseClient.get(entity.getKey());

    assertEquals(1, entitySaved.getMachineTags().size());
    assertEquals("value", entitySaved.getMachineTags().get(0).getValue());
    assertEquals(1, entitySaved.getTags().size());
    assertEquals("value", entitySaved.getTags().get(0).getValue());
    assertEquals(1, entitySaved.getIdentifiers().size());
    assertEquals("id", entitySaved.getIdentifiers().get(0).getIdentifier());
    assertEquals(IdentifierType.LSID, entitySaved.getIdentifiers().get(0).getType());
  }

  @Test
  public void tagsTest() {
    UUID key = UUID.randomUUID();
    T entity = testData.newEntity();
    entity.setKey(key);
    Tag tag = new Tag();
    tag.setValue("value");

    int tagKey = 1;
    when(getMockCollectionEntityService().addTag(key, tag)).thenReturn(tagKey);
    int returnedKey = baseClient.addTag(key, tag);
    assertEquals(tagKey, returnedKey);
    tag.setKey(returnedKey);

    when(getMockCollectionEntityService().listTags(key, null))
        .thenReturn(Collections.singletonList(tag));
    // mock call in ResourceNotFoundRequestFilter
    when(resourceNotFoundService.entityExists(any(), any())).thenReturn(true);

    List<Tag> tagsReturned = baseClient.listTags(key, null);
    assertEquals(1, tagsReturned.size());
    assertEquals(tagKey, tagsReturned.get(0).getKey());
    assertEquals("value", tagsReturned.get(0).getValue());

    doNothing().when(getMockCollectionEntityService()).deleteTag(key, tagKey);
    assertDoesNotThrow(() -> baseClient.deleteTag(key, tagKey));
  }

  @Test
  public void machineTagsTest() {
    UUID entityKey = UUID.randomUUID();
    T entity = testData.newEntity();
    entity.setKey(entityKey);
    MachineTag machineTag = new MachineTag("ns", "name", "value");
    int machineTagKey = 1;
    when(getMockCollectionEntityService().addMachineTag(entityKey, machineTag))
        .thenReturn(machineTagKey);

    int machineTagKeyReturned = baseClient.addMachineTag(entityKey, machineTag);
    assertEquals(machineTagKey, machineTagKeyReturned);
    machineTag.setKey(machineTagKeyReturned);

    when(getMockCollectionEntityService().listMachineTags(entityKey))
        .thenReturn(Collections.singletonList(machineTag));
    // mock call in ResourceNotFoundRequestFilter
    when(resourceNotFoundService.entityExists(any(), any())).thenReturn(true);

    List<MachineTag> machineTags = baseClient.listMachineTags(entityKey);
    assertEquals(1, machineTags.size());
    assertEquals(machineTagKey, machineTags.get(0).getKey());
    assertEquals("value", machineTags.get(0).getValue());

    doNothing().when(getMockCollectionEntityService()).deleteMachineTag(entityKey, machineTagKey);
    assertDoesNotThrow(() -> baseClient.deleteMachineTag(entityKey, machineTagKey));
  }

  @Test
  public void identifiersTest() {
    UUID entityKey = UUID.randomUUID();

    Identifier identifier = new Identifier();
    identifier.setIdentifier("identifier");
    identifier.setType(IdentifierType.LSID);

    int identifierKey = 1;
    when(getMockCollectionEntityService().addIdentifier(entityKey, identifier))
        .thenReturn(identifierKey);
    int identifierKeyReturned = baseClient.addIdentifier(entityKey, identifier);
    assertEquals(identifierKey, identifierKeyReturned);
    identifier.setKey(identifierKeyReturned);

    when(getMockCollectionEntityService().listIdentifiers(entityKey))
        .thenReturn(Collections.singletonList(identifier));
    // mock call in ResourceNotFoundRequestFilter
    when(resourceNotFoundService.entityExists(any(), any())).thenReturn(true);

    List<Identifier> identifiers = baseClient.listIdentifiers(entityKey);
    assertEquals(1, identifiers.size());
    assertEquals(identifierKey, identifiers.get(0).getKey());
    assertEquals("identifier", identifiers.get(0).getIdentifier());
    assertEquals(IdentifierType.LSID, identifiers.get(0).getType());

    doNothing().when(getMockCollectionEntityService()).deleteIdentifier(entityKey, identifierKey);
    assertDoesNotThrow(() -> baseClient.deleteIdentifier(entityKey, identifierKey));
  }

  @Test
  public void commentsTest() {
    UUID entityKey = UUID.randomUUID();

    Comment comment = new Comment();
    comment.setContent("test comment");
    int commentKey = 1;
    when(getMockCollectionEntityService().addComment(entityKey, comment)).thenReturn(commentKey);
    int commentKeyReturned = baseClient.addComment(entityKey, comment);
    assertEquals(commentKey, commentKeyReturned);
    comment.setKey(commentKey);

    when(getMockCollectionEntityService().listComments(entityKey))
        .thenReturn(Collections.singletonList(comment));
    // mock call in ResourceNotFoundRequestFilter
    when(resourceNotFoundService.entityExists(any(), any())).thenReturn(true);

    List<Comment> comments = baseClient.listComments(entityKey);
    assertEquals(1, comments.size());
    assertEquals(commentKey, comments.get(0).getKey());
    assertEquals(comment.getContent(), comments.get(0).getContent());

    doNothing().when(getMockCollectionEntityService()).deleteComment(entityKey, commentKey);
    assertDoesNotThrow(() -> baseClient.deleteComment(entityKey, commentKey));
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
    when(getMockCollectionEntityService().addContactPerson(any(UUID.class), any(Contact.class)))
        .thenReturn(1);
    assertDoesNotThrow(
        () -> getPrimaryCollectionEntityClient().addContactPerson(UUID.randomUUID(), contact));

    // list contacts
    when(getMockCollectionEntityService().listContactPersons(any(UUID.class)))
        .thenReturn(Arrays.asList(contact, contact2));
    // mock call in ResourceNotFoundRequestFilter
    when(resourceNotFoundService.entityExists(any(), any())).thenReturn(true);

    List<Contact> contactsEntity1 =
        getPrimaryCollectionEntityClient().listContactPersons(UUID.randomUUID());
    assertEquals(2, contactsEntity1.size());

    // update contact
    doNothing()
        .when(getMockCollectionEntityService())
        .updateContactPerson(any(UUID.class), any(Contact.class));
    assertDoesNotThrow(
        () -> getPrimaryCollectionEntityClient().updateContactPerson(UUID.randomUUID(), contact));

    // remove contacts
    doNothing()
        .when(getMockCollectionEntityService())
        .removeContactPerson(any(UUID.class), anyInt());
    assertDoesNotThrow(
        () -> getPrimaryCollectionEntityClient().removeContactPerson(UUID.randomUUID(), 1));
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
    when(getMockCollectionEntityService()
            .addOccurrenceMapping(any(UUID.class), eq(occurrenceMapping)))
        .thenReturn(key);
    int occurrenceMappingKey =
        getPrimaryCollectionEntityClient()
            .addOccurrenceMapping(UUID.randomUUID(), occurrenceMapping);
    assertEquals(key, occurrenceMappingKey);
    occurrenceMapping.setKey(occurrenceMappingKey);

    when(getMockCollectionEntityService().listOccurrenceMappings(any(UUID.class)))
        .thenReturn(Collections.singletonList(occurrenceMapping));
    // mock call in ResourceNotFoundRequestFilter
    when(resourceNotFoundService.entityExists(any(), any())).thenReturn(true);

    List<OccurrenceMapping> mappings =
        getPrimaryCollectionEntityClient().listOccurrenceMappings(UUID.randomUUID());
    assertEquals(1, mappings.size());

    doNothing()
        .when(getMockCollectionEntityService())
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

  protected BaseCollectionEntityClient<T, R> getPrimaryCollectionEntityClient() {
    return (BaseCollectionEntityClient<T, R>) baseClient;
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

  @Test
  public void getSourceableFieldsTest() {
    assertFalse(getPrimaryCollectionEntityClient().getSourceableFields().isEmpty());
  }

  @Test
  public void masterSourceMetadataTest() {
    MasterSourceMetadata metadata = new MasterSourceMetadata(Source.IH_IRN, "123");

    int key = 1;
    when(getMockCollectionEntityService().addMasterSourceMetadata(any(UUID.class), eq(metadata)))
        .thenReturn(key);
    // mock call in ResourceNotFoundRequestFilter
    when(resourceNotFoundService.entityExists(any(), any())).thenReturn(true);

    int metadataKey =
        getPrimaryCollectionEntityClient().addMasterSourceMetadata(UUID.randomUUID(), metadata);
    assertEquals(key, metadataKey);
    metadata.setKey(metadataKey);

    when(getMockCollectionEntityService().getMasterSourceMetadata(any(UUID.class)))
        .thenReturn(metadata);

    MasterSourceMetadata masterSourceMetadata =
        getPrimaryCollectionEntityClient().getMasterSourceMetadata(UUID.randomUUID());
    assertEquals(metadata, masterSourceMetadata);

    doNothing().when(getMockCollectionEntityService()).deleteMasterSourceMetadata(any(UUID.class));
    assertDoesNotThrow(
        () -> getPrimaryCollectionEntityClient().deleteMasterSourceMetadata(UUID.randomUUID()));
  }

  @Test
  public void importBatchTest() throws Exception {
    int key = 1;
    when(batchService.handleBatch(any(), any(), any(), anyBoolean())).thenReturn(key);

    Resource collectionsResource = new ClassPathResource("collections/collection_import.csv");
    Resource contactsResource = new ClassPathResource("collections/collection_contacts_import.csv");

    resetSecurityContext("admin", UserRole.GRSCICOLL_ADMIN);

    MockMultipartFile entitiesFile =
        new MockMultipartFile("entitiesFile", collectionsResource.getInputStream());
    MockMultipartFile contactsFile =
        new MockMultipartFile("contactsFile", contactsResource.getInputStream());

    mockMvc
        .perform(
            MockMvcRequestBuilders.multipart(
                    "/grscicoll/" + paramType.getSimpleName().toLowerCase() + "/batch")
                .file(entitiesFile)
                .file(contactsFile)
                .param("format", ExportFormat.CSV.name())
                .with(
                    httpBasic(
                        TestConstants.TEST_GRSCICOLL_ADMIN, TestConstants.TEST_GRSCICOLL_ADMIN))
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isCreated())
        .andExpect(
            MockMvcResultMatchers.content().string(StringContains.containsString("/batch/1")));
  }

  @Test
  public void updateBatchTest() throws Exception {
    int key = 1;
    when(batchService.handleBatch(any(), any(), any(), anyBoolean())).thenReturn(key);

    Resource collectionsResource = new ClassPathResource("collections/collection_import.csv");
    Resource contactsResource = new ClassPathResource("collections/collection_contacts_import.csv");

    resetSecurityContext("admin", UserRole.GRSCICOLL_ADMIN);

    MockMultipartFile entitiesFile =
        new MockMultipartFile("entitiesFile", collectionsResource.getInputStream());
    MockMultipartFile contactsFile =
        new MockMultipartFile("contactsFile", contactsResource.getInputStream());

    mockMvc
        .perform(
            MockMvcRequestBuilders.multipart(
                    "/grscicoll/" + paramType.getSimpleName().toLowerCase() + "/batch")
                .file(entitiesFile)
                .file(contactsFile)
                .param("format", ExportFormat.CSV.name())
                .with(
                    r -> {
                      r.setMethod(HttpMethod.PUT.name());
                      return r;
                    })
                .with(
                    httpBasic(
                        TestConstants.TEST_GRSCICOLL_ADMIN, TestConstants.TEST_GRSCICOLL_ADMIN))
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isCreated())
        .andExpect(
            header()
                .string(
                    "Location",
                    StringEndsWith.endsWith(
                        "/grscicoll/" + paramType.getSimpleName().toLowerCase() + "/batch/1")))
        .andExpect(MockMvcResultMatchers.content().string(StringEndsWith.endsWith("/batch/1")));
  }

  @Test
  public void getBatchTest() throws Exception {
    Batch batch = new Batch();
    batch.setKey(1);
    batch.setState(Batch.State.FINISHED);
    batch.setOperation(Batch.Operation.CREATE);

    Resource collectionsResource = new ClassPathResource("collections/collection_import.csv");
    batch.setResultFilePath(collectionsResource.getFile().getAbsolutePath());

    when(batchService.get(anyInt())).thenReturn(batch);

    mockMvc
        .perform(
            get(
                "/grscicoll/"
                    + paramType.getSimpleName().toLowerCase()
                    + "/batch/"
                    + batch.getKey()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("key").value(batch.getKey()))
        .andExpect(jsonPath("state").value(batch.getState()));
  }

  @Test
  public void getBatchResultFileTest() throws Exception {
    Batch batch = new Batch();
    batch.setKey(1);
    batch.setState(Batch.State.FINISHED);
    batch.setOperation(Batch.Operation.CREATE);

    Resource collectionsResource = new ClassPathResource("collections/collection_import.csv");
    batch.setResultFilePath(collectionsResource.getFile().getAbsolutePath());

    when(batchService.get(anyInt())).thenReturn(batch);

    mockMvc
        .perform(
            get(
                "/grscicoll/"
                    + paramType.getSimpleName().toLowerCase()
                    + "/batch/"
                    + batch.getKey()
                    + "/resultFile"))
        .andExpect(status().isOk())
        .andExpect(header().exists("Content-Disposition"));
  }

  void mockGetEntity(UUID key, T entityToReturn) {
    when(getMockCollectionEntityService().get(key)).thenReturn(entityToReturn);
  }

  protected abstract CollectionEntityService<T> getMockCollectionEntityService();

  protected abstract DuplicatesService getMockDuplicatesService();

  protected abstract MergeService<T> getMockMergeService();

  protected abstract ChangeSuggestionService<T, R> getMockChangeSuggestionService();

  protected abstract R newChangeSuggestion();
}
