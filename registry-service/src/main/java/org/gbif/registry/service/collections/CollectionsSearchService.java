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
package org.gbif.registry.service.collections;

import static org.gbif.registry.service.collections.utils.ParamUtils.parseDateRangeParameters;
import static org.gbif.registry.service.collections.utils.ParamUtils.parseGbifRegion;
import static org.gbif.registry.service.collections.utils.ParamUtils.parseIntegerRangeParameters;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.gbif.api.model.collections.request.CollectionDescriptorsSearchRequest;
import org.gbif.api.model.collections.request.FacetedSearchRequest;
import org.gbif.api.model.collections.request.InstitutionFacetedSearchRequest;
import org.gbif.api.model.collections.request.SearchRequest;
import org.gbif.api.model.collections.search.BaseSearchResponse;
import org.gbif.api.model.collections.search.CollectionFacet;
import org.gbif.api.model.collections.search.CollectionSearchResponse;
import org.gbif.api.model.collections.search.CollectionsFullSearchResponse;
import org.gbif.api.model.collections.search.DescriptorMatch;
import org.gbif.api.model.collections.search.FacetedSearchResponse;
import org.gbif.api.model.collections.search.Highlight;
import org.gbif.api.model.collections.search.InstitutionSearchResponse;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.collections.CollectionFacetParameter;
import org.gbif.api.vocabulary.collections.CollectionsFacetParameter;
import org.gbif.api.vocabulary.collections.InstitutionFacetParameter;
import org.gbif.registry.domain.collections.TypeParam;
import org.gbif.registry.persistence.mapper.collections.CollectionsSearchMapper;
import org.gbif.registry.persistence.mapper.collections.dto.BaseSearchDto;
import org.gbif.registry.persistence.mapper.collections.dto.CollectionSearchDto;
import org.gbif.registry.persistence.mapper.collections.dto.FacetDto;
import org.gbif.registry.persistence.mapper.collections.dto.InstitutionSearchDto;
import org.gbif.registry.persistence.mapper.collections.dto.SearchDto;
import org.gbif.registry.persistence.mapper.collections.params.DescriptorsListParams;
import org.gbif.registry.persistence.mapper.collections.params.FullTextSearchParams;
import org.gbif.registry.persistence.mapper.collections.params.InstitutionListParams;
import org.gbif.registry.persistence.mapper.collections.params.ListParams;
import org.gbif.registry.service.collections.utils.SearchUtils;
import org.gbif.registry.service.collections.utils.Vocabularies;
import org.gbif.vocabulary.client.ConceptClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Service to lookup GRSciColl institutions and collections. */
@Service
public class CollectionsSearchService {

  private static final Pattern HIGHLIGHT_PATTERN = Pattern.compile(".*<b>.+</b>.*");

  private final CollectionsSearchMapper searchMapper;
  private final ConceptClient conceptClient;

  @Autowired
  public CollectionsSearchService(
      CollectionsSearchMapper searchMapper, ConceptClient conceptClient) {
    this.searchMapper = searchMapper;
    this.conceptClient = conceptClient;
  }

  public List<CollectionsFullSearchResponse> search(
      String query,
      boolean highlight,
      TypeParam type,
      List<Boolean> displayOnNHCPortal,
      List<Country> country,
      int limit) {
    List<SearchDto> dtos =
        searchMapper.search(
            FullTextSearchParams.builder()
                .query(query)
                .highlight(highlight)
                .type(type != null ? type.name() : null)
                .displayOnNHCPortal(displayOnNHCPortal)
                .country(country)
                .limit(limit)
                .build());

    // the query can return duplicates so we need an auxiliary map to filter duplicates
    Map<UUID, CollectionsFullSearchResponse> responsesMap = new HashMap<>();
    List<CollectionsFullSearchResponse> responses = new ArrayList<>();
    dtos.forEach(
        dto -> {
          if (responsesMap.containsKey(dto.getKey())) {
            CollectionsFullSearchResponse existing = responsesMap.get(dto.getKey());
            if (highlight) {
              addHighlights(existing, dto);
            }
            if (dto.getDescriptorKey() != null) {
              existing.getDescriptorMatches().add(addDescriptorMatch(dto));
            }
            return;
          }

          CollectionsFullSearchResponse response = new CollectionsFullSearchResponse();
          response.setType(dto.getType());
          response.setCode(dto.getCode());
          response.setKey(dto.getKey());
          response.setName(dto.getName());
          response.setDisplayOnNHCPortal(dto.isDisplayOnNHCPortal());
          response.setCountry(dto.getCountry());
          response.setMailingCountry(dto.getMailingCountry());

          if (dto.getType().equals("collection")) {
            response.setInstitutionKey(dto.getInstitutionKey());
            response.setInstitutionCode(dto.getInstitutionCode());
            response.setInstitutionName(dto.getInstitutionName());
          }

          if (dto.getDescriptorKey() != null) {
            response.getDescriptorMatches().add(addDescriptorMatch(dto));
          }

          if (highlight) {
            addHighlights(response, dto);
          }

          responses.add(response);
          responsesMap.put(dto.getKey(), response);
        });

    return responses;
  }

