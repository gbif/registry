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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.AlternativeCode;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.UserId;
import org.gbif.api.model.collections.suggestions.CollectionChangeSuggestion;
import org.gbif.api.model.collections.suggestions.Status;
import org.gbif.api.model.collections.suggestions.Type;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.api.vocabulary.collections.IdType;
import org.gbif.api.vocabulary.collections.MasterSourceType;
import org.gbif.registry.service.collections.suggestions.CollectionChangeSuggestionService;
import static org.gbif.registry.service.collections.utils.MasterSourceUtils.IH_SYNC_USER;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.net.URI;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Tests the {@link CollectionChangeSuggestionService}. */
public class CollectionChangeSuggestionServiceIT
    extends BaseChangeSuggestionServiceIT<Collection, CollectionChangeSuggestion> {

  CollectionChangeSuggestionService collectionChangeSuggestionService;
  CollectionService collectionService;
  InstitutionService institutionService;

  @Autowired
  public CollectionChangeSuggestionServiceIT(
      SimplePrincipalProvider simplePrincipalProvider,
      CollectionChangeSuggestionService collectionChangeSuggestionService,
      CollectionService collectionService,
      InstitutionService institutionService) {
    super(
        simplePrincipalProvider,
        collectionChangeSuggestionService,
        collectionService,
        collectionService);
    this.institutionService = institutionService;
    this.collectionChangeSuggestionService = collectionChangeSuggestionService;
    this.collectionService = collectionService;
  }

  @Override
  Collection createEntity() {
    Collection c1 = new Collection();
    c1.setCode(UUID.randomUUID().toString());
    c1.setName(UUID.randomUUID().toString());
    return c1;
  }

  @Override
  int updateEntity(Collection entity) {
    entity.setCode(UUID.randomUUID().toString());
    entity.setActive(true);
    entity.setApiUrls(Collections.singletonList(URI.create("http://test.com")));
    entity.setIdentifiers(Collections.singletonList(new Identifier(IdentifierType.LSID, "test")));
    entity.setAlternativeCodes(
        Collections.singletonList(new AlternativeCode(UUID.randomUUID().toString(), "test")));
    return 4;
  }

  @Override
  CollectionChangeSuggestion createEmptyChangeSuggestion() {
    return new CollectionChangeSuggestion();
  }

  @Test
  public void newEntitySuggestionFromIhSyncTest() {
    resetSecurityContext(IH_SYNC_USER, UserRole.GRSCICOLL_ADMIN);
    // State
    Collection entity = createEntity();
    entity.setMasterSource(MasterSourceType.IH);
    entity.setDisplayOnNHCPortal(true);
    entity.setIdentifiers(Collections.singletonList(new Identifier(IdentifierType.IH_IRN, "123456")));

    Address address = new Address();
    address.setCountry(Country.DENMARK);
    entity.setAddress(address);

    Address emptyAddress = new Address();
    entity.setMailingAddress(emptyAddress);

    Contact contact = new Contact();
    contact.setFirstName("first");
    contact.setLastName("last");
    contact.setEmail(Collections.singletonList("aa@aa.com"));
    contact.getUserIds().add(new UserId(IdType.OTHER, "12345"));
    contact.getUserIds().add(new UserId(IdType.OTHER, "abcde"));
    entity.getContactPersons().add(contact);

    CollectionChangeSuggestion suggestion = createEmptyChangeSuggestion();
    suggestion.setSuggestedEntity(entity);
    suggestion.setType(Type.CREATE);
    suggestion.setProposerEmail(PROPOSER);
    suggestion.setComments(Collections.singletonList("comment"));
    suggestion.setCreateInstitution(true);
    suggestion.setIhIdentifier("gbif:ih:irn:123456");

    // When
    int suggKey = collectionChangeSuggestionService.createChangeSuggestion(suggestion);

    // Then
    suggestion = collectionChangeSuggestionService.getChangeSuggestion(suggKey);
    assertCreatedSuggestion(suggestion);
    assertEquals(Type.CREATE, suggestion.getType());
    assertEquals(Country.DENMARK, suggestion.getEntityCountry());
    assertEquals(entity.getName(), suggestion.getEntityName());
    int changes = suggestion.getChanges().size();
    assertTrue(changes > 0);
    // When - update the suggestion (e.g.: the reviewer does some changes)
    suggestion.setSuggestedEntity(entity);
    suggestion.getComments().add("Review");

    contact.setFirstName("first2"); //locked field
    collectionChangeSuggestionService.updateChangeSuggestion(suggestion);

    // Then
    suggestion = collectionChangeSuggestionService.getChangeSuggestion(suggKey);
    assertTrue(entity.lenientEquals(suggestion.getSuggestedEntity()));
    assertEquals(changes, suggestion.getChanges().size());
    assertEquals(2, suggestion.getComments().size());
    assertTrue(suggestion.getSuggestedEntity().getContactPersons().stream()
      .noneMatch(c -> c.getFirstName().equals("first2")));

    // When
    collectionChangeSuggestionService.applyChangeSuggestion(suggKey);

    // Then
    suggestion = collectionChangeSuggestionService.getChangeSuggestion(suggKey);
    assertEquals(Status.APPLIED, suggestion.getStatus());
    assertNotNull(suggestion.getEntityKey());
    assertNotNull(suggestion.getApplied());
    assertNotNull(suggestion.getAppliedBy());

    Collection appliedEntity = collectionService.get(suggestion.getEntityKey());
    Institution createdInstitution = institutionService.get(appliedEntity.getInstitutionKey());
    Collection expected = suggestion.getSuggestedEntity();
    expected.setKey(suggestion.getEntityKey());
    expected.getAddress().setKey(appliedEntity.getAddress().getKey());
    assertTrue(appliedEntity.getAddress().lenientEquals(expected.getAddress()));
    assertEquals(appliedEntity.getCode(), expected.getCode());
    assertEquals(appliedEntity.getName(),expected.getName());
    assertEquals(createdInstitution.getKey(), appliedEntity.getInstitutionKey());
  }
}
