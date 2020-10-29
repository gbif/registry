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

import org.gbif.api.model.collections.lookup.InstitutionMatched;
import org.gbif.api.model.collections.lookup.LookupParams;
import org.gbif.api.model.collections.lookup.Match;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;
import org.gbif.registry.persistence.mapper.collections.LookupMapper;
import org.gbif.registry.persistence.mapper.collections.dto.InstitutionMatchedDto;
import org.gbif.registry.service.collections.lookup.Matches;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

import static org.gbif.api.model.collections.lookup.Match.Reason.ALTERNATIVE_CODE_MATCH;
import static org.gbif.api.model.collections.lookup.Match.Reason.CODE_MATCH;
import static org.gbif.api.model.collections.lookup.Match.Reason.COLLECTION_TO_INSTITUTION_TAG;
import static org.gbif.api.model.collections.lookup.Match.Reason.IDENTIFIER_MATCH;
import static org.gbif.api.model.collections.lookup.Match.Reason.INSTITUTION_TAG;
import static org.gbif.api.model.collections.lookup.Match.Reason.KEY_MATCH;
import static org.gbif.api.model.collections.lookup.Match.Reason.NAME_MATCH;
import static org.gbif.api.model.collections.lookup.Match.Reason.PROBABLY_ON_LOAN;
import static org.gbif.api.model.collections.lookup.Match.exact;
import static org.gbif.api.model.collections.lookup.Match.fuzzy;

@Component
public class InstitutionMatcher extends BaseMatcher<InstitutionMatchedDto, InstitutionMatched> {

  private static final Pattern WHITESPACE_PATTERN = Pattern.compile("[\\h\\s+]");
  private final InstitutionMapper institutionMapper;

  @Autowired
  public InstitutionMatcher(
      InstitutionMapper institutionMapper,
      DatasetService datasetService,
      @Value("${api.root.url}") String apiBaseUrl) {
    super(datasetService, apiBaseUrl);
    this.institutionMapper = institutionMapper;
  }

  public Matches<InstitutionMatched> matchInstitutions(LookupParams params) {
    Matches<InstitutionMatched> matches = new Matches<>();

    matchWithMachineTags(params.getDatasetKey(), machineTagProcessor(params))
        .ifPresent(matches::setMachineTagMatchesMap);

    if (isEnoughMatches(params, matches)) {
      return setAccepted(matches);
    }

    findExactMatches(params).ifPresent(matches::setExactMatchesMap);

    if (isEnoughMatches(params, matches)) {
      return setAccepted(matches);
    }

    findFuzzyMatches(params, matches).ifPresent(matches::setFuzzyMatchesMap);

    return setAccepted(matches);
  }

  private Matches<InstitutionMatched> setAccepted(Matches<InstitutionMatched> matches) {
    matches.setAcceptedMatch(
        chooseAccepted(
            extractMatches(matches.getMachineTagMatchesMap()),
            extractMatches(matches.getExactMatchesMap()),
            extractMatches(matches.getFuzzyMatchesMap()),
            m -> !m.getReasons().contains(PROBABLY_ON_LOAN),
            m -> !m.getReasons().contains(PROBABLY_ON_LOAN),
            Match.Status.AMBIGUOUS_OWNER));

    return matches;
  }

  private BiConsumer<MachineTag, Map<UUID, Match<InstitutionMatched>>> machineTagProcessor(
      LookupParams params) {
    return (mt, matchesMap) -> {
      if (INSTITUTION_TAG_NAME.equals(mt.getName())) {
        addMachineTagMatch(mt, params.getInstitutionCode(), matchesMap, INSTITUTION_TAG);
      } else if (COLLECTION_TO_INSTITUTION_TAG_NAME.equals(mt.getName())) {
        addMachineTagMatch(
            mt, params.getCollectionCode(), matchesMap, COLLECTION_TO_INSTITUTION_TAG);
      }
    };
  }

  private Optional<Map<UUID, Match<InstitutionMatched>>> findExactMatches(LookupParams params) {
    if (!params.containsInstitutionParams()) {
      return Optional.empty();
    }

    Map<UUID, Match<InstitutionMatched>> exactMatches = new HashMap<>();
    // find by code and identifier.
    List<InstitutionMatchedDto> institutionsFoundByCodeAndId =
        findByCodeAndId(params.getInstitutionCode(), params.getInstitutionId());
    // if found we don't look for more matches since these are exact matches
    institutionsFoundByCodeAndId.forEach(
        dto -> {
          Match<InstitutionMatched> exactMatch =
              exact(toEntityMatched(dto), CODE_MATCH, IDENTIFIER_MATCH);
          exactMatches.put(dto.getKey(), exactMatch);
          if (matchesCountry(dto, params.getCountry())) {
            exactMatch.addReason(Match.Reason.COUNTRY_MATCH);
          }
          if (!matchesOwnerInstitution(dto, params.getOwnerInstitutionCode())) {
            exactMatch.addReason(PROBABLY_ON_LOAN);
          }
        });

    return exactMatches.isEmpty() ? Optional.empty() : Optional.of(exactMatches);
  }