  public FacetedSearchResponse<InstitutionSearchResponse, InstitutionFacetParameter>
      searchInstitutions(InstitutionFacetedSearchRequest searchRequest) {

    Pageable page = searchRequest.getPage() == null ? new PagingRequest() : searchRequest.getPage();

    Vocabularies.addChildrenConcepts(searchRequest, conceptClient);

    InstitutionListParams.InstitutionListParamsBuilder listParamsBuilder =
        InstitutionListParams.builder()
            .types(searchRequest.getType())
            .institutionalGovernances(searchRequest.getInstitutionalGovernance())
            .disciplines(searchRequest.getDisciplines())
            .institutionKeys(searchRequest.getInstitutionKeys());
    buildCommonParams(listParamsBuilder, searchRequest);
    InstitutionListParams listParams = listParamsBuilder.build();

    List<InstitutionSearchDto> dtos = searchMapper.searchInstitutions(listParams);
    List<InstitutionSearchResponse> results =
        dtos.stream()
            .map(
                dto -> {
                  InstitutionSearchResponse response = new InstitutionSearchResponse();
                  createCommonResponse(dto, response);
                  response.setTypes(dto.getTypes());
                  response.setInstitutionalGovernances(dto.getInstitutionalGovernances());
                  response.setDisciplines(dto.getDisciplines());
                  response.setLatitude(dto.getLatitude());
                  response.setLongitude(dto.getLongitude());
                  response.setFoundingDate(dto.getFoundingDate());
                  response.setNumberSpecimens(dto.getNumberSpecimens());
                  response.setOccurrenceCount(dto.getOccurrenceCount());
                  response.setTypeSpecimenCount(dto.getTypeSpecimenCount());

                  if (Boolean.TRUE.equals(searchRequest.getHl())) {
                    addHighlights(response, dto);
                  }

                  return response;
                })
            .collect(Collectors.toList());

    List<CollectionFacet<InstitutionFacetParameter>> facets = new ArrayList<>();
    if (searchRequest.getFacets() != null && !searchRequest.getFacets().isEmpty()) {
      searchRequest
          .getFacets()
          .forEach(
              f -> {
                InstitutionListParams.InstitutionListParamsBuilder facetParamsBuilder =
                    searchRequest.isMultiSelectFacets()
                        ? InstitutionListParams.builder()
                        : listParamsBuilder;

                InstitutionListParams facetParams =
                    (InstitutionListParams)
                        facetParamsBuilder
                            .facet(f)
                            .facetMinCount(searchRequest.getFacetMinCount())
                            .facetPage(extractFacetPage(searchRequest, f))
                            .build();

                facets.add(
                    createFacet(
                        f,
                        searchMapper.institutionFacet(facetParams),
                        searchMapper.institutionFacetCardinality(facetParams)));
              });
    }

    return new FacetedSearchResponse<>(
        page, searchMapper.countInstitutions(listParams), results, facets);
  }

