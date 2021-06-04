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
package org.gbif.registry.ws.it.collections.service.suggestions;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.AlternativeCode;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.suggestions.InstitutionChangeSuggestion;
import org.gbif.api.model.collections.suggestions.Type;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.service.collections.suggestions.InstitutionChangeSuggestionService;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Tests the {@link InstitutionChangeSuggestionService}. */
public class InstitutionChangeSuggestionServiceIT
    extends BaseChangeSuggestionServiceIT<Institution, InstitutionChangeSuggestion> {

  private final InstitutionChangeSuggestionService institutionChangeSuggestionService;
  private final InstitutionService institutionService;
  private final CollectionService collectionService;

  @Autowired
  public InstitutionChangeSuggestionServiceIT(
      SimplePrincipalProvider simplePrincipalProvider,
      InstitutionChangeSuggestionService institutionChangeSuggestionService,
      InstitutionService institutionService,
      CollectionService collectionService) {
    super(simplePrincipalProvider, institutionChangeSuggestionService, institutionService);
    this.institutionChangeSuggestionService = institutionChangeSuggestionService;
    this.institutionService = institutionService;
    this.collectionService = collectionService;
  }

  @Test
  public void convertInstitutionToCollectionSuggestionTest() {
    // State
    Institution i1 = new Institution();
    i1.setCode("i1");
    i1.setName("institution 1");

    Address address = new Address();
    address.setCountry(Country.DENMARK);
    i1.setAddress(address);

    UUID i1Key = institutionService.create(i1);

    InstitutionChangeSuggestion suggestion = new InstitutionChangeSuggestion();
    suggestion.setSuggestedEntity(i1);
    suggestion.setType(Type.CONVERSION_TO_COLLECTION);
    suggestion.setEntityKey(i1Key);
    suggestion.setProposerEmail(PROPOSER);
    suggestion.setNameForNewInstitutionForConvertedCollection("newInstitution");
    suggestion.setComments(Collections.singletonList("comment"));

    // When
    int suggKey = institutionChangeSuggestionService.createChangeSuggestion(suggestion);

    // Then
    suggestion = institutionChangeSuggestionService.getChangeSuggestion(suggKey);
    assertCreatedSuggestion(suggestion);
    assertEquals(Country.DENMARK, suggestion.getEntityCountry());
    assertEquals(i1.getName(), suggestion.getEntityName());
    assertEquals(Type.CONVERSION_TO_COLLECTION, suggestion.getType());

    // When
    institutionChangeSuggestionService.applyChangeSuggestion(suggKey);

    // Then
    suggestion = institutionChangeSuggestionService.getChangeSuggestion(suggKey);
    assertNotNull(suggestion.getApplied());
    assertNotNull(suggestion.getAppliedBy());

    Institution appliedInstitution = institutionService.get(i1Key);
    assertNotNull(appliedInstitution.getDeleted());
    UUID collectionKey = appliedInstitution.getConvertedToCollection();
    assertNotNull(collectionKey);
    Collection collectionCreated = collectionService.get(collectionKey);
    assertNotNull(collectionCreated);
    Institution newInstitution = institutionService.get(collectionCreated.getInstitutionKey());
    assertNotNull(newInstitution);
    assertEquals(
        suggestion.getNameForNewInstitutionForConvertedCollection(), newInstitution.getName());
  }

  @Override
  Institution createEntity() {
    Institution i1 = new Institution();
    i1.setCode(UUID.randomUUID().toString());
    i1.setName(UUID.randomUUID().toString());
    return i1;
  }

  @Override
  int updateEntity(Institution entity) {
    entity.setCode(UUID.randomUUID().toString());
    entity.setActive(true);
    entity.setAdditionalNames(Collections.singletonList(UUID.randomUUID().toString()));
    entity.setIdentifiers(Collections.singletonList(new Identifier(IdentifierType.LSID, "test")));
    entity.setAlternativeCodes(
        Collections.singletonList(new AlternativeCode(UUID.randomUUID().toString(), "test")));
    return 5;
  }

  @Override
  InstitutionChangeSuggestion createEmptyChangeSuggestion() {
    return new InstitutionChangeSuggestion();
  }
}
