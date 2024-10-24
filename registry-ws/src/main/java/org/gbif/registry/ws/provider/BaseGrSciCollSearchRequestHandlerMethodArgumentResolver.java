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
package org.gbif.registry.ws.provider;

import static org.gbif.registry.service.collections.utils.SearchUtils.DEFAULT_FACET_LIMIT;
import static org.gbif.ws.util.CommonWsUtils.*;
import static org.gbif.ws.util.WebserviceParameter.PARAM_FACET;
import static org.gbif.ws.util.WebserviceParameter.PARAM_FACET_LIMIT;
import static org.gbif.ws.util.WebserviceParameter.PARAM_FACET_MINCOUNT;
import static org.gbif.ws.util.WebserviceParameter.PARAM_FACET_MULTISELECT;
import static org.gbif.ws.util.WebserviceParameter.PARAM_FACET_OFFSET;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.gbif.api.model.collections.request.SearchRequest;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.*;
import org.gbif.api.vocabulary.collections.CollectionsFacetParameter;
import org.gbif.api.vocabulary.collections.CollectionsSortField;
import org.gbif.api.vocabulary.collections.MasterSourceType;
import org.gbif.api.vocabulary.collections.Source;
import org.gbif.registry.service.collections.utils.SearchUtils;
import org.gbif.ws.server.provider.PageableProvider;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

