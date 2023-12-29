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

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.gbif.api.model.collections.lookup.InstitutionMatched;
import org.gbif.api.model.collections.lookup.LookupParams;
import org.gbif.api.model.collections.lookup.Match;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;
import org.gbif.registry.persistence.mapper.collections.LookupMapper;
import org.gbif.registry.persistence.mapper.collections.dto.InstitutionMatchedDto;
import org.gbif.registry.service.collections.lookup.Matches;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import static org.gbif.api.model.collections.lookup.Match.Reason.DIFFERENT_OWNER;

@Component
public class InstitutionMatcher extends BaseMatcher<InstitutionMatchedDto, InstitutionMatched> {

  private static final Pattern WHITESPACE_PATTERN = Pattern.compile("[\\h\\s+]");
  private final InstitutionMapper institutionMapper;

  @Autowired
  public InstitutionMatcher(
      InstitutionMapper institutionMapper, @Value("${api.root.url}") String apiBaseUrl) {
    super(apiBaseUrl);
    this.institutionMapper = institutionMapper;
  }

  public Matches<InstitutionMatched> matchInstitutions(LookupParams params) {
    Matches<InstitutionMatched> matches = new Matches<>();

    List<InstitutionMatchedDto> dbMatches =
        getDbMatches(
            params.getInstitutionCode(),
            null,
            params.getInstitutionId(),
            params.getDatasetKey(),
            params.getCatalogueNumber());

    // the queries may return duplicates because we retrieve the list of identifiers in the same
    // query. Also, if an institution matches with several fields it will be duplicated
    Map<UUID, InstitutionMatchedDto> dtosMap = new HashMap<>();
    Map<UUID, Set<String>> identifiersMap = new HashMap<>();
    dbMatches.forEach(
        dto -> {
          if (dtosMap.containsKey(dto.getKey())) {
            updateMatches(dtosMap.get(dto.getKey()), dto);
          } else {
            dtosMap.put(dto.getKey(), dto);
          }

          if (!Strings.isNullOrEmpty(dto.getIdentifier())) {
            if (identifiersMap.containsKey(dto.getKey())) {
              identifiersMap.get(dto.getKey()).add(dto.getIdentifier());
            } else {
              Set<String> ids = new HashSet<>();
              ids.add(dto.getIdentifier());
              identifiersMap.put(dto.getKey(), ids);
            }
          }
        });

    Set<Match<InstitutionMatched>> exactMatches = new HashSet<>();
    Set<Match<InstitutionMatched>> fuzzyMatches = new HashSet<>();
    Set<Match<InstitutionMatched>> explicitMatches = new HashSet<>();
    dtosMap
        .values()
        .forEach(
            dto -> {
              Match<InstitutionMatched> match =
                  createMatch(
                      exactMatches,
                      fuzzyMatches,
                      explicitMatches,
                      dto,
                      params.getInstitutionCode());

              if (matchesCountry(dto, params.getCountry())) {
                match.addReason(Match.Reason.COUNTRY_MATCH);
              }
              if (!matchesOwnerInstitution(
                  dto, identifiersMap.get(dto.getKey()), params.getOwnerInstitutionCode())) {
                match.addReason(DIFFERENT_OWNER);
              }
            });

    matches.setExactMatches(exactMatches);
    matches.setFuzzyMatches(fuzzyMatches);
    matches.setExplicitMatches(explicitMatches);

    return setAccepted(matches);
  }

  private Matches<InstitutionMatched> setAccepted(Matches<InstitutionMatched> matches) {
    matches.setAcceptedMatch(
        chooseAccepted(
            matches.getExplicitMatches(),
            matches.getExactMatches(),
            matches.getFuzzyMatches(),
            null,
            null,
            Match.Status.AMBIGUOUS_OWNER));

    return matches;
  }

  private boolean matchesOwnerInstitution(
      InstitutionMatchedDto dto, Set<String> identifiers, String ownerInstitutionCode) {
    if (Strings.isNullOrEmpty(ownerInstitutionCode)) {
      return true;
    }

    if (dto.getCode().equals(ownerInstitutionCode)) {
      return true;
    }

    if (dto.getAlternativeCodes().contains(ownerInstitutionCode)) {
      return true;
    }

    UnaryOperator<String> nameNormalizer =
        s -> StringUtils.stripAccents(WHITESPACE_PATTERN.matcher(s).replaceAll(""));

    if (nameNormalizer
        .apply(dto.getName())
        .equalsIgnoreCase(nameNormalizer.apply(ownerInstitutionCode))) {
      return true;
    }

    if (identifiers != null && identifiers.stream().anyMatch(i -> i.equals(ownerInstitutionCode))) {
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
    institutionMatched.setActive(dto.isActive());
    return institutionMatched;
  }
}
