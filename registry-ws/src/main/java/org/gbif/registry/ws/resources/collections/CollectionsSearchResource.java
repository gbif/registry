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

import org.gbif.registry.search.dataset.service.collections.CollectionsSearchService;
import org.gbif.registry.search.dataset.service.collections.SearchResponse;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "grscicoll/search", produces = MediaType.APPLICATION_JSON_VALUE)
public class CollectionsSearchResource {

  private final CollectionsSearchService collectionsSearchService;

  public CollectionsSearchResource(CollectionsSearchService collectionsSearchService) {
    this.collectionsSearchService = collectionsSearchService;
  }

  @GetMapping
  public List<SearchResponse> searchCollections(
      @RequestParam(value = "q", required = false) String query,
      @RequestParam(value = "highlight", defaultValue = "false") boolean highlight) {
    return collectionsSearchService.search(query, highlight);
  }
}
