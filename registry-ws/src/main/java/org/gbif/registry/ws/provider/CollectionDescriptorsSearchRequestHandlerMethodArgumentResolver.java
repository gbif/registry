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

import org.gbif.api.model.collections.request.CollectionDescriptorsSearchRequest;
import org.gbif.api.util.IsoDateParsingUtils;
import org.gbif.api.vocabulary.collections.CollectionFacetParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.gbif.api.util.SearchTypeValidator.isDateRange;

@SuppressWarnings("NullableProblems")
public class CollectionDescriptorsSearchRequestHandlerMethodArgumentResolver
    extends CollectionSearchRequestHandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return CollectionDescriptorsSearchRequest.class.equals(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {

    CollectionDescriptorsSearchRequest searchRequest =
        CollectionDescriptorsSearchRequest.builder().build();
    fillCollectionSearchRequest(searchRequest, webRequest);

    Map<String, String[]> params = toCaseInsensitiveParams(webRequest);
    extractMultivalueParam(params, "usageKey").ifPresent(searchRequest::setUsageKey);
    extractMultivalueParam(params, "usageName").ifPresent(searchRequest::setUsageName);
    extractMultivalueParam(params, "usageRank").ifPresent(searchRequest::setUsageRank);
    extractMultivalueParam(params, "taxonKey").ifPresent(searchRequest::setTaxonKey);
    extractMultivalueParam(params, "identifiedBy").ifPresent(searchRequest::setIdentifiedBy);
    extractMultivalueParam(params, "typeStatus").ifPresent(searchRequest::setTypeStatus);
    extractMultivalueParam(params, "recordedBy").ifPresent(searchRequest::setRecordedBy);
    extractMultivalueParam(params, "discipline").ifPresent(searchRequest::setDiscipline);
    extractMultivalueParam(params, "objectClassification")
        .ifPresent(searchRequest::setObjectClassification);
    extractMultivalueParam(params, "biome")
      .ifPresent(searchRequest::setBiome);
    extractMultivalueParam(params, "issue").ifPresent(searchRequest::setIssue);
    extractMultivalueRangeParam(params, "individualCount")
        .ifPresent(searchRequest::setIndividualCount);
    extractMultivalueCountryParam(params, "descriptorCountry")
        .ifPresent(searchRequest::setDescriptorCountry);

    String[] dateIdentifiedParams = params.get("dateIdentified".toLowerCase());
    if (dateIdentifiedParams != null) {
      List<String> result = new ArrayList<>();
      for (String di : dateIdentifiedParams) {
        if (isDateRange(di)) {
          IsoDateParsingUtils.parseDateRange(di);
        } else {
          IsoDateParsingUtils.parseDate(di);
        }
        result.add(di);
      }
      searchRequest.setDateIdentified(result);
    }

    fillFacetParams(searchRequest, webRequest, facetParamParser());

    return searchRequest;
  }

  private Function<String, CollectionFacetParameter> facetParamParser() {
    return s -> {
      for (CollectionFacetParameter value : CollectionFacetParameter.values()) {
        if (normalizeFacet(value.name()).equals(normalizeFacet(s))) {
          return value;
        }
      }
      return null;
    };
  }
}
