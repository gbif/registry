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

import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
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
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.identity.service.IdentityService;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.it.collections.data.TestData;
import org.gbif.registry.ws.it.collections.data.TestDataFactory;
import org.gbif.ws.NotFoundException;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.validation.ValidationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    extends BaseServiceTest {

  protected final CrudService<T> crudService;
  protected final Class<T> paramType;
  protected final TestData<T> testData;

  public static final Pageable DEFAULT_PAGE = new PagingRequest(0L, 5);

  @RegisterExtension public CollectionsDatabaseInitializer collectionsDatabaseInitializer;

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule = new TestCaseDatabaseInitializer();

  public BaseCollectionEntityServiceIT(
      CrudService<T> crudService,
      SimplePrincipalProvider principalProvider,
      EsManageServer esServer,
      IdentityService identityService,
      Class<T> paramType) {
    super(principalProvider, esServer);
    this.crudService = crudService;
    this.paramType = paramType;
    this.testData = TestDataFactory.create(paramType);
    collectionsDatabaseInitializer = new CollectionsDatabaseInitializer(identityService);
  }

  @Test
  public void crudTest() {
    // create
    T entity = testData.newEntity();
    UUID key = crudService.create(entity);

    assertNotNull(key);

    T entitySaved = crudService.get(key);
    assertEquals(key, entitySaved.getKey());
    assertTrue(entity.lenientEquals(entitySaved));
    assertNotNull(entitySaved.getCreatedBy());
    assertNotNull(entitySaved.getCreated());
    assertNotNull(entitySaved.getModifiedBy());
    assertNotNull(entitySaved.getModified());

    // update
    entity = testData.updateEntity(entitySaved);
    crudService.update(entity);

    entitySaved = crudService.get(key);
    assertTrue(entity.lenientEquals(entitySaved));
    assertNotEquals(entitySaved.getCreated(), entitySaved.getModified());

    // delete
    crudService.delete(key);
    entitySaved = crudService.get(key);
    assertNotNull(entitySaved.getDeleted());
  }

  @Test
  public void createInvalidEntityTest() {
    assertThrows(ValidationException.class, () -> crudService.create(testData.newInvalidEntity()));
  }

  @Test
  public void deleteMissingEntityTest() {
    assertThrows(IllegalArgumentException.class, () -> crudService.delete(UUID.randomUUID()));
  }

  @Test
  public void updateDeletedEntityTest() {
    T entity = testData.newEntity();
    UUID key = crudService.create(entity);
    entity.setKey(key);
    crudService.delete(key);

    T entity2 = crudService.get(key);
    assertNotNull(entity2.getDeleted());
    assertThrows(IllegalArgumentException.class, () -> crudService.update(entity2));
  }

  @Test
  public void restoreDeletedEntityTest() {
    T entity = testData.newEntity();
    UUID key = crudService.create(entity);
    entity.setKey(key);
    crudService.delete(key);
    entity = crudService.get(key);
    assertNotNull(entity.getDeleted());

    // restore it
    entity.setDeleted(null);
    crudService.update(entity);
    entity = crudService.get(key);
    assertNull(entity.getDeleted());
  }

  @Test
  public void updateInvalidEntityTest() {
    T entity = testData.newEntity();
    UUID key = crudService.create(entity);

    T newEntity = testData.newInvalidEntity();
    newEntity.setKey(key);
    assertThrows(ValidationException.class, () -> crudService.update(newEntity));
  }

  @Test
  public void getMissingEntity() {
    try {
      T entity = crudService.get(UUID.randomUUID());
      assertNull(entity);
    } catch (Exception ex) {
      assertEquals(NotFoundException.class, ex.getClass());
    }
  }

  @Test
  public void createFullEntityTest() {
    T entity = testData.newEntity();

    MachineTag machineTag = new MachineTag("ns", "name", "value");
    entity.setMachineTags(Collections.singletonList(machineTag));

    Tag tag = new Tag();
    tag.setValue("value");
    entity.setTags(Collections.singletonList(tag));

    Identifier identifier = new Identifier();
    identifier.setIdentifier("id");
    identifier.setType(IdentifierType.LSID);
    entity.setIdentifiers(Collections.singletonList(identifier));

    UUID key = crudService.create(entity);
    T entitySaved = crudService.get(key);

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
    TagService tagService = (TagService) crudService;

    UUID key = crudService.create(testData.newEntity());

    Tag tag = new Tag();
    tag.setValue("value");
    int tagKey = tagService.addTag(key, tag);

    List<Tag> tags = tagService.listTags(key, null);
    assertEquals(1, tags.size());
    assertEquals(tagKey, tags.get(0).getKey());
    assertEquals("value", tags.get(0).getValue());

    tagService.deleteTag(key, tagKey);
    assertEquals(0, tagService.listTags(key, null).size());
  }

  @Test
  public void machineTagsTest() {
    MachineTagService machineTagService = (MachineTagService) crudService;

    T entity = testData.newEntity();
    UUID key = crudService.create(entity);

    MachineTag machineTag = new MachineTag("ns", "name", "value");
    int machineTagKey = machineTagService.addMachineTag(key, machineTag);

    List<MachineTag> machineTags = machineTagService.listMachineTags(key);
    assertEquals(1, machineTags.size());
    assertEquals(machineTagKey, machineTags.get(0).getKey());
    assertEquals("value", machineTags.get(0).getValue());

    machineTagService.deleteMachineTag(key, machineTagKey);
    assertEquals(0, machineTagService.listMachineTags(key).size());
  }

  @Test
  public void identifiersTest() {
    IdentifierService identifierService = (IdentifierService) crudService;

    T entity = testData.newEntity();
    UUID key = crudService.create(entity);

    Identifier identifier = new Identifier();
    identifier.setIdentifier("identifier");
    identifier.setType(IdentifierType.LSID);

    int identifierKey = identifierService.addIdentifier(key, identifier);

    List<Identifier> identifiers = identifierService.listIdentifiers(key);
    assertEquals(1, identifiers.size());
    assertEquals(identifierKey, identifiers.get(0).getKey());
    assertEquals("identifier", identifiers.get(0).getIdentifier());
    assertEquals(IdentifierType.LSID, identifiers.get(0).getType());

    identifierService.deleteIdentifier(key, identifierKey);
    assertEquals(0, identifierService.listIdentifiers(key).size());
  }

  @Test
  public void commentsTest() {
    CommentService commentService = (CommentService) crudService;

    T entity = testData.newEntity();
    UUID key = crudService.create(entity);

    Comment comment = new Comment();
    comment.setContent("test comment");

    int commentKey = commentService.addComment(key, comment);

    List<Comment> comments = commentService.listComments(key);
    assertEquals(1, comments.size());
    assertEquals(commentKey, comments.get(0).getKey());
    assertEquals(comment.getContent(), comments.get(0).getContent());

    commentService.deleteComment(key, commentKey);
    assertEquals(0, commentService.listComments(key).size());
  }
}
