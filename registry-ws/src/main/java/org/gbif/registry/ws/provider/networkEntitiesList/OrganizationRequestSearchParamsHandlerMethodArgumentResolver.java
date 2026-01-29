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
package org.gbif.registry.ws.provider.networkEntitiesList;

import org.apache.commons.lang3.StringUtils;
import org.gbif.api.model.registry.search.OrganizationRequestSearchParams;
import org.gbif.api.util.Range;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.Country;

import java.util.Optional;
import java.util.UUID;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.google.common.base.Strings;

@SuppressWarnings("NullableProblems")
public class OrganizationRequestSearchParamsHandlerMethodArgumentResolver
    extends BaseRequestSearchParamsHandlerMethodArgumentResolver
    implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return OrganizationRequestSearchParams.class.equals(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {
    OrganizationRequestSearchParams params = new OrganizationRequestSearchParams();
    fillCommonParams(params, webRequest);

    Optional.ofNullable(webRequest.getParameter(OrganizationRequestSearchParams.IS_ENDORSED_PARAM))
        .ifPresent(v -> params.setIsEndorsed(Boolean.parseBoolean(v)));

    String networkKeyParam =
        webRequest.getParameter(OrganizationRequestSearchParams.NETWORK_KEY_PARAM);
    if (!Strings.isNullOrEmpty(networkKeyParam)) {
      try {
        params.setNetworkKey(UUID.fromString(networkKeyParam));
      } catch (Exception e) {
        throw new IllegalArgumentException("Invalid UUID for network key: " + networkKeyParam);
      }
    }

    // canModify
    String user = webRequest.getParameter("canModify");
    if (StringUtils.isNotEmpty(user)) {
      params.setCanModify(user);
    }

    // country
    String countryParam = webRequest.getParameter("country");
    if (!Strings.isNullOrEmpty(countryParam)) {
      Country country = Country.fromIsoCode(countryParam);
      if (country == null) {
        // if nothing found also try by enum name
        country = VocabularyUtils.lookupEnum(countryParam, Country.class);
      }
      params.setCountry(country);
    }

    // numPublishedDatasets range parameter
    String numPublishedDatasetsParam = webRequest.getParameter("numPublishedDatasets");
    if (!Strings.isNullOrEmpty(numPublishedDatasetsParam)) {
      try {
        Range<Integer> range = parseNumPublishedDatasetsRange(numPublishedDatasetsParam.trim());
        params.setNumPublishedDatasets(range);
      } catch (Exception e) {
        throw new IllegalArgumentException("Invalid format for numPublishedDatasets parameter: " + numPublishedDatasetsParam + ". " +
            "Examples: '5' (exactly 5), '1,*' (at least 1), '*,10' (at most 10), '5,15' (between 5 and 15)");
      }
    }

    String contactUserIdParam = webRequest.getParameter("contactUserId");
    if (!Strings.isNullOrEmpty(contactUserIdParam)) {
      params.setContactUserId(contactUserIdParam);
    }

    String contactEmailParam = webRequest.getParameter("contactEmail");
    if (!Strings.isNullOrEmpty(contactEmailParam)) {
      params.setContactEmail(contactEmailParam);
    }

    return params;
  }

  private Range<Integer> parseNumPublishedDatasetsRange(String value) {
    if ("*".equals(value)) {
      // Match all - no range restriction  
      return Range.closed(null, null);
    }
    
    if (!value.contains(",")) {
      // Exact match: "5"
      int exactValue = Integer.parseInt(value);
      return Range.closed(exactValue, exactValue);
    }
    
    // Range with comma: "1,*", "*,10", "5,15"
    String[] parts = value.split(",", 2);
    String minPart = parts[0].trim();
    String maxPart = parts[1].trim();
    
    if (("*".equals(minPart) || minPart.isEmpty()) && ("*".equals(maxPart) || maxPart.isEmpty())) {
      return Range.closed(null, null);
    }
    
    if ("*".equals(minPart) || minPart.isEmpty()) {
      // Max only: "*,10" or ",10"
      int maxValue = Integer.parseInt(maxPart);
      return Range.closed(null, maxValue);
    }
    
    if ("*".equals(maxPart) || maxPart.isEmpty()) {
      // Min only: "1,*" or "1,"
      int minValue = Integer.parseInt(minPart);
      return Range.closed(minValue, null);
    }
    
    // Both min and max: "5,15"
    int minValue = Integer.parseInt(minPart);
    int maxValue = Integer.parseInt(maxPart);
    return Range.closed(minValue, maxValue);
  }
}
