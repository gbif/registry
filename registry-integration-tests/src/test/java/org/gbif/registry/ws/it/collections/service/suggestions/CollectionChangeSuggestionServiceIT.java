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

import org.gbif.api.model.collections.AlternativeCode;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.suggestions.CollectionChangeSuggestion;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.service.collections.suggestions.CollectionChangeSuggestionService;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.net.URI;
import java.util.Collections;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

/** Tests the {@link CollectionChangeSuggestionService}. */
public class CollectionChangeSuggestionServiceIT
    extends BaseChangeSuggestionServiceIT<Collection, CollectionChangeSuggestion> {

  @Autowired
  public CollectionChangeSuggestionServiceIT(
      SimplePrincipalProvider simplePrincipalProvider,
      CollectionChangeSuggestionService collectionChangeSuggestionService,
      CollectionService collectionService) {
    super(
        simplePrincipalProvider,
        collectionChangeSuggestionService,
        collectionService,
        collectionService);
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
    entity.setApiUrl(URI.create("http://test.com"));
    entity.setIdentifiers(Collections.singletonList(new Identifier(IdentifierType.LSID, "test")));
    entity.setAlternativeCodes(
        Collections.singletonList(new AlternativeCode(UUID.randomUUID().toString(), "test")));
    return 5;
  }

  @Override
  CollectionChangeSuggestion createEmptyChangeSuggestion() {
    return new CollectionChangeSuggestion();
  }
}
