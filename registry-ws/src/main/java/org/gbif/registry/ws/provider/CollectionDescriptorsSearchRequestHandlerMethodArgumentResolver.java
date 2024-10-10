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
import java.util.Arrays;
import java.util.Optional;
import org.gbif.api.model.collections.request.CollectionDescriptorsSearchRequest;
import org.gbif.api.util.SearchTypeValidator;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TypeStatus;
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

    String[] usageKeys = webRequest.getParameterValues("usageKey");
    if (usageKeys != null && usageKeys.length > 0) {
      searchRequest.setUsageKey(new ArrayList<>());
      for (String keyParam : usageKeys) {
        try {
          searchRequest.getUsageKey().add(keyParam);
        } catch (Exception ex) {
          throw new IllegalArgumentException(
              "Invalid integer for usage key parameter: " + keyParam);
        }
      }
    }

    String[] usageNames = webRequest.getParameterValues("usageName");
    if (usageNames != null && usageNames.length > 0) {
      searchRequest.setUsageName(Arrays.asList(usageNames));
    }

    String[] usageRanks = webRequest.getParameterValues("usageRank");
    if (usageRanks != null && usageRanks.length > 0) {
      searchRequest.setUsageRank(new ArrayList<>());
      for (String param : usageRanks) {
        try {
          searchRequest.getUsageRank().add(param);
        } catch (Exception ex) {
          throw new IllegalArgumentException("Invalid rank for usage rank parameter: " + param);
        }
      }
    }

    String[] taxonKeys = webRequest.getParameterValues("taxonKey");
    if (taxonKeys != null && taxonKeys.length > 0) {
      searchRequest.setTaxonKey(new ArrayList<>());
      for (String keyParam : taxonKeys) {
        try {
          searchRequest.getTaxonKey().add(keyParam);
        } catch (Exception ex) {
          throw new IllegalArgumentException(
              "Invalid integer for taxon key parameter: " + keyParam);
        }
      }
    }

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

    String[] identifiedByParam = webRequest.getParameterValues("identifiedBy");
    if (identifiedByParam != null && identifiedByParam.length > 0) {
      searchRequest.setIdentifiedBy(Arrays.asList(identifiedByParam));
    }

    Optional.ofNullable(webRequest.getParameter("dateIdentified"))
        .ifPresent(v -> searchRequest.setDateIdentified(SearchTypeValidator.parseDateRange(v)));

    String[] typeStatusParam = webRequest.getParameterValues("typeStatus");
    if (typeStatusParam != null && typeStatusParam.length > 0) {
      searchRequest.setTypeStatus(new ArrayList<>());
      for (String param : typeStatusParam) {
        try {
          searchRequest.getTypeStatus().add(TypeStatus.valueOf(param).name());
        } catch (Exception ex) {
          throw new IllegalArgumentException("Invalid type status parameter: " + param);
        }
      }
    }

    String[] recordedByParam = webRequest.getParameterValues("recordedBy");
    if (recordedByParam != null && recordedByParam.length > 0) {
      searchRequest.setRecordedBy(Arrays.asList(recordedByParam));
    }

    String[] disciplineParam = webRequest.getParameterValues("discipline");
    if (disciplineParam != null && disciplineParam.length > 0) {
      searchRequest.setDiscipline(Arrays.asList(disciplineParam));
    }

    String[] objectClassificationParam = webRequest.getParameterValues("objectClassification");
    if (objectClassificationParam != null && objectClassificationParam.length > 0) {
      searchRequest.setObjectClassification(Arrays.asList(objectClassificationParam));
    }

    String[] issueParam = webRequest.getParameterValues("issue");
    if (issueParam != null && issueParam.length > 0) {
      searchRequest.setIssue(Arrays.asList(issueParam));
    }

    return searchRequest;
  }
}
