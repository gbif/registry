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
package org.gbif.registry.search.dataset.service.collections;

import org.gbif.registry.persistence.mapper.collections.CollectionsSearchMapper;
import org.gbif.registry.persistence.mapper.collections.dto.SearchDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.elasticsearch.common.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// TODO: tests
@Service
public class CollectionsSearchService {

  private static final String HIGHLIGHT_DELIMITER = "<b>";

  private CollectionsSearchMapper searchMapper;

  @Autowired
  public CollectionsSearchService(CollectionsSearchMapper searchMapper) {
    this.searchMapper = searchMapper;
  }

  public List<SearchResponse> search(String query, boolean highlight) {
    List<SearchDto> dtos = searchMapper.search(query, highlight);

    List<SearchResponse> responses = new ArrayList<>();
    dtos.forEach(
        dto -> {
          SearchResponse response = new SearchResponse();
          response.setType(dto.getType());
          response.setCode(dto.getCode());
          response.setKey(dto.getKey());
          response.setName(dto.getName());

          if (dto.getType().equals("collection")) {
            response.setInstitutionKey(dto.getInstitutionKey());
            response.setInstitutionCode(dto.getInstitutionCode());
            response.setInstitutionName(dto.getInstitutionName());
          }

          List<SearchResponse.Match> matches = new ArrayList<>();
          createHighlightMatch(dto.getCodeHighlight(), "code").ifPresent(matches::add);
          createHighlightMatch(dto.getNameHighlight(), "name").ifPresent(matches::add);
          createHighlightMatch(dto.getDescriptionHighlight(), "description")
              .ifPresent(matches::add);
          createHighlightMatch(dto.getAlternativeCodesHighlight(), "alternativeCode")
              .ifPresent(matches::add);
          createHighlightMatch(dto.getAddressHighlight(), "address").ifPresent(matches::add);
          createHighlightMatch(dto.getCityHighlight(), "city").ifPresent(matches::add);
          createHighlightMatch(dto.getProvinceHighlight(), "province").ifPresent(matches::add);
          createHighlightMatch(dto.getCountryHighlight(), "country").ifPresent(matches::add);
          createHighlightMatch(dto.getMailAddressHighlight(), "mailingAddress")
              .ifPresent(matches::add);
          createHighlightMatch(dto.getMailCityHighlight(), "mailingCity").ifPresent(matches::add);
          createHighlightMatch(dto.getMailProvinceHighlight(), "mailingProvince")
              .ifPresent(matches::add);
          createHighlightMatch(dto.getMailCountryHighlight(), "mailingCountry")
              .ifPresent(matches::add);
          if (!matches.isEmpty()) {
            response.setMatches(matches);
          }

          responses.add(response);
        });

    return responses;
  }

  private static Optional<SearchResponse.Match> createHighlightMatch(
      String highlight, String fieldName) {
    if (!Strings.isNullOrEmpty(highlight) && highlight.contains(HIGHLIGHT_DELIMITER)) {
      SearchResponse.Match match = new SearchResponse.Match();
      match.setField(fieldName);
      match.setSnippet(highlight);
      return Optional.of(match);
    }

    return Optional.empty();
  }
}
