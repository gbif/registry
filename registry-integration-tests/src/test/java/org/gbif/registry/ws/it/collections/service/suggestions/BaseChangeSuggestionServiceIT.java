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
package org.gbif.registry.ws.it.collections.service.suggestions;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.collections.Contactable;
import org.gbif.api.model.collections.IdType;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.PrimaryCollectionEntity;
import org.gbif.api.model.collections.UserId;
import org.gbif.api.model.collections.suggestions.Change;
import org.gbif.api.model.collections.suggestions.ChangeSuggestion;
import org.gbif.api.model.collections.suggestions.ChangeSuggestionService;
import org.gbif.api.model.collections.suggestions.Status;
import org.gbif.api.model.collections.suggestions.Type;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.LenientEquals;
import org.gbif.api.service.collections.ContactService;
import org.gbif.api.service.collections.CrudService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.ws.it.collections.service.BaseServiceIT;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Collections;
import java.util.UUID;

import javax.validation.ValidationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the {@link ChangeSuggestionService}. */
public abstract class BaseChangeSuggestionServiceIT<
        T extends PrimaryCollectionEntity & Contactable & LenientEquals<T>,
        R extends ChangeSuggestion<T>>
    extends BaseServiceIT {

  protected static final String PROPOSER = "proposer@test.com";
  protected static final Pageable DEFAULT_PAGE = new PagingRequest(0L, 5);

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule = new TestCaseDatabaseInitializer();

  private final ChangeSuggestionService<T, R> changeSuggestionService;
  private final CrudService<T> crudService;
  private final ContactService contactService;

  protected BaseChangeSuggestionServiceIT(
      SimplePrincipalProvider simplePrincipalProvider,
      ChangeSuggestionService<T, R> changeSuggestionService,
      CrudService<T> crudService,
      ContactService contactService) {
    super(simplePrincipalProvider);
    this.changeSuggestionService = changeSuggestionService;
    this.crudService = crudService;
    this.contactService = contactService;
  }

  @Test
  public void newEntitySuggestionTest() {
    // State
    T entity = createEntity();

    Address address = new Address();
    address.setCountry(Country.DENMARK);
    entity.setAddress(address);

    Contact contact1 = new Contact();
    contact1.setFirstName("first");
    contact1.setLastName("last");
    contact1.setEmail(Collections.singletonList("aa@aa.com"));
    contact1.getUserIds().add(new UserId(IdType.OTHER, "12345"));
    contact1.getUserIds().add(new UserId(IdType.OTHER, "abcde"));
    entity.getContactPersons().add(contact1);

    R suggestion = createEmptyChangeSuggestion();
    suggestion.setSuggestedEntity(entity);
    suggestion.setType(Type.CREATE);
    suggestion.setProposerEmail(PROPOSER);
    suggestion.setComments(Collections.singletonList("comment"));

    // When
    int suggKey = changeSuggestionService.createChangeSuggestion(suggestion);

    // Then
    suggestion = changeSuggestionService.getChangeSuggestion(suggKey);
    assertCreatedSuggestion(suggestion);
    assertEquals(Type.CREATE, suggestion.getType());
    assertEquals(Country.DENMARK, suggestion.getEntityCountry());
    assertEquals(entity.getName(), suggestion.getEntityName());
    int changes = suggestion.getChanges().size();
    assertTrue(changes > 0);

    // When - update the suggestion (e.g.: the reviewer does some changes)
    entity.setCode(UUID.randomUUID().toString());
    suggestion.setSuggestedEntity(entity);
    suggestion.getComments().add("Review");
    changeSuggestionService.updateChangeSuggestion(suggestion);

    // Then
    suggestion = changeSuggestionService.getChangeSuggestion(suggKey);
    assertTrue(entity.lenientEquals(suggestion.getSuggestedEntity()));
    assertEquals(changes + 1, suggestion.getChanges().size());
    assertEquals(2, suggestion.getComments().size());

    // When
    changeSuggestionService.applyChangeSuggestion(suggKey);

    // Then
    suggestion = changeSuggestionService.getChangeSuggestion(suggKey);
    assertEquals(Status.APPLIED, suggestion.getStatus());
    assertNotNull(suggestion.getEntityKey());
    assertNotNull(suggestion.getApplied());
    assertNotNull(suggestion.getAppliedBy());

    T appliedEntity = crudService.get(suggestion.getEntityKey());
    T expected = suggestion.getSuggestedEntity();
    expected.setKey(suggestion.getEntityKey());
    expected.getAddress().setKey(appliedEntity.getAddress().getKey());
    assertTrue(appliedEntity.lenientEquals(expected));
    assertEquals(1, appliedEntity.getContactPersons().size());
  }

  @Test
  public void updateEntityChangeSuggestionTest() {
    // State
    T entity = createEntity();

    Address address = new Address();
    address.setCountry(Country.DENMARK);
    entity.setAddress(address);

    UUID entityKey = crudService.create(entity);

    Contact contact1 = new Contact();
    contact1.setFirstName("first");
    contact1.setLastName("last");
    contact1.setEmail(Collections.singletonList("aa@aa.com"));
    contact1.getUserIds().add(new UserId(IdType.OTHER, "12345"));
    contact1.getUserIds().add(new UserId(IdType.OTHER, "abcde"));
    entity.getContactPersons().add(contact1);
    contactService.addContactPerson(entityKey, contact1);

    Contact contact2 = new Contact();
    contact2.setFirstName("second");
    contact2.setEmail(Collections.singletonList("aa@aa.com"));
    contact2.getUserIds().add(new UserId(IdType.OTHER, "54321"));
    entity.getContactPersons().add(contact2);
    contactService.addContactPerson(entityKey, contact2);

    // suggested changes
    int numberChanges = updateEntity(entity);
    address.setCity("city");

    R suggestion = createEmptyChangeSuggestion();
    suggestion.setSuggestedEntity(entity);
    suggestion.setType(Type.UPDATE);
    suggestion.setEntityKey(entityKey);
    suggestion.setProposerEmail(PROPOSER);
    suggestion.setComments(Collections.singletonList("comment"));

    // update a contact
    contact1.setFirstName("first2");
    numberChanges++;

    // add another contact
    Contact contact3 = new Contact();
    contact3.setFirstName("third");
    contact3.setEmail(Collections.singletonList("aa@aa.com"));
    contact3.getUserIds().add(new UserId(IdType.OTHER, "54321"));
    suggestion.getSuggestedEntity().getContactPersons().add(contact3);

    // remove one contact
    suggestion.getSuggestedEntity().getContactPersons().remove(contact2);

    // When
    int suggKey = changeSuggestionService.createChangeSuggestion(suggestion);

    // Then
    suggestion = changeSuggestionService.getChangeSuggestion(suggKey);
    assertCreatedSuggestion(suggestion);
    assertEquals(Type.UPDATE, suggestion.getType());
    assertEquals(Country.DENMARK, suggestion.getEntityCountry());
    assertEquals(entity.getName(), suggestion.getEntityName());
    assertEquals(address.getCity(), suggestion.getSuggestedEntity().getAddress().getCity());
    assertTrue(entity.lenientEquals(suggestion.getSuggestedEntity()));
    assertEquals(numberChanges, suggestion.getChanges().size());

    // When - update the suggestion (e.g.: the reviewer does some changes)
    entity.setCode(UUID.randomUUID().toString());
    numberChanges++;
    suggestion.setSuggestedEntity(entity);
    suggestion.getComments().add("Review");
    changeSuggestionService.updateChangeSuggestion(suggestion);

    // Then
    suggestion = changeSuggestionService.getChangeSuggestion(suggKey);
    assertEquals(numberChanges, suggestion.getChanges().size());
    assertEquals(2, suggestion.getComments().size());
    assertEquals(1, suggestion.getChanges().stream().filter(Change::isOverwritten).count());

    // When - modify current entity with same change as suggestion
    T currentEntity = crudService.get(entityKey);
    currentEntity.setCode(entity.getCode());
    crudService.update(currentEntity);

    // Then
    suggestion = changeSuggestionService.getChangeSuggestion(suggKey);
    assertEquals(numberChanges, suggestion.getChanges().size());
    assertEquals(1, suggestion.getChanges().stream().filter(Change::isOverwritten).count());
    assertEquals(1, suggestion.getChanges().stream().filter(Change::isOutdated).count());

    // When
    changeSuggestionService.applyChangeSuggestion(suggKey);

    // Then
    suggestion = changeSuggestionService.getChangeSuggestion(suggKey);
    assertEquals(Status.APPLIED, suggestion.getStatus());
    assertNotNull(suggestion.getApplied());
    assertNotNull(suggestion.getAppliedBy());

    T applied = crudService.get(suggestion.getEntityKey());
    assertEquals(2, applied.getContactPersons().size());
    assertTrue(
        applied.getContactPersons().stream()
            .anyMatch(
                c ->
                    c.getKey().equals(contact1.getKey())
                        && c.getFirstName().equals(contact1.getFirstName())));
    assertTrue(
        applied.getContactPersons().stream().anyMatch(c -> c.getFirstName().equals(contact3.getFirstName())));
    assertTrue(
        applied.getContactPersons().stream().noneMatch(c -> c.getKey().equals(contact2.getKey())));
  }

  @Test
  public void deleteInstitutionSuggestionTest() {
    // State
    T entity = createEntity();

    Address address = new Address();
    address.setCountry(Country.DENMARK);
    entity.setAddress(address);

    UUID entityKey = crudService.create(entity);

    R suggestion = createEmptyChangeSuggestion();
    suggestion.setSuggestedEntity(entity);
    suggestion.setType(Type.DELETE);
    suggestion.setEntityKey(entityKey);
    suggestion.setProposerEmail(PROPOSER);
    suggestion.setComments(Collections.singletonList("comment"));

    // When
    int suggKey = changeSuggestionService.createChangeSuggestion(suggestion);

    // Then
    suggestion = changeSuggestionService.getChangeSuggestion(suggKey);
    assertCreatedSuggestion(suggestion);
    assertEquals(Country.DENMARK, suggestion.getEntityCountry());
    assertEquals(entity.getName(), suggestion.getEntityName());
    assertEquals(Type.DELETE, suggestion.getType());

    // When
    changeSuggestionService.applyChangeSuggestion(suggKey);

    // Then
    suggestion = changeSuggestionService.getChangeSuggestion(suggKey);
    assertEquals(Status.APPLIED, suggestion.getStatus());
    assertNotNull(suggestion.getApplied());
    assertNotNull(suggestion.getAppliedBy());
    T appliedEntity = crudService.get(entityKey);
    assertNotNull(appliedEntity.getDeleted());
  }

  @Test
  public void mergeInstitutionSuggestionTest() {
    // State
    T entity = createEntity();

    Address address = new Address();
    address.setCountry(Country.DENMARK);
    entity.setAddress(address);

    UUID entityKey = crudService.create(entity);

    T entity2 = createEntity();
    UUID entity2Key = crudService.create(entity2);

    R suggestion = createEmptyChangeSuggestion();
    suggestion.setSuggestedEntity(entity);
    suggestion.setType(Type.MERGE);
    suggestion.setEntityKey(entityKey);
    suggestion.setProposerEmail(PROPOSER);
    suggestion.setMergeTargetKey(entity2Key);
    suggestion.setComments(Collections.singletonList("comment"));

    // When
    int suggKey = changeSuggestionService.createChangeSuggestion(suggestion);

    // Then
    suggestion = changeSuggestionService.getChangeSuggestion(suggKey);
    assertCreatedSuggestion(suggestion);
    assertEquals(Country.DENMARK, suggestion.getEntityCountry());
    assertEquals(entity.getName(), suggestion.getEntityName());
    assertEquals(Type.MERGE, suggestion.getType());

    // When
    changeSuggestionService.applyChangeSuggestion(suggKey);

    // Then
    suggestion = changeSuggestionService.getChangeSuggestion(suggKey);
    assertEquals(Status.APPLIED, suggestion.getStatus());
    assertNotNull(suggestion.getApplied());
    assertNotNull(suggestion.getAppliedBy());

    T appliedEntity = crudService.get(entityKey);
    assertEquals(entity2Key, getReplacedByValue(appliedEntity));
    assertNotNull(appliedEntity.getDeleted());
  }

  @Test
  public void discardSuggestionTest() {
    // State
    T entity = createEntity();

    R suggestion = createEmptyChangeSuggestion();
    suggestion.setSuggestedEntity(entity);
    suggestion.setType(Type.CREATE);
    suggestion.setProposerEmail(PROPOSER);
    suggestion.setComments(Collections.singletonList("comment"));

    // When
    int suggKey = changeSuggestionService.createChangeSuggestion(suggestion);

    // Then
    suggestion = changeSuggestionService.getChangeSuggestion(suggKey);
    assertCreatedSuggestion(suggestion);
    assertEquals(Type.CREATE, suggestion.getType());

    // When
    changeSuggestionService.discardChangeSuggestion(suggKey);

    // Then
    suggestion = changeSuggestionService.getChangeSuggestion(suggKey);
    assertEquals(Status.DISCARDED, suggestion.getStatus());
    assertNotNull(suggestion.getDiscarded());
    assertNotNull(suggestion.getDiscardedBy());
  }

  @Test
  public void listTest() {
    // State
    T entity = createEntity();
    R suggestion = createEmptyChangeSuggestion();
    suggestion.setSuggestedEntity(entity);
    suggestion.setType(Type.CREATE);
    suggestion.setProposerEmail(PROPOSER);
    suggestion.setComments(Collections.singletonList("comment"));

    int suggKey1 = changeSuggestionService.createChangeSuggestion(suggestion);

    T entity2 = createEntity();
    UUID entity2Key = crudService.create(entity2);
    R suggestion2 = createEmptyChangeSuggestion();
    suggestion2.setSuggestedEntity(entity2);
    suggestion2.setEntityKey(entity2Key);
    suggestion2.setType(Type.UPDATE);
    suggestion2.setProposerEmail(PROPOSER);
    suggestion2.setComments(Collections.singletonList("comment"));

    int suggKey2 = changeSuggestionService.createChangeSuggestion(suggestion2);

    // When
    PagingResponse<R> results =
        changeSuggestionService.list(Status.APPLIED, null, null, null, DEFAULT_PAGE);
    // Then
    assertEquals(0, results.getResults().size());
    assertEquals(0, results.getCount());

    // When
    results = changeSuggestionService.list(null, Type.CREATE, null, null, DEFAULT_PAGE);
    // Then
    assertEquals(1, results.getResults().size());
    assertEquals(1, results.getCount());

    // When
    results = changeSuggestionService.list(null, null, null, entity2Key, DEFAULT_PAGE);
    // Then
    assertEquals(1, results.getResults().size());
    assertEquals(1, results.getCount());

    // When - user with no rights can't see the proposer email
    resetSecurityContext("user", UserRole.USER);
    results = changeSuggestionService.list(null, null, null, entity2Key, DEFAULT_PAGE);
    // Then
    assertTrue(results.getResults().stream().allMatch(v -> v.getProposerEmail() == null));
  }

  @Test
  public void invalidEmailTest() {
    // State
    T entity = createEntity();

    R suggestion = createEmptyChangeSuggestion();
    suggestion.setSuggestedEntity(entity);
    suggestion.setType(Type.CREATE);
    suggestion.setProposerEmail("myfakeemail");
    suggestion.setComments(Collections.singletonList("comment"));

    // When
    assertThrows(
        ValidationException.class,
        () -> changeSuggestionService.createChangeSuggestion(suggestion));
  }

  protected void assertCreatedSuggestion(R created) {
    assertEquals(Status.PENDING, created.getStatus());
    assertNull(created.getApplied());
    assertNull(created.getDiscarded());
    assertEquals(getSimplePrincipalProvider().get().getName(), created.getModifiedBy());
    assertEquals(PROPOSER, created.getProposerEmail());
    assertEquals(1, created.getComments().size());
  }

  protected UUID getReplacedByValue(T entity) {
    if (entity instanceof Institution) {
      return ((Institution) entity).getReplacedBy();
    } else if (entity instanceof Collection) {
      return ((Collection) entity).getReplacedBy();
    } else {
      throw new UnsupportedOperationException();
    }
  }

  abstract T createEntity();

  abstract int updateEntity(T entity);

  abstract R createEmptyChangeSuggestion();
}
