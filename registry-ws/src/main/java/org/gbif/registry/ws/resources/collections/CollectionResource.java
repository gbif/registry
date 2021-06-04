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
package org.gbif.registry.ws.resources.collections;

import org.gbif.api.annotation.NullToNotFound;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.request.CollectionSearchRequest;
import org.gbif.api.model.collections.suggestions.CollectionChangeSuggestion;
import org.gbif.api.model.collections.view.CollectionView;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.registry.service.collections.duplicates.CollectionDuplicatesService;
import org.gbif.registry.service.collections.merge.CollectionMergeService;
import org.gbif.registry.service.collections.suggestions.CollectionChangeSuggestionService;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Class that acts both as the WS endpoint for {@link Collection} entities and also provides an
 * implementation of {@link CollectionService}.
 */
@RestController
@RequestMapping(value = "grscicoll/collection", produces = MediaType.APPLICATION_JSON_VALUE)
public class CollectionResource
    extends PrimaryCollectionEntityResource<Collection, CollectionChangeSuggestion> {

  public final CollectionService collectionService;

  public CollectionResource(
      CollectionMergeService collectionMergeService,
      CollectionDuplicatesService duplicatesService,
      CollectionService collectionService,
      CollectionChangeSuggestionService collectionChangeSuggestionService) {
    super(
        collectionMergeService,
        collectionService,
        collectionChangeSuggestionService,
        duplicatesService,
        Collection.class);
    this.collectionService = collectionService;
  }

  @GetMapping("{key}")
  @NullToNotFound("/grscicoll/collection/{key}")
  public CollectionView getCollectionView(@PathVariable UUID key) {
    return collectionService.getCollectionView(key);
  }

  @GetMapping
  public PagingResponse<CollectionView> list(CollectionSearchRequest searchRequest) {
    return collectionService.list(searchRequest);
  }

  @GetMapping("deleted")
  public PagingResponse<CollectionView> listDeleted(Pageable page) {
    return collectionService.listDeleted(page);
  }

  @GetMapping("suggest")
  public List<KeyCodeNameResult> suggest(@RequestParam(value = "q", required = false) String q) {
    return collectionService.suggest(q);
  }
}
