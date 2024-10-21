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

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Optional;
import org.gbif.api.model.collections.request.CollectionDescriptorsSearchRequest;
import org.gbif.api.util.SearchTypeValidator;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.Rank;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

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

    extractMultivalueParam(webRequest, "usageKey", Integer::parseInt)
        .ifPresent(searchRequest::setUsageKey);
    extractMultivalueParam(webRequest, "usageName").ifPresent(searchRequest::setUsageName);
    extractMultivalueParam(webRequest, "usageRank", Rank::valueOf)
        .ifPresent(searchRequest::setUsageRank);
    extractMultivalueParam(webRequest, "taxonKey", Integer::parseInt)
        .ifPresent(searchRequest::setTaxonKey);
    extractMultivalueParam(webRequest, "identifiedBy").ifPresent(searchRequest::setIdentifiedBy);
    extractMultivalueParam(webRequest, "typeStatus").ifPresent(searchRequest::setTypeStatus);
    extractMultivalueParam(webRequest, "recordedBy").ifPresent(searchRequest::setRecordedBy);
    extractMultivalueParam(webRequest, "discipline").ifPresent(searchRequest::setDiscipline);
    extractMultivalueParam(webRequest, "objectClassification")
        .ifPresent(searchRequest::setObjectClassification);
    extractMultivalueParam(webRequest, "issue").ifPresent(searchRequest::setIssue);

    String[] descriptorCountries = webRequest.getParameterValues("descriptorCountry");
    if (descriptorCountries != null && descriptorCountries.length > 0) {
      searchRequest.setDescriptorCountry(new ArrayList<>());
      for (String countryParam : descriptorCountries) {
        Country country = Country.fromIsoCode(countryParam);
        if (country == null) {
          // if nothing found also try by enum name
          country = VocabularyUtils.lookupEnum(countryParam, Country.class);
        }

        if (country != null) {
          searchRequest.getDescriptorCountry().add(country);
        }
      }
    }

    String individualCountParam = webRequest.getParameter("individualCount");
    if (!Strings.isNullOrEmpty(individualCountParam)) {
      validateIntegerRange(individualCountParam, "individualCount");
      searchRequest.setIndividualCount(individualCountParam);
    }

    Optional.ofNullable(webRequest.getParameter("dateIdentified"))
        .ifPresent(v -> searchRequest.setDateIdentified(SearchTypeValidator.parseDateRange(v)));

    return searchRequest;
  }
}
