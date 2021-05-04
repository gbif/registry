package org.gbif.registry.ws.it.collections.resource;

import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Commentable;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.LenientEquals;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.service.collections.CrudService;
import org.gbif.api.service.registry.CommentService;
import org.gbif.api.service.registry.IdentifierService;
import org.gbif.api.service.registry.MachineTagService;
import org.gbif.api.service.registry.TagService;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.client.collections.BaseCollectionEntityClient;
import org.gbif.registry.ws.it.collections.data.TestData;
import org.gbif.registry.ws.it.collections.data.TestDataFactory;
import org.gbif.registry.ws.it.fixtures.TestConstants;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

abstract class BaseCollectionEntityResourceIT<
        T extends
            CollectionEntity & Identifiable & Taggable & MachineTaggable & Commentable
                & LenientEquals<T>>
    extends BaseResourceTest {

  protected final BaseCollectionEntityClient<T> baseClient;
  protected final TestData<T> testData;
  protected final Class<T> paramType;

  @Autowired protected MockMvc mockMvc;

  @Autowired protected ObjectMapper objectMapper;

  public BaseCollectionEntityResourceIT(
      Class<? extends BaseCollectionEntityClient<T>> cls,
      SimplePrincipalProvider simplePrincipalProvider,
      EsManageServer esServer,
      Class<T> paramType,
      int localServerPort) {
    super(simplePrincipalProvider, esServer);
    this.baseClient = prepareClient(TestConstants.TEST_GRSCICOLL_ADMIN, localServerPort, cls);
    this.testData = TestDataFactory.create(paramType);
    this.paramType = paramType;
  }

  @Test
  public void crudTest() {
    // create
    T entity = testData.newEntity();
    UUID entityKey = UUID.randomUUID();

    when(getMockCrudService().create(entity)).thenReturn(entityKey);
    UUID key = baseClient.create(entity);
    assertEquals(entityKey, key);

    mockGetEntity(entityKey, entity);
    T entitySaved = baseClient.get(key);
    assertTrue(entity.lenientEquals(entitySaved));
    entitySaved.setKey(key);

    // update
    entity = testData.updateEntity(entitySaved);
    doNothing().when(getMockCrudService()).update(entity);
    baseClient.update(entity);

    mockGetEntity(entityKey, entity);
    entitySaved = baseClient.get(key);
    assertTrue(entity.lenientEquals(entitySaved));

    // delete
    doNothing().when(getMockCrudService()).delete(key);
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

    mockMvc
        .perform(
            put("/grscicoll/"
                    + paramType.getSimpleName().toLowerCase()
                    + "/"
                    + UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(entity))
                .with(
                    httpBasic(
                        TestConstants.TEST_GRSCICOLL_ADMIN, TestConstants.TEST_GRSCICOLL_ADMIN)))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void getMissingEntity() throws Exception {
    UUID key = UUID.randomUUID();
    mockGetEntity(key, null);

    mockMvc
        .perform(
            get(
                "/grscicoll/"
                    + paramType.getSimpleName().toLowerCase()
                    + "/"
                    + UUID.randomUUID().toString()))
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

    Tag tag = new Tag();
    tag.setValue("value");

    int tagKey = 1;
    when(getMockTagService().addTag(key, tag)).thenReturn(tagKey);
    int returnedKey = baseClient.addTag(key, tag);
    assertEquals(tagKey, returnedKey);
    tag.setKey(returnedKey);

    when(getMockTagService().listTags(key, null)).thenReturn(Collections.singletonList(tag));

    List<Tag> tagsReturned = baseClient.listTags(key, null);
    assertEquals(1, tagsReturned.size());
    assertEquals(tagKey, tagsReturned.get(0).getKey());
    assertEquals("value", tagsReturned.get(0).getValue());

    doNothing().when(getMockTagService()).deleteTag(key, tagKey);
    assertDoesNotThrow(() -> baseClient.deleteTag(key, tagKey));
  }

  @Test
  public void machineTagsTest() {
    UUID entityKey = UUID.randomUUID();
    MachineTag machineTag = new MachineTag("ns", "name", "value");
    int machineTagKey = 1;
    when(getMockMachineTagService().addMachineTag(entityKey, machineTag)).thenReturn(machineTagKey);

    int machineTagKeyReturned = baseClient.addMachineTag(entityKey, machineTag);
    assertEquals(machineTagKey, machineTagKeyReturned);
    machineTag.setKey(machineTagKeyReturned);

    when(getMockMachineTagService().listMachineTags(entityKey))
        .thenReturn(Collections.singletonList(machineTag));
    List<MachineTag> machineTags = baseClient.listMachineTags(entityKey);
    assertEquals(1, machineTags.size());
    assertEquals(machineTagKey, machineTags.get(0).getKey());
    assertEquals("value", machineTags.get(0).getValue());

    doNothing().when(getMockMachineTagService()).deleteMachineTag(entityKey, machineTagKey);
    assertDoesNotThrow(() -> baseClient.deleteMachineTag(entityKey, machineTagKey));
  }

  @Test
  public void identifiersTest() {
    UUID entityKey = UUID.randomUUID();

    Identifier identifier = new Identifier();
    identifier.setIdentifier("identifier");
    identifier.setType(IdentifierType.LSID);

    int identifierKey = 1;
    when(getMockIdentifierService().addIdentifier(entityKey, identifier)).thenReturn(identifierKey);
    int identifierKeyReturned = baseClient.addIdentifier(entityKey, identifier);
    assertEquals(identifierKey, identifierKeyReturned);
    identifier.setKey(identifierKeyReturned);

    when(getMockIdentifierService().listIdentifiers(entityKey))
        .thenReturn(Collections.singletonList(identifier));
    List<Identifier> identifiers = baseClient.listIdentifiers(entityKey);
    assertEquals(1, identifiers.size());
    assertEquals(identifierKey, identifiers.get(0).getKey());
    assertEquals("identifier", identifiers.get(0).getIdentifier());
    assertEquals(IdentifierType.LSID, identifiers.get(0).getType());

    doNothing().when(getMockIdentifierService()).deleteIdentifier(entityKey, identifierKey);
    assertDoesNotThrow(() -> baseClient.deleteIdentifier(entityKey, identifierKey));
  }

  @Test
  public void commentsTest() {
    UUID entityKey = UUID.randomUUID();

    Comment comment = new Comment();
    comment.setContent("test comment");
    int commentKey = 1;
    when(getMockCommentService().addComment(entityKey, comment)).thenReturn(commentKey);
    int commentKeyReturned = baseClient.addComment(entityKey, comment);
    assertEquals(commentKey, commentKeyReturned);
    comment.setKey(commentKey);

    when(getMockCommentService().listComments(entityKey))
        .thenReturn(Collections.singletonList(comment));
    List<Comment> comments = baseClient.listComments(entityKey);
    assertEquals(1, comments.size());
    assertEquals(commentKey, comments.get(0).getKey());
    assertEquals(comment.getContent(), comments.get(0).getContent());

    doNothing().when(getMockCommentService()).deleteComment(entityKey, commentKey);
    assertDoesNotThrow(() -> baseClient.deleteComment(entityKey, commentKey));
  }

  void mockGetEntity(UUID key, T entityToReturn) {
    when(getMockCrudService().get(key)).thenReturn(entityToReturn);
  }

  protected abstract CrudService<T> getMockCrudService();

  protected abstract TagService getMockTagService();

  protected abstract MachineTagService getMockMachineTagService();

  protected abstract IdentifierService getMockIdentifierService();

  protected abstract CommentService getMockCommentService();
}
