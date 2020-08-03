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

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Contactable;
import org.gbif.api.model.collections.lookup.LookupParams;
import org.gbif.api.model.collections.lookup.Match;
import org.gbif.api.model.registry.Commentable;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.persistence.mapper.collections.BaseMapper;
import org.gbif.registry.persistence.mapper.collections.LookupMapper;
import org.gbif.registry.service.collections.lookup.Matches;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import static org.gbif.api.model.collections.lookup.Match.Reason.ALTERNATIVE_CODE_MATCH;
import static org.gbif.api.model.collections.lookup.Match.Reason.CODE_MATCH;
import static org.gbif.api.model.collections.lookup.Match.Reason.COUNTRY_MATCH;
import static org.gbif.api.model.collections.lookup.Match.Reason.IDENTIFIER_MATCH;
import static org.gbif.api.model.collections.lookup.Match.Reason.NAME_MATCH;

/** Base matcher that contains common methods for the GrSciColl matchers. */
public abstract class BaseMatcher<
    T extends
        CollectionEntity & Taggable & Identifiable & MachineTaggable & Commentable & Contactable> {

  public static final String PROCESSING_NAMESPACE = "processing.gbif.org";
  public static final String INSTITUTION_TAG_NAME = "institutionCode";
  public static final String COLLECTION_TAG_NAME = "collectionCode";
  public static final String COLLECTION_TO_INSTITUTION_TAG_NAME = "collectionToInstitutionCode";
  public static final String INSTITUTION_TO_COLLECTION_TAG_NAME = "institutionToCollectionCode";

  private static final Logger LOG = LoggerFactory.getLogger(BaseMatcher.class);

  private final DatasetService datasetService;

  protected BaseMatcher(DatasetService datasetService) {
    this.datasetService = datasetService;
  }

  protected List<T> findByCodeAndId(String code, String id) {
    if (!Strings.isNullOrEmpty(code) && !Strings.isNullOrEmpty(id)) {
      return getLookupMapper().lookup(code, id, null, null);
    }
    return Collections.emptyList();
  }

  protected List<T> findByAlternativeCode(String alternativeCode) {
    if (!Strings.isNullOrEmpty(alternativeCode)) {
      return getLookupMapper().lookup(null, null, null, alternativeCode);
    }
    return Collections.emptyList();
  }

  protected List<T> findByCode(String code) {
    if (!Strings.isNullOrEmpty(code)) {
      return getLookupMapper().lookup(code, null, null, null);
    }
    return Collections.emptyList();
  }

  protected List<T> findByName(String code) {
    if (!Strings.isNullOrEmpty(code)) {
      return getLookupMapper().lookup(null, null, code, null);
    }
    return Collections.emptyList();
  }

  protected List<T> findByIdentifier(String id) {
    if (!Strings.isNullOrEmpty(id)) {
      return getLookupMapper().lookup(null, id, null, null);
    }
    return Collections.emptyList();
  }

  protected Optional<Map<UUID, Match<T>>> matchWithMachineTags(
      UUID datasetKey, BiConsumer<MachineTag, Map<UUID, Match<T>>> tagProcessor) {
    if (datasetKey == null) {
      return Optional.empty();
    }

    Dataset dataset = getDataset(datasetKey);
    if (dataset == null) {
      return Optional.empty();
    }

    Map<UUID, Match<T>> matchesMap = new HashMap<>();
    dataset.getMachineTags().stream()
        .filter(isMachineTagSupported())
        .forEach(mt -> tagProcessor.accept(mt, matchesMap));

    return matchesMap.isEmpty() ? Optional.empty() : Optional.of(matchesMap);
  }

  protected void addMachineTagMatch(
      MachineTag mt, String param, Map<UUID, Match<T>> matchesMap, Match.Reason reason) {
    if (Strings.isNullOrEmpty(param)) {
      return;
    }

    String[] val = mt.getValue().split(":");
    if (val.length > 1) {
      UUID mtKey = UUID.fromString(val[0]);
      String mtCode = mt.getValue().substring(val[0].length() + 1);

      if (!Strings.isNullOrEmpty(param) && mtCode.equals(param)) {
        T entity = getBaseMapper().get(mtKey);
        if (entity != null) {
          matchesMap.computeIfAbsent(mtKey, k -> Match.machineTag(entity)).addReason(reason);
        }
      }
    }
  }

  private Dataset getDataset(UUID datasetKey) {
    try {
      // done in a try-catch since we may get non-existing dataset keys
      return datasetService.get(datasetKey);
    } catch (Exception ex) {
      LOG.warn("Couldn't find dataset for key {}", datasetKey);
      return null;
    }
  }

  protected Match<T> chooseAccepted(
      Set<Match<T>> machineTagMatches, Set<Match<T>> exactMatches, Set<Match<T>> fuzzyMatches) {
    if (!machineTagMatches.isEmpty()) {
      if (machineTagMatches.size() == 1) {
        Match<T> acceptedMatch = machineTagMatches.iterator().next();
        acceptedMatch.setStatus(Match.Status.ACCEPTED);
        return acceptedMatch;
      }
      return Match.none(Match.Status.AMBIGUOUS_MACHINE_TAGS);
    } else if (!exactMatches.isEmpty()) {
      // if there is no unique match we try with the country if provided
      Optional<Match<T>> uniqueMatch =
          findUniqueMatch(exactMatches, Collections.singletonList(isCountryMatch()));
      if (uniqueMatch.isPresent()) {
        Match<T> acceptedMatch = uniqueMatch.get();
        acceptedMatch.setStatus(Match.Status.ACCEPTED);
        return acceptedMatch;
      }
      return Match.none(Match.Status.AMBIGUOUS);
    } else if (!fuzzyMatches.isEmpty()) {
      // if there is no unique match we try with other fields or the country if provided
      Optional<Match<T>> uniqueMatch =
          findUniqueMatch(fuzzyMatches, Arrays.asList(isMultipleFieldsMatch(), isCountryMatch()));
      if (uniqueMatch.isPresent()) {
        Match<T> acceptedMatch = uniqueMatch.get();
        acceptedMatch.setStatus(Match.Status.DOUBTFUL);
        return acceptedMatch;
      }
      return Match.none(Match.Status.AMBIGUOUS);
    }
    return Match.none();
  }

  private Optional<Match<T>> findUniqueMatch(
      Set<Match<T>> matches, List<Predicate<Match<T>>> alternativeMatchFinders) {
    if (matches.size() == 1) {
      // just one match, we return it
      return Optional.of(matches.iterator().next());
    } else {
      // we try with the alternative methods
      for (Predicate<Match<T>> p : alternativeMatchFinders) {
        List<Match<T>> found = matches.stream().filter(p).collect(Collectors.toList());
        if (found.size() == 1) {
          return Optional.of(found.get(0));
        }
      }
    }
    return Optional.empty();
  }

  /**
   * At least the code or ID has to match and some of the other fields (name or alternative code).
   */
  private Predicate<Match<T>> isMultipleFieldsMatch() {
    return match ->
        (match.getReasons().contains(CODE_MATCH) || match.getReasons().contains(IDENTIFIER_MATCH))
            && (match.getReasons().contains(NAME_MATCH)
                || match.getReasons().contains(ALTERNATIVE_CODE_MATCH));
  }

  private Predicate<Match<T>> isCountryMatch() {
    return match -> (match.getReasons().contains(COUNTRY_MATCH));
  }

  protected boolean matchesCountry(T entity, Country country) {
    if (country == null) {
      return false;
    }

    Predicate<Address> countryMatch =
        address ->
            Optional.ofNullable(address)
                .map(Address::getCountry)
                .map(c -> c == country)
                .orElse(false);

    return countryMatch.test(entity.getAddress()) || countryMatch.test(entity.getMailingAddress());
  }

  protected void checkCountryMatch(LookupParams params, Match<T> match) {
    if (matchesCountry(match.getEntityMatched(), params.getCountry())) {
      match.addReason(COUNTRY_MATCH);
    }
  }

  protected boolean isEnoughMatches(LookupParams params, Matches<T> matches) {
    return !matches.isEmpty() && !params.isVerbose();
  }

  protected Set<Match<T>> extractMatches(Map<UUID, Match<T>> map) {
    return !map.isEmpty() ? new HashSet<>(map.values()) : Collections.emptySet();
  }

  protected Set<Match<T>> extractAndFilter(
      Map<UUID, Match<T>> matches, Predicate<Match<T>> filterCondition) {
    return extractMatches(matches).stream().filter(filterCondition).collect(Collectors.toSet());
  }

  abstract LookupMapper<T> getLookupMapper();

  abstract BaseMapper<T> getBaseMapper();

  abstract Predicate<MachineTag> isMachineTagSupported();
}