  public FacetedSearchResponse<CollectionSearchResponse, CollectionFacetParameter>
      searchCollections(CollectionDescriptorsSearchRequest searchRequest) {

    Pageable page = searchRequest.getPage() == null ? new PagingRequest() : searchRequest.getPage();

    Set<UUID> institutionKeys = new HashSet<>();
    if (searchRequest.getInstitution() != null) {
      institutionKeys.addAll(searchRequest.getInstitution());
    }
    if (searchRequest.getInstitutionKeys() != null) {
      institutionKeys.addAll(searchRequest.getInstitutionKeys());
    }

    Vocabularies.addChildrenConcepts(searchRequest, conceptClient);

    DescriptorsListParams.DescriptorsListParamsBuilder listParamsBuilder =
        DescriptorsListParams.builder()
            .contentTypes(searchRequest.getContentTypes())
            .preservationTypes(searchRequest.getPreservationTypes())
            .accessionStatus(searchRequest.getAccessionStatus())
            .personalCollection(searchRequest.getPersonalCollection())
            .institutionKeys(new ArrayList<>(institutionKeys))
            .usageName(searchRequest.getUsageName())
            .usageKey(searchRequest.getUsageKey())
            .usageRank(searchRequest.getUsageRank())
            .taxonKey(searchRequest.getTaxonKey())
            .descriptorCountry(searchRequest.getDescriptorCountry())
            .individualCount(parseIntegerRangeParameters(searchRequest.getIndividualCount()))
            .identifiedBy(searchRequest.getIdentifiedBy())
            .dateIdentified(parseDateRangeParameters(searchRequest.getDateIdentified()))
            .typeStatus(searchRequest.getTypeStatus())
            .recordedBy(searchRequest.getRecordedBy())
            .discipline(searchRequest.getDiscipline())
            .objectClassification(searchRequest.getObjectClassification())
            .issues(searchRequest.getIssue());
    buildCommonParams(listParamsBuilder, searchRequest);
    DescriptorsListParams listParams = listParamsBuilder.build();

    List<CollectionSearchDto> dtos = searchMapper.searchCollections(listParams);
    Map<UUID, CollectionSearchResponse> responsesMap = new HashMap<>();
    List<CollectionSearchResponse> results = new ArrayList<>();
    dtos.forEach(
        dto -> {
          if (responsesMap.containsKey(dto.getKey())) {
            CollectionSearchResponse existing = responsesMap.get(dto.getKey());
            if (Boolean.TRUE.equals(listParams.getHighlight())) {
              addHighlights(existing, dto);
            }
            if (isCollectionDescriptorResult(dto, listParams)) {
              existing.getDescriptorMatches().add(addDescriptorMatch(dto));
            }
            return;
          }

          CollectionSearchResponse response = new CollectionSearchResponse();
          responsesMap.put(dto.getKey(), response);
          results.add(response);

          createCommonResponse(dto, response);
          response.setContentTypes(dto.getContentTypes());
          response.setPersonalCollection(dto.isPersonalCollection());
          response.setPreservationTypes(dto.getPreservationTypes());
          response.setAccessionStatus(dto.getAccessionStatus());
          response.setInstitutionKey(dto.getInstitutionKey());
          response.setInstitutionName(dto.getInstitutionName());
          response.setInstitutionCode(dto.getInstitutionCode());
          response.setNumberSpecimens(dto.getNumberSpecimens());
          response.setTaxonomicCoverage(dto.getTaxonomicCoverage());
          response.setGeographicCoverage(dto.getGeographicCoverage());
          response.setDepartment(dto.getDepartment());
          response.setDivision(dto.getDivision());
          response.setDisplayOnNHCPortal(dto.isDisplayOnNHCPortal());
          response.setOccurrenceCount(dto.getOccurrenceCount());
          response.setTypeSpecimenCount(dto.getTypeSpecimenCount());

          if (isCollectionDescriptorResult(dto, listParams)) {
            response.getDescriptorMatches().add(addDescriptorMatch(dto));
          }

          if (Boolean.TRUE.equals(searchRequest.getHl())) {
            addHighlights(response, dto);
          }
        });

    List<CollectionFacet<CollectionFacetParameter>> facets = new ArrayList<>();
    if (searchRequest.getFacets() != null && !searchRequest.getFacets().isEmpty()) {
      searchRequest
          .getFacets()
          .forEach(
              f -> {
                DescriptorsListParams.DescriptorsListParamsBuilder facetParamsBuilder =
                    searchRequest.isMultiSelectFacets()
                        ? DescriptorsListParams.builder()
                        : listParamsBuilder;

                DescriptorsListParams facetParams =
                    (DescriptorsListParams)
                        facetParamsBuilder
                            .facet(f)
                            .facetMinCount(searchRequest.getFacetMinCount())
                            .facetPage(extractFacetPage(searchRequest, f))
                            .build();

                facets.add(
                    createFacet(
                        f,
                        searchMapper.collectionFacet(facetParams),
                        searchMapper.collectionFacetCardinality(facetParams)));
              });
    }

    return new FacetedSearchResponse<>(
        page, searchMapper.countCollections(listParams), results, facets);
  }

  private <F extends CollectionsFacetParameter> CollectionFacet<F> createFacet(
      F f, List<FacetDto> facetDtos, long cardinality) {
    List<CollectionFacet.Count> facetCounts =
        facetDtos.stream()
            .filter(
                dto ->
                    !Strings.isNullOrEmpty(dto.getFacet())
                        && !"null".equalsIgnoreCase(dto.getFacet()))
            .map(dto -> new CollectionFacet.Count(dto.getFacet(), dto.getCount()))
            .collect(Collectors.toList());

    CollectionFacet<F> collectionFacet = new CollectionFacet<>();
    collectionFacet.setField(f);
    collectionFacet.setCounts(facetCounts);
    collectionFacet.setCardinality(cardinality);
    return collectionFacet;
  }

