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
package org.gbif.registry.service.collections.lookup.matchers;

import org.gbif.api.model.collections.lookup.CollectionMatched;
import org.gbif.api.model.collections.lookup.LookupParams;
import org.gbif.api.model.collections.lookup.Match;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.persistence.mapper.collections.LookupMapper;
import org.gbif.registry.persistence.mapper.collections.dto.CollectionMatchedDto;
import org.gbif.registry.service.collections.lookup.Matches;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static org.gbif.api.model.collections.lookup.Match.Reason.ALTERNATIVE_CODE_MATCH;
import static org.gbif.api.model.collections.lookup.Match.Reason.CODE_MATCH;
import static org.gbif.api.model.collections.lookup.Match.Reason.COLLECTION_TAG;
import static org.gbif.api.model.collections.lookup.Match.Reason.IDENTIFIER_MATCH;
import static org.gbif.api.model.collections.lookup.Match.Reason.INSTITUTION_TO_COLLECTION_TAG;
import static org.gbif.api.model.collections.lookup.Match.Reason.INST_COLL_MISMATCH;
import static org.gbif.api.model.collections.lookup.Match.Reason.KEY_MATCH;
import static org.gbif.api.model.collections.lookup.Match.Reason.NAME_MATCH;
import static org.gbif.api.model.collections.lookup.Match.exact;
import static org.gbif.api.model.collections.lookup.Match.fuzzy;

@Component
public class CollectionMatcher extends BaseMatcher<CollectionMatchedDto, CollectionMatched> {

  private final CollectionMapper collectionMapper;

  @Autowired
  public CollectionMatcher(
      CollectionMapper collectionMapper,
      DatasetService datasetService,
      @Value("${api.root.url}") String apiBaseUrl) {
    super(datasetService, apiBaseUrl);
    this.collectionMapper = collectionMapper;
  }

  public Matches<CollectionMatched> matchCollections(
      LookupParams params, Set<UUID> institutionMatches) {
    Matches<CollectionMatched> matches = new Matches<>();

    matchWithMachineTags(params.getDatasetKey(), machineTagProcessor(params))
        .ifPresent(matches::setMachineTagMatchesMap);

    if (isEnoughMatches(params, matches)) {
      return setAccepted(matches);
    }

    findExactMatches(params, institutionMatches).ifPresent(matches::setExactMatchesMap);

    if (isEnoughMatches(params, matches)) {
      return setAccepted(matches);
    }

    findFuzzyMatches(params, matches, institutionMatches).ifPresent(matches::setFuzzyMatchesMap);

    return setAccepted(matches);
  }

  private Matches<CollectionMatched> setAccepted(Matches<CollectionMatched> matches) {
    matches.setAcceptedMatch(
        chooseAccepted(
            extractMatches(matches.getMachineTagMatchesMap()),
            extractMatches(matches.getExactMatchesMap()),
            extractMatches(matches.getFuzzyMatchesMap()),
            null,
            m -> !m.getReasons().contains(INST_COLL_MISMATCH),
            Match.Status.AMBIGUOUS_INSTITUTION_MISMATCH));
    return matches;
  }

  private BiConsumer<MachineTag, Map<UUID, Match<CollectionMatched>>> machineTagProcessor(
      LookupParams params) {
    return (mt, matchesMap) -> {
      if (COLLECTION_TAG_NAME.equals(mt.getName())) {
        addMachineTagMatch(mt, params.getCollectionCode(), matchesMap, COLLECTION_TAG);
      } else if (INSTITUTION_TO_COLLECTION_TAG_NAME.equals(mt.getName())) {
        addMachineTagMatch(
            mt, params.getInstitutionCode(), matchesMap, INSTITUTION_TO_COLLECTION_TAG);
      }
    };
  }

  private Optional<Map<UUID, Match<CollectionMatched>>> findExactMatches(
      LookupParams params, Set<UUID> institutionMatches) {
    if (!params.containsCollectionParams()) {
      return Optional.empty();
    }

    Map<UUID, Match<CollectionMatched>> exactMatches = new HashMap<>();
    // find by code and identifier
    List<CollectionMatchedDto> collectionsFoundByCodeAndId =
        findByCodeAndId(params.getCollectionCode(), params.getCollectionId());
    // if found we don't look for more matches since these are exact matches
    collectionsFoundByCodeAndId.forEach(
        dto -> {
          Match<CollectionMatched> exactMatch =
              exact(toEntityMatched(dto), CODE_MATCH, IDENTIFIER_MATCH);
          exactMatches.put(dto.getKey(), exactMatch);
          if (matchesCountry(dto, params.getCountry())) {
            exactMatch.addReason(Match.Reason.COUNTRY_MATCH);
          }
          if (!isMatchWithInstitutions(dto, institutionMatches)) {
            exactMatch.addReason(INST_COLL_MISMATCH);
          }
        });

    return exactMatches.isEmpty() ? Optional.empty() : Optional.of(exactMatches);
  }

