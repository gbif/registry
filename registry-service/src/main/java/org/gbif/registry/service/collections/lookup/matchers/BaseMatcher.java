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

import org.gbif.api.model.collections.lookup.EntityMatched;
import org.gbif.api.model.collections.lookup.LookupParams;
import org.gbif.api.model.collections.lookup.Match;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.persistence.mapper.collections.LookupMapper;
import org.gbif.registry.persistence.mapper.collections.dto.EntityMatchedDto;
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
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import static org.gbif.api.model.collections.lookup.Match.Reason.ALTERNATIVE_CODE_MATCH;
import static org.gbif.api.model.collections.lookup.Match.Reason.CODE_MATCH;
import static org.gbif.api.model.collections.lookup.Match.Reason.COUNTRY_MATCH;
import static org.gbif.api.model.collections.lookup.Match.Reason.IDENTIFIER_MATCH;
import static org.gbif.api.model.collections.lookup.Match.Reason.KEY_MATCH;
import static org.gbif.api.model.collections.lookup.Match.Reason.NAME_MATCH;

/** Base matcher that contains common methods for the GrSciColl matchers. */
public abstract class BaseMatcher<T extends EntityMatchedDto, R extends EntityMatched> {

  public static final String PROCESSING_NAMESPACE = "processing.gbif.org";
  public static final String INSTITUTION_TAG_NAME = "institutionCode";
  public static final String COLLECTION_TAG_NAME = "collectionCode";
  public static final String COLLECTION_TO_INSTITUTION_TAG_NAME = "collectionToInstitutionCode";
  public static final String INSTITUTION_TO_COLLECTION_TAG_NAME = "institutionToCollectionCode";

  private static final Logger LOG = LoggerFactory.getLogger(BaseMatcher.class);

  private final DatasetService datasetService;
  protected final String apiBaseUrl;

  protected BaseMatcher(DatasetService datasetService, String apiBaseUrl) {
    this.datasetService = datasetService;
    this.apiBaseUrl = apiBaseUrl;
  }

  protected List<T> findByCodeAndId(String code, String id) {
    if (!Strings.isNullOrEmpty(code) && !Strings.isNullOrEmpty(id)) {
      return getLookupMapper().lookup(null, code, id, null, null);
    }
    return Collections.emptyList();
  }

  protected List<T> findByAlternativeCode(String alternativeCode) {
    if (!Strings.isNullOrEmpty(alternativeCode)) {
      return getLookupMapper().lookup(null, null, null, null, alternativeCode);
    }
    return Collections.emptyList();
  }

  protected List<T> findByCode(String code) {
    if (!Strings.isNullOrEmpty(code)) {
      return getLookupMapper().lookup(null, code, null, null, null);
    }
    return Collections.emptyList();
  }

  protected List<T> findByName(String... names) {
    List<String> namesList = parseParamsList(names);
    if (!namesList.isEmpty()) {
      return getLookupMapper().lookup(null, null, null, namesList, null);
    }
    return Collections.emptyList();
  }

  protected List<T> findByIdentifier(String id) {
    if (!Strings.isNullOrEmpty(id)) {
      return getLookupMapper().lookup(null, null, id, null, null);
    }
    return Collections.emptyList();
  }

  protected List<T> findByKey(String keyAsString) {
    if (!Strings.isNullOrEmpty(keyAsString)) {
      try {
        T entity = findByKey(UUID.fromString(keyAsString));
        if (entity != null) {
          return Collections.singletonList(entity);
        }
      } catch (Exception ex) {
        LOG.warn("Couldn't find entity by key: {}", keyAsString, ex);
      }
    }
    return Collections.emptyList();
  }

  private T findByKey(UUID key) {
    if (key != null) {
      List<T> dtos = getLookupMapper().lookup(key, null, null, null, null);
      if (dtos.size() == 1) {
        return dtos.get(0);
      }
    }
    return null;
  }

  protected Optional<Map<UUID, Match<R>>> matchWithMachineTags(
      UUID datasetKey, BiConsumer<MachineTag, Map<UUID, Match<R>>> tagProcessor) {
    if (datasetKey == null) {
      return Optional.empty();
    }

    List<MachineTag> machineTags = getDatasetMachineTags(datasetKey);
    if (machineTags.isEmpty()) {
      return Optional.empty();
    }

    Map<UUID, Match<R>> matchesMap = new HashMap<>();
    machineTags.stream()
        .filter(isMachineTagSupported())
        .forEach(mt -> tagProcessor.accept(mt, matchesMap));

    return matchesMap.isEmpty() ? Optional.empty() : Optional.of(matchesMap);
  }

  protected void addMachineTagMatch(
      MachineTag mt, String param, Map<UUID, Match<R>> matchesMap, Match.Reason reason) {
    if (Strings.isNullOrEmpty(param)) {
      return;
    }

    String[] val = mt.getValue().split(":");
    if (val.length > 1) {
      UUID mtKey = UUID.fromString(val[0]);
      String mtCode = mt.getValue().substring(val[0].length() + 1);

      if (!Strings.isNullOrEmpty(param) && mtCode.equals(param)) {
        T entity = findByKey(mtKey);
        if (entity != null) {
          matchesMap
              .computeIfAbsent(mtKey, k -> Match.machineTag(toEntityMatched(entity)))
              .addReason(reason);
        }
      }
    }
  }