  private static <F extends CollectionsFacetParameter> Pageable extractFacetPage(
      FacetedSearchRequest<F> searchRequest, F facetParameter) {
    if (searchRequest.getFacetPages() != null
        && searchRequest.getFacetPages().get(facetParameter) != null) {
      return searchRequest.getFacetPages().get(facetParameter);
    }

    int limit =
        searchRequest.getFacetLimit() != null
            ? searchRequest.getFacetLimit()
            : SearchUtils.DEFAULT_FACET_LIMIT;

    long offset = searchRequest.getFacetOffset() != null ? searchRequest.getFacetOffset() : 0;
    return new PagingRequest(offset, limit);
  }

  private static boolean isCollectionDescriptorResult(
      CollectionSearchDto dto, DescriptorsListParams params) {
    return dto.getDescriptorKey() != null
        && (dto.getQueryDescriptorRank() != null && dto.getQueryDescriptorRank() > 0
            || params.descriptorSearchWithoutQuery());
  }

  private static DescriptorMatch addDescriptorMatch(SearchDto dto) {
    DescriptorMatch descriptorMatch = new DescriptorMatch();
    descriptorMatch.setKey(dto.getDescriptorKey());
    descriptorMatch.setDescriptorGroupKey(dto.getDescriptorGroupKey());
    descriptorMatch.setUsageName(dto.getDescriptorUsageName());
    descriptorMatch.setUsageKey(dto.getDescriptorUsageKey());
    descriptorMatch.setUsageRank(dto.getDescriptorUsageRank());
    descriptorMatch.setCountry(dto.getDescriptorCountry());
    descriptorMatch.setIndividualCount(dto.getDescriptorIndividualCount());
    descriptorMatch.setIdentifiedBy(dto.getDescriptorIdentifiedBy());
    descriptorMatch.setDateIdentified(dto.getDescriptorDateIdentified());
    descriptorMatch.setTypeStatus(dto.getDescriptorTypeStatus());
    descriptorMatch.setRecordedBy(dto.getDescriptorRecordedBy());
    descriptorMatch.setDiscipline(dto.getDescriptorDiscipline());
    descriptorMatch.setObjectClassification(dto.getDescriptorObjectClassification());
    descriptorMatch.setIssues(dto.getDescriptorIssues());
    return descriptorMatch;
  }

  private void buildCommonParams(
      ListParams.ListParamsBuilder listParams, SearchRequest searchRequest) {
    String query =
        searchRequest.getQ() != null
            ? Strings.emptyToNull(CharMatcher.WHITESPACE.trimFrom(searchRequest.getQ()))
            : searchRequest.getQ();

    listParams
        .query(query)
        .code(searchRequest.getCode())
        .name(searchRequest.getName())
        .alternativeCode(searchRequest.getAlternativeCode())
        .machineTagNamespace(searchRequest.getMachineTagNamespace())
        .machineTagName(searchRequest.getMachineTagName())
        .machineTagValue(searchRequest.getMachineTagValue())
        .identifierType(searchRequest.getIdentifierType())
        .identifier(searchRequest.getIdentifier())
        .countries(searchRequest.getCountry())
        .regionCountries(parseGbifRegion(searchRequest))
        .city(searchRequest.getCity())
        .fuzzyName(searchRequest.getFuzzyName())
        .active(searchRequest.getActive())
        .masterSourceType(searchRequest.getMasterSourceType())
        .numberSpecimens(parseIntegerRangeParameters(searchRequest.getNumberSpecimens()))
        .displayOnNHCPortal(searchRequest.getDisplayOnNHCPortal())
        .replacedBy(searchRequest.getReplacedBy())
        .occurrenceCount(parseIntegerRangeParameters(searchRequest.getOccurrenceCount()))
        .typeSpecimenCount(parseIntegerRangeParameters(searchRequest.getTypeSpecimenCount()))
        .sourceId(searchRequest.getSourceId())
        .source(searchRequest.getSource())
        .sortBy(searchRequest.getSortBy())
        .sortOrder(searchRequest.getSortOrder())
        .highlight(searchRequest.getHl())
        .page(searchRequest.getPage());
  }

