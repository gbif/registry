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
import org.gbif.api.service.collections.CollectionEntityService;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.identity.service.IdentityService;
import org.gbif.registry.ws.it.collections.data.TestData;
import org.gbif.registry.ws.it.collections.data.TestDataFactory;
import org.gbif.ws.NotFoundException;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

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
    extends BaseServiceIT {

  protected final CollectionEntityService<T> collectionEntityService;
  protected final Class<T> paramType;
  protected final TestData<T> testData;

  public static final Pageable DEFAULT_PAGE = new PagingRequest(0L, 5);

  @RegisterExtension protected CollectionsDatabaseInitializer collectionsDatabaseInitializer;

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule = new TestCaseDatabaseInitializer();

  public BaseCollectionEntityServiceIT(
      CollectionEntityService<T> collectionEntityService,
      SimplePrincipalProvider principalProvider,
      IdentityService identityService,
      Class<T> paramType) {
    super(principalProvider);
    this.collectionEntityService = collectionEntityService;
    this.paramType = paramType;
    this.testData = TestDataFactory.create(paramType);
    collectionsDatabaseInitializer = new CollectionsDatabaseInitializer(identityService);
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
}