  private Optional<Map<UUID, Match<CollectionMatched>>> findFuzzyMatches(
      LookupParams params, Matches<CollectionMatched> matches, Set<UUID> institutionMatches) {
    if (!params.containsCollectionParams()) {
      return Optional.empty();
    }

    Map<UUID, Match<CollectionMatched>> fuzzyMatches = new HashMap<>();
    BiConsumer<List<CollectionMatchedDto>, Match.Reason> addFuzzyCollMatch =
        (l, r) ->
            l.stream()
                .filter(dto -> !matches.getExactMatchesMap().containsKey(dto.getKey()))
                .forEach(
                    dto -> {
                      Match<CollectionMatched> fuzzyMatch =
                          fuzzyMatches
                              .computeIfAbsent(dto.getKey(), k -> fuzzy(toEntityMatched(dto)))
                              .addReason(r);
                      if (matchesCountry(dto, params.getCountry())) {
                        fuzzyMatch.addReason(Match.Reason.COUNTRY_MATCH);
                      }
                      if (!isMatchWithInstitutions(dto, institutionMatches)) {
                        fuzzyMatch.addReason(INST_COLL_MISMATCH);
                      }
                    });

    // find matches by code
    CompletableFuture<List<CollectionMatchedDto>> codeSearch =
        CompletableFuture.supplyAsync(() -> findByCode(params.getCollectionCode()));

    // find matches by identifier
    CompletableFuture<List<CollectionMatchedDto>> identifierSearch =
        CompletableFuture.supplyAsync(() -> findByIdentifier(params.getCollectionId()));

    // find matches by alternative code
    CompletableFuture<List<CollectionMatchedDto>> altCodeSearch =
        CompletableFuture.supplyAsync(() -> findByAlternativeCode(params.getCollectionCode()));

    // find matches using the code and id as name
    CompletableFuture<List<CollectionMatchedDto>> nameSearch =
        CompletableFuture.supplyAsync(
            () -> findByName(params.getCollectionCode(), params.getCollectionId()));

    // find matches by using the id as key
    CompletableFuture<List<CollectionMatchedDto>> keySearch =
        CompletableFuture.supplyAsync(() -> findByKey(params.getCollectionId()));

    CompletableFuture.allOf(codeSearch, identifierSearch, altCodeSearch, nameSearch, keySearch);

    // add results to the matches
    addFuzzyCollMatch.accept(codeSearch.join(), CODE_MATCH);
    addFuzzyCollMatch.accept(identifierSearch.join(), IDENTIFIER_MATCH);
    addFuzzyCollMatch.accept(altCodeSearch.join(), ALTERNATIVE_CODE_MATCH);
    addFuzzyCollMatch.accept(nameSearch.join(), NAME_MATCH);
    addFuzzyCollMatch.accept(keySearch.join(), KEY_MATCH);

    return fuzzyMatches.isEmpty() ? Optional.empty() : Optional.of(fuzzyMatches);
  }

  private boolean isMatchWithInstitutions(CollectionMatchedDto dto, Set<UUID> institutionMatches) {
    return institutionMatches.isEmpty()
        || dto.getInstitutionKey() == null
        || institutionMatches.contains(dto.getInstitutionKey());
  }

  @Override
  LookupMapper<CollectionMatchedDto> getLookupMapper() {
    return collectionMapper;
  }

  @Override
  CollectionMatched toEntityMatched(CollectionMatchedDto dto) {
    CollectionMatched collectionMatched = new CollectionMatched();
    collectionMatched.setKey(dto.getKey());
    collectionMatched.setCode(dto.getCode());
    collectionMatched.setName(dto.getName());
    collectionMatched.setSelf(URI.create(apiBaseUrl + "grscicoll/collection/" + dto.getKey()));
    collectionMatched.setInstitutionKey(dto.getInstitutionKey());
    collectionMatched.setInstitutionCode(dto.getInstitutionCode());
    collectionMatched.setInstitutionName(dto.getInstitutionName());
    collectionMatched.setInstitutionLink(
        URI.create(apiBaseUrl + "grscicoll/institution/" + dto.getInstitutionKey()));
    return collectionMatched;
  }

  @Override
  Predicate<MachineTag> isMachineTagSupported() {
    return mt ->
        mt.getNamespace().equals(PROCESSING_NAMESPACE)
            && Arrays.asList(COLLECTION_TAG_NAME, INSTITUTION_TO_COLLECTION_TAG_NAME)
                .contains(mt.getName());
  }
}
