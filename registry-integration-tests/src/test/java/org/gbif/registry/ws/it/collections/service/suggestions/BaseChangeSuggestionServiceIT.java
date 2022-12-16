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
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.collections.Contactable;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.MasterSourceMetadata;
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
import org.gbif.api.service.collections.CollectionEntityService;
import org.gbif.api.service.collections.ContactService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.api.vocabulary.collections.IdType;
import org.gbif.api.vocabulary.collections.MasterSourceType;
import org.gbif.api.vocabulary.collections.Source;
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
        T extends CollectionEntity & Contactable & LenientEquals<T>, R extends ChangeSuggestion<T>>
    extends BaseServiceIT {

  protected static final String PROPOSER = "proposer@test.com";
  protected static final Pageable DEFAULT_PAGE = new PagingRequest(0L, 5);

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule = new TestCaseDatabaseInitializer()
       ;

  private final ChangeSuggestionService<T, R> changeSuggestionService;
  private final CollectionEntityService<T> collectionEntityService;
  private final ContactService contactService;

  protected BaseChangeSuggestionServiceIT(
      SimplePrincipalProvider simplePrincipalProvider,
      ChangeSuggestionService<T, R> changeSuggestionService,
      CollectionEntityService<T> collectionEntityService,
      ContactService contactService) {
    super(simplePrincipalProvider);
    this.changeSuggestionService = changeSuggestionService;
    this.collectionEntityService = collectionEntityService;
    this.contactService = contactService;
  }

  @Test
  public void newEntitySuggestionTest() {
    // State
    T entity = createEntity();
    entity.setMasterSource(MasterSourceType.GRSCICOLL);
    entity.setDisplayOnNHCPortal(true);

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

    T appliedEntity = collectionEntityService.get(suggestion.getEntityKey());
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

    UUID entityKey = collectionEntityService.create(entity);

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
    numberChanges++;

    // remove one contact
    suggestion.getSuggestedEntity().getContactPersons().remove(contact2);
    numberChanges++;

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
    T currentEntity = collectionEntityService.get(entityKey);
    currentEntity.setCode(entity.getCode());
    collectionEntityService.update(currentEntity);

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

    T applied = collectionEntityService.get(suggestion.getEntityKey());
    assertEquals(2, applied.getContactPersons().size());
    assertTrue(
        applied.getContactPersons().stream()
            .anyMatch(
                c ->
                    c.getKey().equals(contact1.getKey())
                        && c.getFirstName().equals(contact1.getFirstName())));
    assertTrue(
        applied.getContactPersons().stream()
            .anyMatch(c -> c.getFirstName().equals(contact3.getFirstName())));
    assertTrue(
        applied.getContactPersons().stream().noneMatch(c -> c.getKey().equals(contact2.getKey())));
  }

  @Test
  public void masterSourceSuggestionsTest() {
    // State
    T entity = createEntity();
    UUID entityKey = collectionEntityService.create(entity);

    Contact contact1 = new Contact();
    contact1.setFirstName("first");
    entity.getContactPersons().add(contact1);
    contactService.addContactPerson(entityKey, contact1);

    collectionEntityService.addMasterSourceMetadata(
        entityKey, new MasterSourceMetadata(Source.IH_IRN, "14"));

    // update entity
    entity.setName("another different name");

    // this shouldn't change when the suggestion is applied
    entity.setMasterSource(MasterSourceType.GBIF_REGISTRY);

    // add one contact (shouldn't be added)
    Contact newContact = new Contact();
    newContact.setFirstName("second");
    entity.getContactPersons().add(newContact);

    R suggestion = createEmptyChangeSuggestion();
    suggestion.setSuggestedEntity(entity);
    suggestion.setType(Type.UPDATE);
    suggestion.setEntityKey(entityKey);
    suggestion.setProposerEmail(PROPOSER);
    suggestion.setComments(Collections.singletonList("comment"));

    // When
    int suggKey = changeSuggestionService.createChangeSuggestion(suggestion);
    changeSuggestionService.applyChangeSuggestion(suggKey);

    // Then
    T entityUpdated = collectionEntityService.get(entityKey);

    assertEquals(MasterSourceType.IH, entityUpdated.getMasterSource());
    assertEquals(1, entityUpdated.getContactPersons().size());

    // When, Then
    R deleteSuggestion = createEmptyChangeSuggestion();
    deleteSuggestion.setEntityKey(entityKey);
    deleteSuggestion.setType(Type.DELETE);
    assertThrows(
        IllegalArgumentException.class,
        () -> changeSuggestionService.createChangeSuggestion(deleteSuggestion));
  }

  @Test
  public void deleteInstitutionSuggestionTest() {
    // State
    T entity = createEntity();

    Address address = new Address();
    address.setCountry(Country.DENMARK);
    entity.setAddress(address);

    UUID entityKey = collectionEntityService.create(entity);

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
    T appliedEntity = collectionEntityService.get(entityKey);
    assertNotNull(appliedEntity.getDeleted());
  }

  @Test
  public void mergeInstitutionSuggestionTest() {
    // State
    T entity = createEntity();

    Address address = new Address();
    address.setCountry(Country.DENMARK);
    entity.setAddress(address);

    UUID entityKey = collectionEntityService.create(entity);

    T entity2 = createEntity();
    UUID entity2Key = collectionEntityService.create(entity2);

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

    T appliedEntity = collectionEntityService.get(entityKey);
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
    UUID entity2Key = collectionEntityService.create(entity2);
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

  @Test
  public void updateContactsTest() {
    // State
    T entity = createEntity();

    Address address = new Address();
    address.setCountry(Country.DENMARK);
    entity.setAddress(address);

    UUID entityKey = collectionEntityService.create(entity);

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

    entity = collectionEntityService.get(entityKey);

    // suggestion to change contact1
    entity.getContactPersons().stream()
        .filter(c -> c.getKey().equals(contact1.getKey()))
        .findFirst()
        .get()
        .setFirstName("11");

    R suggestion1 = createEmptyChangeSuggestion();
    suggestion1.setSuggestedEntity(entity);
    suggestion1.setType(Type.UPDATE);
    suggestion1.setEntityKey(entityKey);
    suggestion1.setProposerEmail(PROPOSER);
    suggestion1.setComments(Collections.singletonList("contact1"));

    // When
    int sugg1Key = changeSuggestionService.createChangeSuggestion(suggestion1);

    // Then
    suggestion1 = changeSuggestionService.getChangeSuggestion(sugg1Key);
    assertEquals(1, suggestion1.getChanges().size());

    // suggestion to change contact2
    // it's done from the current entity since the suggestion1 hasn't been applied yet
    entity = collectionEntityService.get(entityKey);
    entity.getContactPersons().stream()
        .filter(c -> c.getKey().equals(contact2.getKey()))
        .findFirst()
        .get()
        .setFirstName("22");

    R suggestion2 = createEmptyChangeSuggestion();
    suggestion2.setSuggestedEntity(entity);
    suggestion2.setType(Type.UPDATE);
    suggestion2.setEntityKey(entityKey);
    suggestion2.setProposerEmail(PROPOSER);
    suggestion2.setComments(Collections.singletonList("contact2"));

    // When
    int sugg2Key = changeSuggestionService.createChangeSuggestion(suggestion2);

    // Then
    suggestion2 = changeSuggestionService.getChangeSuggestion(sugg2Key);
    assertEquals(1, suggestion2.getChanges().size());

    // When
    changeSuggestionService.applyChangeSuggestion(sugg1Key);

    // Then
    T applied = collectionEntityService.get(suggestion1.getEntityKey());
    assertEquals(2, applied.getContactPersons().size());
    assertTrue(applied.getContactPersons().stream().anyMatch(c -> c.getFirstName().equals("11")));
    assertTrue(
        applied.getContactPersons().stream().anyMatch(c -> c.getFirstName().equals("second")));

    // When
    changeSuggestionService.applyChangeSuggestion(sugg2Key);

    // Then
    applied = collectionEntityService.get(suggestion2.getEntityKey());
    assertEquals(2, applied.getContactPersons().size());
    assertTrue(applied.getContactPersons().stream().anyMatch(c -> c.getFirstName().equals("11")));
    assertTrue(applied.getContactPersons().stream().anyMatch(c -> c.getFirstName().equals("22")));
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
