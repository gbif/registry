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
package org.gbif.registry.service.collections.lookup.matchers;

import org.gbif.api.model.collections.lookup.CollectionMatched;
import org.gbif.api.model.collections.lookup.InstitutionMatched;
import org.gbif.api.model.collections.lookup.LookupParams;
import org.gbif.api.model.collections.lookup.Match;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.persistence.mapper.collections.LookupMapper;
import org.gbif.registry.persistence.mapper.collections.dto.CollectionMatchedDto;
import org.gbif.registry.service.collections.lookup.Matches;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.*;

import static org.gbif.api.model.collections.lookup.Match.Reason.INST_COLL_MISMATCH;

@Component
public class CollectionMatcher extends BaseMatcher<CollectionMatchedDto, CollectionMatched> {

  private final CollectionMapper collectionMapper;

  @Autowired
  public CollectionMatcher(
      CollectionMapper collectionMapper, @Value("${api.root.url}") String apiBaseUrl) {
    super(apiBaseUrl);
    this.collectionMapper = collectionMapper;
  }

  public Matches<CollectionMatched> matchCollections(
      LookupParams params, Set<Match<InstitutionMatched>> institutionMatches) {
    Matches<CollectionMatched> matches = new Matches<>();

    List<CollectionMatchedDto> dbMatches =
        getDbMatches(
            params.getCollectionCode(),
            params.getInstitutionCode(),
            params.getCollectionId(),
            params.getDatasetKey());

    // the queries may return duplicates because a collection can match with several fields
    Map<UUID, CollectionMatchedDto> dtosMap = new HashMap<>();
    dbMatches.forEach(
        dto -> {
          if (dtosMap.containsKey(dto.getKey())) {
            updateMatches(dtosMap.get(dto.getKey()), dto);
          } else {
            dtosMap.put(dto.getKey(), dto);
          }
        });

    Set<Match<CollectionMatched>> exactMatches = new HashSet<>();
    Set<Match<CollectionMatched>> fuzzyMatches = new HashSet<>();
    Set<Match<CollectionMatched>> explicitMatches = new HashSet<>();
    dtosMap
        .values()
        .forEach(
            dto -> {
              Match<CollectionMatched> match =
                  createCollectionMatch(
                      exactMatches,
                      fuzzyMatches,
                      explicitMatches,
                      institutionMatches,
                      dto,
                      params.getCollectionCode());

              if (matchesCountry(dto, params.getCountry())) {
                match.addReason(Match.Reason.COUNTRY_MATCH);
              }
              if (!isMatchWithInstitutions(dto, institutionMatches)) {
                match.addReason(INST_COLL_MISMATCH);
              }
            });

    matches.setExactMatches(exactMatches);
    matches.setFuzzyMatches(fuzzyMatches);
    matches.setExplicitMatches(explicitMatches);

    return setAccepted(matches);
  }

  protected Match<CollectionMatched> createCollectionMatch(
      Set<Match<CollectionMatched>> exactMatches,
      Set<Match<CollectionMatched>> fuzzyMatches,
      Set<Match<CollectionMatched>> explicitMatches,
      Set<Match<InstitutionMatched>> institutionMatches,
      CollectionMatchedDto dto,
      String codeParam) {

    if (dto.getInstitutionKey() != null && institutionMatches.size() == 1) {
      Match<InstitutionMatched> institutionMatched = institutionMatches.iterator().next();
      if (institutionMatched.getEntityMatched().getKey().equals(dto.getInstitutionKey())
              && institutionMatched.getMatchType() == Match.MatchType.EXACT
          || institutionMatched.getMatchType() == Match.MatchType.EXPLICIT_MAPPING) {

        if (dto.isExplicitMapping()) {
          Match<CollectionMatched> match =
              Match.explicitMapping(toEntityMatched(dto), getMatchReasons(dto));
          match.addReason(Match.Reason.BELONGS_TO_INSTITUTION_MATCHED);
          explicitMatches.add(match);
          return match;
        } else {
          Match<CollectionMatched> match = Match.exact(toEntityMatched(dto), getMatchReasons(dto));
          match.addReason(Match.Reason.BELONGS_TO_INSTITUTION_MATCHED);
          exactMatches.add(match);
          return match;
        }
      }
    }

    return createMatch(exactMatches, fuzzyMatches, explicitMatches, dto, codeParam);
  }

  private Matches<CollectionMatched> setAccepted(Matches<CollectionMatched> matches) {
    matches.setAcceptedMatch(
        chooseAccepted(
            matches.getExplicitMatches(),
            matches.getExactMatches(),
            matches.getFuzzyMatches(),
            null,
            m -> !m.getReasons().contains(INST_COLL_MISMATCH),
            Match.Status.AMBIGUOUS_INSTITUTION_MISMATCH));
    return matches;
  }

  private boolean isMatchWithInstitutions(
      CollectionMatchedDto dto, Set<Match<InstitutionMatched>> institutionMatches) {
    return institutionMatches.isEmpty()
        || institutionMatches.stream()
            .anyMatch(m -> m.getEntityMatched().getKey().equals(dto.getInstitutionKey()));
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
    collectionMatched.setActive(dto.isActive());
    return collectionMatched;
  }
}
