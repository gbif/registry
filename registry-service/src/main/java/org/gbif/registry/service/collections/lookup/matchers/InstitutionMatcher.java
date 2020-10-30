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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

import static org.gbif.api.model.collections.lookup.Match.Reason.COLLECTION_TO_INSTITUTION_TAG;
import static org.gbif.api.model.collections.lookup.Match.Reason.INSTITUTION_TAG;
import static org.gbif.api.model.collections.lookup.Match.Reason.POSSIBLY_ON_LOAN;

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
        .ifPresent(matches::setMachineTagMatches);

    if (isEnoughMatches(params, matches)) {
      return setAccepted(matches);
    }

    List<InstitutionMatchedDto> dbMatches =
        getDbMatches(params.getInstitutionCode(), params.getInstitutionId());

    Set<Match<InstitutionMatched>> exactMatches = new HashSet<>();
    Set<Match<InstitutionMatched>> fuzzyMatches = new HashSet<>();

    dbMatches.forEach(
        dto -> {
          Match<InstitutionMatched> match = createMatch(exactMatches, fuzzyMatches, dto);

          if (matchesCountry(dto, params.getCountry())) {
            match.addReason(Match.Reason.COUNTRY_MATCH);
          }
          if (!matchesOwnerInstitution(dto, params.getOwnerInstitutionCode())) {
            match.addReason(POSSIBLY_ON_LOAN);
          }
        });

    matches.setExactMatches(exactMatches);
    matches.setFuzzyMatches(fuzzyMatches);

    return setAccepted(matches);
  }

  private Matches<InstitutionMatched> setAccepted(Matches<InstitutionMatched> matches) {
    matches.setAcceptedMatch(
        chooseAccepted(
            matches.getMachineTagMatches(),
            matches.getExactMatches(),
            matches.getFuzzyMatches(),
            m -> !m.getReasons().contains(POSSIBLY_ON_LOAN),
            m -> !m.getReasons().contains(POSSIBLY_ON_LOAN),
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
        && dto.getIdentifiers().stream().anyMatch(i -> i.equals(ownerInstitutionCode))) {
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
    institutionMatched.setSelfLink(
        URI.create(apiBaseUrl + "grscicoll/institution/" + dto.getKey()));
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