  private Optional<Map<UUID, Match<InstitutionMatched>>> findFuzzyMatches(
      LookupParams params, Matches<InstitutionMatched> allMatches) {
    if (!params.containsInstitutionParams()) {
      return Optional.empty();
    }

    Map<UUID, Match<InstitutionMatched>> fuzzyMatches = new HashMap<>();
    BiConsumer<List<InstitutionMatchedDto>, Match.Reason> addFuzzyInstMatch =
        (l, r) ->
            l.stream()
                .filter(dto -> !allMatches.getExactMatchesMap().containsKey(dto.getKey()))
                .forEach(
                    dto -> {
                      Match<InstitutionMatched> fuzzyMatch =
                          fuzzyMatches
                              .computeIfAbsent(dto.getKey(), k -> fuzzy(toEntityMatched(dto)))
                              .addReason(r);
                      if (matchesCountry(dto, params.getCountry())) {
                        fuzzyMatch.addReason(Match.Reason.COUNTRY_MATCH);
                      }
                      if (!matchesOwnerInstitution(dto, params.getOwnerInstitutionCode())) {
                        fuzzyMatch.addReason(PROBABLY_ON_LOAN);
                      }
                    });

    // find matches by code
    CompletableFuture<List<InstitutionMatchedDto>> codeSearch =
        CompletableFuture.supplyAsync(() -> findByCode(params.getInstitutionCode()));

    // find matches by identifier
    CompletableFuture<List<InstitutionMatchedDto>> identifierSearch =
        CompletableFuture.supplyAsync(() -> findByIdentifier(params.getInstitutionId()));

    // find matches by alternative code
    CompletableFuture<List<InstitutionMatchedDto>> altCodeSearch =
        CompletableFuture.supplyAsync(() -> findByAlternativeCode(params.getInstitutionCode()));

    // find matches by using the code and the id as name
    CompletableFuture<List<InstitutionMatchedDto>> nameSearch =
        CompletableFuture.supplyAsync(
            () -> findByName(params.getInstitutionCode(), params.getInstitutionId()));

    // find matches by using the id as key
    CompletableFuture<List<InstitutionMatchedDto>> keySearch =
        CompletableFuture.supplyAsync(() -> findByKey(params.getInstitutionId()));

    CompletableFuture.allOf(codeSearch, identifierSearch, altCodeSearch, nameSearch, keySearch)
        .join();

    // add results to the matches
    addFuzzyInstMatch.accept(codeSearch.join(), CODE_MATCH);
    addFuzzyInstMatch.accept(identifierSearch.join(), IDENTIFIER_MATCH);
    addFuzzyInstMatch.accept(altCodeSearch.join(), ALTERNATIVE_CODE_MATCH);
    addFuzzyInstMatch.accept(nameSearch.join(), NAME_MATCH);
    addFuzzyInstMatch.accept(keySearch.join(), KEY_MATCH);

    return fuzzyMatches.isEmpty() ? Optional.empty() : Optional.of(fuzzyMatches);
  }

  private boolean matchesOwnerInstitution(InstitutionMatchedDto dto, String ownerInstitutionCode) {
    if (Strings.isNullOrEmpty(ownerInstitutionCode)) {
      return true;
    }

    if (dto.getCode().equals(ownerInstitutionCode.trim())) {
      return true;
    }

    UnaryOperator<String> nameNormalizer =
        s -> StringUtils.stripAccents(WHITESPACE_PATTERN.matcher(s).replaceAll(""));

    if (nameNormalizer
        .apply(dto.getName())
        .equalsIgnoreCase(nameNormalizer.apply(ownerInstitutionCode))) {
      return true;
    }

    if (dto.getIdentifiers() != null
        && dto.getIdentifiers().stream()
            .anyMatch(i -> i.getIdentifier().equals(ownerInstitutionCode))) {
      return true;
    }

    return false;
  }

  @Override
  LookupMapper<InstitutionMatchedDto> getLookupMapper() {
    return institutionMapper;
  }

  @Override
  InstitutionMatched toEntityMatched(InstitutionMatchedDto dto) {
    InstitutionMatched institutionMatched = new InstitutionMatched();
    institutionMatched.setKey(dto.getKey());
    institutionMatched.setCode(dto.getCode());
    institutionMatched.setName(dto.getName());
    institutionMatched.setSelf(URI.create(apiBaseUrl + "grscicoll/institution/" + dto.getKey()));
    return institutionMatched;
  }

  @Override
  Predicate<MachineTag> isMachineTagSupported() {
    return mt ->
        mt.getNamespace().equals(PROCESSING_NAMESPACE)
            && Arrays.asList(INSTITUTION_TAG_NAME, COLLECTION_TO_INSTITUTION_TAG_NAME)
                .contains(mt.getName());
  }
}
