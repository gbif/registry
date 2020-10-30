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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static org.gbif.api.model.collections.lookup.Match.Reason.COLLECTION_TAG;
import static org.gbif.api.model.collections.lookup.Match.Reason.INSTITUTION_TO_COLLECTION_TAG;
import static org.gbif.api.model.collections.lookup.Match.Reason.INST_COLL_MISMATCH;

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
        .ifPresent(matches::setMachineTagMatches);

    if (isEnoughMatches(params, matches)) {
      return setAccepted(matches);
    }

    List<CollectionMatchedDto> dbMatches =
        getDbMatches(params.getCollectionCode(), params.getCollectionId());

    Set<Match<CollectionMatched>> exactMatches = new HashSet<>();
    Set<Match<CollectionMatched>> fuzzyMatches = new HashSet<>();

    dbMatches.forEach(
        dto -> {
          Match<CollectionMatched> match = createMatch(exactMatches, fuzzyMatches, dto);

          if (matchesCountry(dto, params.getCountry())) {
            match.addReason(Match.Reason.COUNTRY_MATCH);
          }
          if (!isMatchWithInstitutions(dto, institutionMatches)) {
            match.addReason(INST_COLL_MISMATCH);
          }
        });

    matches.setExactMatches(exactMatches);
    matches.setFuzzyMatches(fuzzyMatches);

    return setAccepted(matches);
  }

  private Matches<CollectionMatched> setAccepted(Matches<CollectionMatched> matches) {
    matches.setAcceptedMatch(
        chooseAccepted(
            matches.getMachineTagMatches(),
            matches.getExactMatches(),
            matches.getFuzzyMatches(),
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
    collectionMatched.setSelfLink(URI.create(apiBaseUrl + "grscicoll/collection/" + dto.getKey()));
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
