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
package org.gbif.registry.search.dataset.service.collections;

import org.gbif.api.model.collections.search.CollectionsSearchResponse;
import org.gbif.registry.persistence.mapper.collections.CollectionsSearchMapper;
import org.gbif.registry.persistence.mapper.collections.dto.SearchDto;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.elasticsearch.common.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Service to lookup GRSciColl institutions and collections. */
@Service
public class CollectionsSearchService {

  private static final Pattern HIGHLIGHT_PATTERN = Pattern.compile(".*<b>.+</b>.*");

  private final CollectionsSearchMapper searchMapper;

  public enum TypeParam {
    INSTITUTION,
    COLLECTION;
  }

  @Autowired
  public CollectionsSearchService(CollectionsSearchMapper searchMapper) {
    this.searchMapper = searchMapper;
  }

  public List<CollectionsSearchResponse> search(
      String query, boolean highlight, TypeParam type, Boolean displayOnNHCPortal, int limit) {
    List<SearchDto> dtos =
        searchMapper.search(
            query, highlight, type != null ? type.name() : null, displayOnNHCPortal);

    // the query can return duplicates so we need an auxiliary map to filter duplicates
    Map<UUID, CollectionsSearchResponse> responsesMap = new HashMap<>();
    List<CollectionsSearchResponse> responses = new ArrayList<>();
    dtos.forEach(
        dto -> {
          if (responsesMap.containsKey(dto.getKey())) {
            if (highlight) {
              CollectionsSearchResponse existing = responsesMap.get(dto.getKey());
              addMatches(existing, dto);
            }
            return;
          }

          CollectionsSearchResponse response = new CollectionsSearchResponse();
          response.setType(dto.getType());
          response.setCode(dto.getCode());
          response.setKey(dto.getKey());
          response.setName(dto.getName());

          if (dto.getType().equals("collection")) {
            response.setInstitutionKey(dto.getInstitutionKey());
            response.setInstitutionCode(dto.getInstitutionCode());
            response.setInstitutionName(dto.getInstitutionName());
          }

          if (highlight) {
            addMatches(response, dto);
          }

          responses.add(response);
          responsesMap.put(dto.getKey(), response);
        });

    return responses.stream().limit(limit).collect(Collectors.toList());
  }

  private void addMatches(CollectionsSearchResponse response, SearchDto dto) {
    Set<CollectionsSearchResponse.Match> matches = new HashSet<>();
    createHighlightMatch(dto.getCodeHighlight(), "code").ifPresent(matches::add);
    createHighlightMatch(dto.getDescriptionHighlight(), "description").ifPresent(matches::add);
    createHighlightMatch(dto.getAlternativeCodesHighlight(), "alternativeCode")
        .ifPresent(matches::add);
    createHighlightMatch(dto.getAddressHighlight(), "address").ifPresent(matches::add);
    createHighlightMatch(dto.getCityHighlight(), "city").ifPresent(matches::add);
    createHighlightMatch(dto.getProvinceHighlight(), "province").ifPresent(matches::add);
    createHighlightMatch(dto.getCountryHighlight(), "country").ifPresent(matches::add);
    createHighlightMatch(dto.getMailAddressHighlight(), "mailingAddress").ifPresent(matches::add);
    createHighlightMatch(dto.getMailCityHighlight(), "mailingCity").ifPresent(matches::add);
    createHighlightMatch(dto.getMailProvinceHighlight(), "mailingProvince").ifPresent(matches::add);
    createHighlightMatch(dto.getMailCountryHighlight(), "mailingCountry").ifPresent(matches::add);

    Optional<CollectionsSearchResponse.Match> nameMatch =
        createHighlightMatch(dto.getNameHighlight(), "name");
    if (nameMatch.isPresent()) {
      matches.add(nameMatch.get());
    } else if (dto.isSimilarityMatch()) {
      CollectionsSearchResponse.Match match = new CollectionsSearchResponse.Match();
      match.setField("name");
      match.setSnippet(dto.getName());
      matches.add(match);
    }

    if (!matches.isEmpty()) {
      if (response.getMatches() == null) {
        response.setMatches(matches);
      } else {
        response.getMatches().addAll(matches);
      }
    }
  }

  private static Optional<CollectionsSearchResponse.Match> createHighlightMatch(
      String highlight, String fieldName) {
    if (!Strings.isNullOrEmpty(highlight) && HIGHLIGHT_PATTERN.matcher(highlight).matches()) {
      CollectionsSearchResponse.Match match = new CollectionsSearchResponse.Match();
      match.setField(fieldName);
      match.setSnippet(highlight);
      return Optional.of(match);
    }

    return Optional.empty();
  }
}
