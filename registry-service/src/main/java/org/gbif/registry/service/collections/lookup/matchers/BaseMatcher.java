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
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.persistence.mapper.collections.LookupMapper;
import org.gbif.registry.persistence.mapper.collections.dto.BaseEntityMatchedDto;
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

import com.google.common.base.Strings;

import static org.gbif.api.model.collections.lookup.Match.Reason.ALTERNATIVE_CODE_MATCH;
import static org.gbif.api.model.collections.lookup.Match.Reason.CODE_MATCH;
import static org.gbif.api.model.collections.lookup.Match.Reason.COUNTRY_MATCH;
import static org.gbif.api.model.collections.lookup.Match.Reason.IDENTIFIER_MATCH;
import static org.gbif.api.model.collections.lookup.Match.Reason.KEY_MATCH;
import static org.gbif.api.model.collections.lookup.Match.Reason.NAME_MATCH;
import static org.gbif.api.model.collections.lookup.Match.exact;
import static org.gbif.api.model.collections.lookup.Match.fuzzy;

/** Base matcher that contains common methods for the GrSciColl matchers. */
public abstract class BaseMatcher<T extends EntityMatchedDto, R extends EntityMatched> {

  public static final String PROCESSING_NAMESPACE = "processing.gbif.org";
  public static final String INSTITUTION_TAG_NAME = "institutionCode";
  public static final String COLLECTION_TAG_NAME = "collectionCode";
  public static final String COLLECTION_TO_INSTITUTION_TAG_NAME = "collectionToInstitutionCode";
  public static final String INSTITUTION_TO_COLLECTION_TAG_NAME = "institutionToCollectionCode";

  protected final String apiBaseUrl;

  protected BaseMatcher(String apiBaseUrl) {
    this.apiBaseUrl = apiBaseUrl;
  }

  protected Set<Match<R>> matchWithMachineTags(
      List<MachineTag> machineTags, BiConsumer<MachineTag, Map<UUID, Match<R>>> tagProcessor) {
    if (machineTags == null || machineTags.isEmpty()) {
      return Collections.emptySet();
    }

    Map<UUID, Match<R>> matchesMap = new HashMap<>();
    machineTags.stream()
        .filter(isMachineTagSupported())
        .forEach(mt -> tagProcessor.accept(mt, matchesMap));

    return matchesMap.isEmpty() ? Collections.emptySet() : new HashSet<>(matchesMap.values());
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
        List<T> entity = getLookupMapper().lookup(null, null, mtKey);
        if (entity != null && entity.size() == 1) {
          matchesMap
              .computeIfAbsent(mtKey, k -> Match.explicitMapping(toEntityMatched(entity.get(0))))
              .addReason(reason);
        }
      }
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
      return Match.none(Match.Status.AMBIGUOUS_EXPLICIT_MAPPINGS);
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
    return country != null
        && (dto.getAddressCountry() == country || dto.getMailingAddressCountry() == country);
  }

  protected boolean isEnoughMatches(LookupParams params, Matches<R> matches) {
    return !matches.isEmpty() && !params.isVerbose();
  }

  protected Set<Match<R>> filterMatches(
      Set<Match<R>> matches, Predicate<Match<R>> filterCondition) {
    return filterCondition != null
        ? matches.stream().filter(filterCondition).collect(Collectors.toSet())
        : matches;
  }

  private static UUID parseUUID(String value) {
    try {
      return value != null ? UUID.fromString(value) : null;
    } catch (Exception e) {
      return null;
    }
  }

  private static String cleanString(String value) {
    return !Strings.isNullOrEmpty(value) ? value.trim() : null;
  }

  protected List<T> getDbMatches(String codeParam, String identifierParam) {
    String code = cleanString(codeParam);
    String identifier = cleanString(identifierParam);

    if (code == null && identifier == null) {
      return Collections.emptyList();
    }

    UUID key = parseUUID(identifier);

    return getLookupMapper().lookup(code, identifier, key);
  }

  protected Match<R> createMatch(Set<Match<R>> exactMatches, Set<Match<R>> fuzzyMatches, T dto) {
    Match<R> match = null;
    if (dto.isCodeMatch() && dto.isIdentifierMatch()) {
      match = exact(toEntityMatched(dto), CODE_MATCH, IDENTIFIER_MATCH);
      exactMatches.add(match);
    } else {
      match = fuzzy(toEntityMatched(dto));
      fuzzyMatches.add(match);
      match.setReasons(getMatchReasons(dto));
    }
    return match;
  }

  protected Set<Match.Reason> getMatchReasons(T dto) {
    Set<Match.Reason> reasons = new HashSet<>();
    if (dto.isKeyMatch()) {
      reasons.add(KEY_MATCH);
    }
    if (dto.isCodeMatch()) {
      reasons.add(CODE_MATCH);
    }
    if (dto.isIdentifierMatch()) {
      reasons.add(IDENTIFIER_MATCH);
    }
    if (dto.isAlternativeCodeMatch()) {
      reasons.add(ALTERNATIVE_CODE_MATCH);
    }
    if (dto.isNameMatchWithCode() || dto.isNameMatchWithIdentifier()) {
      reasons.add(NAME_MATCH);
    }

    return reasons;
  }

  protected static <T extends BaseEntityMatchedDto> void updateMatches(T existing, T newDto) {
    if (newDto.isKeyMatch()) {
      existing.setKeyMatch(true);
    }
    if (newDto.isCodeMatch()) {
      existing.setCodeMatch(true);
    }
    if (newDto.isIdentifierMatch()) {
      existing.setIdentifierMatch(true);
    }
    if (newDto.isAlternativeCodeMatch()) {
      existing.setAlternativeCodeMatch(true);
    }
    if (newDto.isNameMatchWithCode()) {
      existing.setNameMatchWithCode(true);
    }
    if (newDto.isNameMatchWithIdentifier()) {
      existing.setNameMatchWithIdentifier(true);
    }
  }

  abstract LookupMapper<T> getLookupMapper();

  abstract R toEntityMatched(T dto);

  abstract Predicate<MachineTag> isMachineTagSupported();
}
