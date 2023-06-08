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
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.registry.search.DatasetRequestSearchParams;
import org.gbif.api.model.registry.search.OrganizationRequestSearchParams;
import org.gbif.api.model.registry.search.RequestSearchParams;
import org.gbif.api.util.SearchTypeValidator;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.ws.server.provider.PageableProvider;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("NullableProblems")
public class OrganizationRequestSearchParamsHandlerMethodArgumentResolver
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
    params.setIdentifier(webRequest.getParameter(RequestSearchParams.IDENTIFIER_PARAM));
    params.setIdentifierType(
        VocabularyUtils.lookupEnum(
            webRequest.getParameter(RequestSearchParams.IDENTIFIER_TYPE_PARAM),
            IdentifierType.class));
    params.setMachineTagName(webRequest.getParameter(RequestSearchParams.MACHINE_TAG_NAME_PARAM));
    params.setMachineTagNamespace(
        webRequest.getParameter(RequestSearchParams.MACHINE_TAG_NAMESPACE_PARAM));
    params.setMachineTagValue(webRequest.getParameter(RequestSearchParams.MACHINE_TAG_VALUE_PARAM));
    params.setQ(webRequest.getParameter(RequestSearchParams.Q_PARAM));
    Optional.ofNullable(webRequest.getParameter(OrganizationRequestSearchParams.IS_ENDORSED_PARAM))
        .ifPresent(v -> params.setIsEndorsed(Boolean.parseBoolean(v)));
    Optional.ofNullable(webRequest.getParameter(RequestSearchParams.MODIFIED_PARAM))
        .ifPresent(v -> params.setModified(SearchTypeValidator.parseDateRange(v)));

    String networkKeyParam =
        webRequest.getParameter(OrganizationRequestSearchParams.NETWORK_KEY_PARAM);
    if (!Strings.isNullOrEmpty(networkKeyParam)) {
      try {
        params.setNetworkKey(UUID.fromString(networkKeyParam));
      } catch (Exception e) {
        throw new IllegalArgumentException("Invalid UUID for network key: " + networkKeyParam);
      }
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

    // page
    Pageable page = PageableProvider.getPagingRequest(webRequest, Integer.MAX_VALUE);
    params.setLimit(page.getLimit());
    params.setOffset(page.getOffset());

    return params;
  }
}
