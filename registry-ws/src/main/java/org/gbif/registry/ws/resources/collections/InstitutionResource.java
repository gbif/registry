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
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.merge.ConvertToCollectionParams;
import org.gbif.api.model.collections.request.InstitutionSearchRequest;
import org.gbif.api.model.collections.suggestions.InstitutionChangeSuggestion;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.registry.service.collections.duplicates.InstitutionDuplicatesService;
import org.gbif.registry.service.collections.merge.InstitutionMergeService;
import org.gbif.registry.service.collections.suggestions.InstitutionChangeSuggestionService;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Class that acts both as the WS endpoint for {@link Institution} entities and also provides an *
 * implementation of {@link InstitutionService}.
 */
@RestController
@RequestMapping(value = "grscicoll/institution", produces = MediaType.APPLICATION_JSON_VALUE)
public class InstitutionResource
    extends PrimaryCollectionEntityResource<Institution, InstitutionChangeSuggestion> {

  private final InstitutionService institutionService;
  private final InstitutionMergeService institutionMergeService;

  public InstitutionResource(
      InstitutionMergeService institutionMergeService,
      InstitutionDuplicatesService duplicatesService,
      InstitutionService institutionService,
      InstitutionChangeSuggestionService institutionChangeSuggestionService) {
    super(
        institutionMergeService,
        institutionService,
        institutionChangeSuggestionService,
        duplicatesService,
        Institution.class);
    this.institutionService = institutionService;
    this.institutionMergeService = institutionMergeService;
  }

  @GetMapping("{key}")
  @NullToNotFound("/grscicoll/institution/{key}")
  public Institution get(@PathVariable UUID key) {
    return institutionService.get(key);
  }

  @GetMapping
  public PagingResponse<Institution> list(InstitutionSearchRequest searchRequest) {
    return institutionService.list(searchRequest);
  }

  @GetMapping("deleted")
  public PagingResponse<Institution> listDeleted(Pageable page) {
    return institutionService.listDeleted(page);
  }

  @GetMapping("suggest")
  public List<KeyCodeNameResult> suggest(@RequestParam(value = "q", required = false) String q) {
    return institutionService.suggest(q);
  }

  @PostMapping("{key}/convertToCollection")
  public UUID convertToCollection(
      @PathVariable("key") UUID entityKey, @RequestBody ConvertToCollectionParams params) {
    return institutionMergeService.convertToCollection(
        entityKey, params.getInstitutionForNewCollectionKey(), params.getNameForNewInstitution());
  }
}
