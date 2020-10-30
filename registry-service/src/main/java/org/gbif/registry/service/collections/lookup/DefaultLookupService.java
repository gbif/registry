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

import org.gbif.api.model.collections.lookup.AlternativeMatches;
import org.gbif.api.model.collections.lookup.CollectionMatched;
import org.gbif.api.model.collections.lookup.InstitutionMatched;
import org.gbif.api.model.collections.lookup.LookupParams;
import org.gbif.api.model.collections.lookup.LookupResult;
import org.gbif.api.model.collections.lookup.Match;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.registry.service.collections.lookup.matchers.CollectionMatcher;
import org.gbif.registry.service.collections.lookup.matchers.InstitutionMatcher;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.gbif.api.model.collections.lookup.Match.MATCH_TYPE_COMPARATOR;

@Service
public class DefaultLookupService implements LookupService {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultLookupService.class);

  private final InstitutionMatcher institutionMatcher;
  private final CollectionMatcher collectionMatcher;
  private final DatasetService datasetService;

  @Autowired
  public DefaultLookupService(
      InstitutionMatcher institutionMatcher,
      CollectionMatcher collectionMatcher,
      DatasetService datasetService) {
    this.institutionMatcher = institutionMatcher;
    this.collectionMatcher = collectionMatcher;
    this.datasetService = datasetService;
  }

  @Override
  public LookupResult lookup(LookupParams params) {
    LookupResult result = new LookupResult();

    // get the machine tags of the dataset that will be used by the matchers
    List<MachineTag> datasetMachineTags = getDatasetMachineTags(params.getDatasetKey());

    Matches<InstitutionMatched> institutionMatches =
        institutionMatcher.matchInstitutions(params, datasetMachineTags);
    result.setInstitutionMatch(institutionMatches.getAcceptedMatch());

    Matches<CollectionMatched> collectionMatches =
        collectionMatcher.matchCollections(
            params, getInstitutionsMatched(institutionMatches), datasetMachineTags);
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

  private Set<UUID> getInstitutionsMatched(Matches<InstitutionMatched> institutionMatches) {
    return institutionMatches.getAllMatches().stream()
        .map(m -> m.getEntityMatched().getKey())
        .collect(Collectors.toSet());
  }

  private List<MachineTag> getDatasetMachineTags(UUID datasetKey) {
    if (datasetKey == null) {
      return Collections.emptyList();
    }

    try {
      // done in a try-catch since we may get non-existing dataset keys
      return datasetService.listMachineTags(datasetKey);
    } catch (Exception ex) {
      LOG.warn("Couldn't find dataset for key {}", datasetKey);
      return Collections.emptyList();
    }
  }
}
