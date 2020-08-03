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
package org.gbif.registry.service.collections.lookup;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.lookup.AlternativeMatches;
import org.gbif.api.model.collections.lookup.LookupParams;
import org.gbif.api.model.collections.lookup.LookupResult;
import org.gbif.api.model.collections.lookup.Match;
import org.gbif.registry.service.collections.lookup.matchers.CollectionMatcher;
import org.gbif.registry.service.collections.lookup.matchers.InstitutionMatcher;

import java.util.Comparator;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.gbif.api.model.collections.lookup.Match.MATCH_TYPE_COMPARATOR;

@Service
public class DefaultLookupService implements LookupService {

  private final InstitutionMatcher institutionMatcher;
  private final CollectionMatcher collectionMatcher;

  @Autowired
  public DefaultLookupService(
      InstitutionMatcher institutionMatcher, CollectionMatcher collectionMatcher) {
    this.institutionMatcher = institutionMatcher;
    this.collectionMatcher = collectionMatcher;
  }

  @Override
  public LookupResult lookup(LookupParams params) {
    LookupResult result = new LookupResult();

    Matches<Institution> institutionMatches = institutionMatcher.matchInstitutions(params);
    result.setInstitutionMatch(institutionMatches.getAcceptedMatch());

    Matches<Collection> collectionMatches =
        collectionMatcher.matchCollections(params, getInstitutionsMatched(institutionMatches));
    result.setCollectionMatch(collectionMatches.getAcceptedMatch());

    if (params.isVerbose()) {
      AlternativeMatches alternativeMatches = new AlternativeMatches();
      // add alternative matches
      institutionMatches.getAllMatches().stream()
          .filter(m -> !m.equals(result.getInstitutionMatch()))
          .sorted(Comparator.comparing(Match::getMatchType, MATCH_TYPE_COMPARATOR))
          .forEach(m -> alternativeMatches.getInstitutionMatches().add(m));

      collectionMatches.getAllMatches().stream()
          .filter(m -> !m.equals(result.getCollectionMatch()))
          .sorted(Comparator.comparing(Match::getMatchType, MATCH_TYPE_COMPARATOR))
          .forEach(m -> alternativeMatches.getCollectionMatches().add(m));

      result.setAlternativeMatches(alternativeMatches);
    }

    return result;
  }

  private Set<UUID> getInstitutionsMatched(Matches<Institution> institutionMatches) {
    return institutionMatches.getAllMatches().stream()
        .map(m -> m.getEntityMatched().getKey())
        .collect(Collectors.toSet());
  }
}