public abstract class BaseGrSciCollSearchRequestHandlerMethodArgumentResolver<
        F extends CollectionsFacetParameter>
    implements HandlerMethodArgumentResolver {

  public static final int MAX_PAGE_SIZE = 1000;

  protected <T extends SearchRequest<F>> void fillSearchRequestParams(
      T request, NativeWebRequest webRequest) {
    // page
    Pageable page = PageableProvider.getPagingRequest(webRequest, MAX_PAGE_SIZE);
    request.setLimit(page.getLimit());
    request.setOffset(page.getOffset());

    String hl = webRequest.getParameter("hl");
    if (!Strings.isNullOrEmpty(hl)) {
      try {
        request.setHl(Boolean.parseBoolean(hl));
      } catch (Exception e) {
        throw new IllegalArgumentException("Invalid boolean for hl: " + hl);
      }
    }

    extractMultivalueParam(webRequest, "alternativeCode").ifPresent(request::setAlternativeCode);
    extractMultivalueParam(webRequest, "code").ifPresent(request::setCode);
    extractMultivalueParam(webRequest, "name").ifPresent(request::setName);
    extractMultivalueParam(webRequest, "contact", UUID::fromString).ifPresent(request::setContact);
    extractMultivalueParam(webRequest, "identifier").ifPresent(request::setIdentifier);
    extractMultivalueParam(
            webRequest,
            "identifierType",
            v -> VocabularyUtils.lookup(v, IdentifierType.class).orElse(null))
        .ifPresent(request::setIdentifierType);

    extractMultivalueParam(webRequest, "machineTagName").ifPresent(request::setMachineTagName);
    extractMultivalueParam(webRequest, "machineTagNamespace")
        .ifPresent(request::setMachineTagNamespace);
    extractMultivalueParam(webRequest, "machineTagValue").ifPresent(request::setMachineTagValue);
    extractMultivalueParam(webRequest, "city").ifPresent(request::setCity);
    extractMultivalueParam(webRequest, "fuzzyName").ifPresent(request::setFuzzyName);

    request.setQ(webRequest.getParameter("q"));

    extractMultivalueParam(webRequest, "active", Boolean::parseBoolean)
        .ifPresent(request::setActive);

    String[] countryParams = webRequest.getParameterValues("country");
    if (countryParams != null && countryParams.length > 0) {
      request.setCountry(new ArrayList<>());
      for (int i = 0; i < countryParams.length; i++) {
        String countryParam = countryParams[i];
        Country country = Country.fromIsoCode(countryParam);
        if (country == null) {
          // if nothing found also try by enum name
          country = VocabularyUtils.lookupEnum(countryParam, Country.class);
        }

        if (country != null) {
          request.getCountry().add(country);
        }
      }
    }

    String[] gbifRegionParams = webRequest.getParameterValues("gbifRegion");
    if (gbifRegionParams != null && gbifRegionParams.length > 0) {
      request.setGbifRegion(new ArrayList<>());
      for (int i = 0; i < gbifRegionParams.length; i++) {
        String gbifRegionParam = gbifRegionParams[i];
        GbifRegion gbifRegion = GbifRegion.fromString(gbifRegionParam);
        if (gbifRegion != null) {
          request.getGbifRegion().add(gbifRegion);
        }
      }
    }

    extractMultivalueParam(webRequest, "masterSourceType", MasterSourceType::valueOf)
        .ifPresent(request::setMasterSourceType);

    String numberSpecimensParam = webRequest.getParameter("numberSpecimens");
    if (!Strings.isNullOrEmpty(numberSpecimensParam)) {
      validateIntegerRange(numberSpecimensParam, "numberSpecimens");
      request.setNumberSpecimens(numberSpecimensParam);
    }

    extractMultivalueParam(webRequest, "displayOnNHCPortal", Boolean::parseBoolean)
        .ifPresent(request::setDisplayOnNHCPortal);

    String sortByParam = webRequest.getParameter("sortBy");
    if (!Strings.isNullOrEmpty(sortByParam)) {
      try {
        request.setSortBy(CollectionsSortField.valueOf(sortByParam));
      } catch (Exception e) {
        throw new IllegalArgumentException("Invalid sort by parameter: " + sortByParam);
      }
    }

    String sortOrderParam = webRequest.getParameter("sortOrder");
    if (!Strings.isNullOrEmpty(sortOrderParam)) {
      try {
        request.setSortOrder(SortOrder.valueOf(sortOrderParam));
      } catch (Exception e) {
        throw new IllegalArgumentException("Invalid sort order parameter: " + sortOrderParam);
      }
    }

    String occurrenceCountParam = webRequest.getParameter("occurrenceCount");
    if (!Strings.isNullOrEmpty(occurrenceCountParam)) {
      validateIntegerRange(occurrenceCountParam, "occurrenceCount");
      request.setOccurrenceCount(occurrenceCountParam);
    }

    String typeSpecimenCountParam = webRequest.getParameter("typeSpecimenCount");
    if (!Strings.isNullOrEmpty(typeSpecimenCountParam)) {
      validateIntegerRange(typeSpecimenCountParam, "typeSpecimen");
      request.setTypeSpecimenCount(typeSpecimenCountParam);
    }

    extractMultivalueParam(webRequest, "institutionKey", UUID::fromString)
        .ifPresent(request::setInstitutionKeys);
    extractMultivalueParam(webRequest, "source", Source::valueOf).ifPresent(request::setSource);
    extractMultivalueParam(webRequest, "sourceId").ifPresent(request::setSourceId);

    fillFacetParams(request, webRequest);
  }

  protected Optional<List<String>> extractMultivalueParam(
      NativeWebRequest webRequest, String paramName) {
    String[] listParams = webRequest.getParameterValues(paramName);
    if (listParams != null && listParams.length > 0) {
      return Optional.of(Arrays.asList(listParams));
    }
    return Optional.empty();
  }

  protected <T> Optional<List<T>> extractMultivalueParam(
      NativeWebRequest webRequest, String paramName, Function<String, T> mapper) {
    String[] listParams = webRequest.getParameterValues(paramName);

    if (listParams != null) {
      List<T> result = new ArrayList<>();
      for (String param : listParams) {
        try {
          result.add(mapper.apply(param));
        } catch (Exception ex) {
          throw new IllegalArgumentException(
              "Invalid value " + param + " for parameter " + paramName);
        }
      }
      return Optional.of(result);
    }
    return Optional.empty();
  }

  protected <T extends SearchRequest<F>> void fillFacetParams(
      T searchRequest, NativeWebRequest webRequest) {
    final Map<String, String[]> params =
        webRequest.getParameterMap().entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey().toLowerCase(), Map.Entry::getValue));

    final String facetMultiSelectValue = getFirstIgnoreCase(params, PARAM_FACET_MULTISELECT);
    if (facetMultiSelectValue != null) {
      searchRequest.setMultiSelectFacets(Boolean.parseBoolean(facetMultiSelectValue));
    }

    final String facetMinCountValue = getFirstIgnoreCase(params, PARAM_FACET_MINCOUNT);
    if (facetMinCountValue != null) {
      searchRequest.setFacetMinCount(Integer.parseInt(facetMinCountValue));
    }

    final String facetLimit = getFirstIgnoreCase(params, PARAM_FACET_LIMIT);
    if (facetLimit != null) {
      searchRequest.setFacetLimit(Integer.parseInt(facetLimit));
    }

    final String facetOffset = getFirstIgnoreCase(params, PARAM_FACET_OFFSET);
    if (facetOffset != null) {
      searchRequest.setFacetOffset(Integer.parseInt(facetOffset));
    }

    final List<String> facetParams =
        params.get(PARAM_FACET) != null
            ? Arrays.asList(params.get(PARAM_FACET))
            : Collections.emptyList();
    if (!facetParams.isEmpty()) {
      Set<F> parsedFacets = new HashSet<>();
      for (String f : facetParams) {
        if (f.isEmpty()) {
          continue;
        }
        F facet = findFacetParam(f);
        if (facet != null) {
          parsedFacets.add(facet);
          String pFacetOffset = getFirstIgnoreCase(params, f + '.' + PARAM_FACET_OFFSET);
          String pFacetLimit = getFirstIgnoreCase(params, f + '.' + PARAM_FACET_LIMIT);
          if (pFacetLimit != null) {
            if (pFacetOffset != null) {
              searchRequest
                  .getFacetPages()
                  .put(
                      facet,
                      new PagingRequest(
                          Integer.parseInt(pFacetOffset), Integer.parseInt(pFacetLimit)));
            } else {
              searchRequest
                  .getFacetPages()
                  .put(facet, new PagingRequest(0, Integer.parseInt(pFacetLimit)));
            }
          } else if (pFacetOffset != null) {
            searchRequest
                .getFacetPages()
                .put(facet, new PagingRequest(Integer.parseInt(pFacetOffset), DEFAULT_FACET_LIMIT));
          }
        }
      }
      searchRequest.setFacets(parsedFacets);
    }
  }

  private static String getFirstIgnoreCase(Map<String, String[]> params, String param) {
    return getFirst(params, param.toLowerCase());
  }

  protected abstract F findFacetParam(String facetParam);

  protected static void validateIntegerRange(String param, String paramName) {
    boolean rangeMatch = SearchUtils.INTEGER_RANGE.matcher(param).find();
    boolean numberMatch = true;
    try {
      Integer.parseInt(param);
    } catch (NumberFormatException ex) {
      numberMatch = false;
    }
    if (!rangeMatch && !numberMatch) {
      throw new IllegalArgumentException(
          "Invalid " + paramName + " parameter.Only a number or a range is accepted: " + param);
    }
  }
}