  private void createCommonResponse(BaseSearchDto dto, BaseSearchResponse response) {
    response.setKey(dto.getKey());
    response.setCode(dto.getCode());
    response.setName(dto.getName());
    response.setDescription(dto.getDescription());
    response.setActive(dto.isActive());
    response.setCountry(dto.getCountry());
    response.setMailingCountry(dto.getMailingCountry());
    response.setCity(dto.getCity());
    response.setMailingCity(dto.getMailingCity());
    response.setAlternativeCodes(dto.getAlternativeCodes());
    response.setDisplayOnNHCPortal(dto.isDisplayOnNHCPortal());
    response.setFeaturedImageUrl(dto.getFeaturedImageUrl());
    response.setFeaturedImageLicense(dto.getFeaturedImageLicense());
    response.setFeaturedImageAttribution(dto.getFeaturedImageAttribution());
  }

  private void addHighlights(BaseSearchResponse response, BaseSearchDto dto) {
    Set<Highlight> highlights = new HashSet<>();
    createHighlightMatch(dto.getCodeHighlight(), "code").ifPresent(highlights::add);
    createHighlightMatch(dto.getDescriptionHighlight(), "description").ifPresent(highlights::add);
    createHighlightMatch(dto.getAlternativeCodesHighlight(), "alternativeCode")
        .ifPresent(highlights::add);
    createHighlightMatch(dto.getAddressHighlight(), "address").ifPresent(highlights::add);
    createHighlightMatch(dto.getCityHighlight(), "city").ifPresent(highlights::add);
    createHighlightMatch(dto.getProvinceHighlight(), "province").ifPresent(highlights::add);
    createHighlightMatch(dto.getCountryHighlight(), "country").ifPresent(highlights::add);
    createHighlightMatch(dto.getMailAddressHighlight(), "mailingAddress")
        .ifPresent(highlights::add);
    createHighlightMatch(dto.getMailCityHighlight(), "mailingCity").ifPresent(highlights::add);
    createHighlightMatch(dto.getMailProvinceHighlight(), "mailingProvince")
        .ifPresent(highlights::add);
    createHighlightMatch(dto.getMailCountryHighlight(), "mailingCountry")
        .ifPresent(highlights::add);
    createHighlightMatch(dto.getDescriptorUsageNameHighlight(), "descriptor.usageName")
        .ifPresent(highlights::add);
    createHighlightMatch(dto.getDescriptorCountryHighlight(), "descriptor.country")
        .ifPresent(highlights::add);
    createHighlightMatch(dto.getDescriptorIdentifiedByHighlight(), "descriptor.identifiedBy")
        .ifPresent(highlights::add);
    createHighlightMatch(dto.getDescriptorTypeStatusHighlight(), "descriptor.typeStatus")
        .ifPresent(highlights::add);
    createHighlightMatch(dto.getDescriptorRecordedByHighlight(), "descriptor.recordedBy")
        .ifPresent(highlights::add);
    createHighlightMatch(dto.getDescriptorDisciplineHighlight(), "descriptor.discipline")
        .ifPresent(highlights::add);
    createHighlightMatch(
            dto.getDescriptorObjectClassificationHighlight(), "descriptor.objectClassification")
        .ifPresent(highlights::add);
    createHighlightMatch(dto.getDescriptorIssuesHighlight(), "descriptor.issues")
        .ifPresent(highlights::add);
    createHighlightMatch(dto.getDescriptorGroupTitleHighlight(), "descriptorGroup.title")
        .ifPresent(highlights::add);
    createHighlightMatch(
            dto.getDescriptorGroupDescriptionHighlight(), "descriptorGroup.description")
        .ifPresent(highlights::add);

    Optional<Highlight> nameMatch = createHighlightMatch(dto.getNameHighlight(), "name");
    if (nameMatch.isPresent()) {
      highlights.add(nameMatch.get());
    } else if (dto.isSimilarityMatch()) {
      Highlight highlight = new Highlight();
      highlight.setField("name");
      highlight.setSnippet(dto.getName());
      highlights.add(highlight);
    }

    if (!highlights.isEmpty()) {
      if (response.getHighlights() == null) {
        response.setHighlights(highlights);
      } else {
        response.getHighlights().addAll(highlights);
      }
    }
  }

  private static Optional<Highlight> createHighlightMatch(String highlightText, String fieldName) {
    if (!Strings.isNullOrEmpty(highlightText)
        && HIGHLIGHT_PATTERN.matcher(highlightText).matches()) {
      Highlight highlight = new Highlight();
      highlight.setField(fieldName);
      highlight.setSnippet(highlightText);
      return Optional.of(highlight);
    }

    return Optional.empty();
  }
}