  private List<MachineTag> getDatasetMachineTags(UUID datasetKey) {
    try {
      // done in a try-catch since we may get non-existing dataset keys
      return datasetService.listMachineTags(datasetKey);
    } catch (Exception ex) {
      LOG.warn("Couldn't find dataset for key {}", datasetKey);
      return Collections.emptyList();
    }
  }

  protected Match<R> chooseAccepted(
      Set<Match<R>> machineTagMatches,
      Set<Match<R>> exactMatches,
      Set<Match<R>> fuzzyMatches,
      Predicate<Match<R>> exactExcludeFilter,
      Predicate<Match<R>> fuzzyExcludeFilter,
      Match.Status filterStatus) {
    if (!machineTagMatches.isEmpty()) {
      if (machineTagMatches.size() == 1) {
        Match<R> acceptedMatch = machineTagMatches.iterator().next();
        acceptedMatch.setStatus(Match.Status.ACCEPTED);
        return acceptedMatch;
      }
      return Match.none(Match.Status.AMBIGUOUS_MACHINE_TAGS);
    } else if (!exactMatches.isEmpty()) {
      Set<Match<R>> filteredMatched = filterMatches(exactMatches, exactExcludeFilter);
      if (filteredMatched.isEmpty()) {
        return Match.none(filterStatus);
      }

      // if there is no unique match we try with the country if provided
      Optional<Match<R>> uniqueMatch =
          findUniqueMatch(filteredMatched, Collections.singletonList(isCountryMatch()));
      if (uniqueMatch.isPresent()) {
        Match<R> acceptedMatch = uniqueMatch.get();
        acceptedMatch.setStatus(Match.Status.ACCEPTED);
        return acceptedMatch;
      }
      return Match.none(Match.Status.AMBIGUOUS);
    } else if (!fuzzyMatches.isEmpty()) {
      Set<Match<R>> filteredMatched = filterMatches(fuzzyMatches, fuzzyExcludeFilter);
      if (filteredMatched.isEmpty()) {
        return Match.none(filterStatus);
      }

      // if there is no unique match we try with other fields or the country if provided
      Optional<Match<R>> uniqueMatch =
          findUniqueMatch(
              filteredMatched, Arrays.asList(isMultipleFieldsMatch(), isCountryMatch()));
      if (uniqueMatch.isPresent()) {
        Match<R> acceptedMatch = uniqueMatch.get();
        acceptedMatch.setStatus(Match.Status.DOUBTFUL);
        return acceptedMatch;
      }
      return Match.none(Match.Status.AMBIGUOUS);
    }
    return Match.none();
  }

  private Optional<Match<R>> findUniqueMatch(
      Set<Match<R>> matches, List<Predicate<Match<R>>> alternativeMatchFinders) {
    if (matches.size() == 1) {
      // just one match, we return it
      return Optional.of(matches.iterator().next());
    } else {
      // we try with the alternative methods
      for (Predicate<Match<R>> p : alternativeMatchFinders) {
        List<Match<R>> found = matches.stream().filter(p).collect(Collectors.toList());
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
  private Predicate<Match<R>> isMultipleFieldsMatch() {
    return match ->
        (match.getReasons().contains(CODE_MATCH)
                || match.getReasons().contains(IDENTIFIER_MATCH)
                || match.getReasons().contains(KEY_MATCH))
            && (match.getReasons().contains(NAME_MATCH)
                || match.getReasons().contains(ALTERNATIVE_CODE_MATCH)
                || match.getReasons().contains(KEY_MATCH));
  }

  private Predicate<Match<R>> isCountryMatch() {
    return match -> (match.getReasons().contains(COUNTRY_MATCH));
  }

  protected boolean matchesCountry(T dto, Country country) {
    if (country == null) {
      return false;
    }

    return dto.getAddressCountry() == country || dto.getMailingAddressCountry() == country;
  }

  protected boolean isEnoughMatches(LookupParams params, Matches<R> matches) {
    return !matches.isEmpty() && !params.isVerbose();
  }

  protected Set<Match<R>> extractMatches(Map<UUID, Match<R>> map) {
    return !map.isEmpty() ? new HashSet<>(map.values()) : Collections.emptySet();
  }

  protected Set<Match<R>> filterMatches(
      Set<Match<R>> matches, Predicate<Match<R>> filterCondition) {
    return filterCondition != null
        ? matches.stream().filter(filterCondition).collect(Collectors.toSet())
        : matches;
  }

  private static List<String> parseParamsList(String... params) {
    if (params != null) {
      return Stream.of(params).filter(c -> !Strings.isNullOrEmpty(c)).collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  abstract LookupMapper<T> getLookupMapper();

  abstract R toEntityMatched(T dto);

  abstract Predicate<MachineTag> isMachineTagSupported();
}
