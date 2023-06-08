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

import com.google.common.base.Strings;

import org.gbif.api.model.registry.search.DatasetRequestSearchParams;
import org.gbif.api.model.registry.search.InstallationRequestSearchParams;
import org.gbif.api.model.registry.search.OrganizationRequestSearchParams;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.Country;

import org.gbif.api.vocabulary.DatasetType;

import org.gbif.api.vocabulary.InstallationType;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("NullableProblems")
public class InstallationRequestSearchParamsHandlerMethodArgumentResolver
    extends BaseRequestSearchParamsHandlerMethodArgumentResolver
    implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return InstallationRequestSearchParams.class.equals(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {
    InstallationRequestSearchParams params = new InstallationRequestSearchParams();
    fillCommonParams(params, webRequest);

    params.setType(
        VocabularyUtils.lookupEnum(
            webRequest.getParameter(InstallationRequestSearchParams.INSTALLATION_TYPE_PARAM),
            InstallationType.class));

    String organizationKeyParam =
        webRequest.getParameter(InstallationRequestSearchParams.ORGANIZATION_KEY_PARAM);
    if (!Strings.isNullOrEmpty(organizationKeyParam)) {
      try {
        params.setOrganizationKey(UUID.fromString(organizationKeyParam));
      } catch (Exception e) {
        throw new IllegalArgumentException(
            "Invalid UUID for organization key: " + organizationKeyParam);
      }
    }

    String endorsedByNodeKeyParam =
        webRequest.getParameter(InstallationRequestSearchParams.ENDORSED_BY_PARAM);
    if (!Strings.isNullOrEmpty(endorsedByNodeKeyParam)) {
      try {
        params.setEndorsedByNodeKey(UUID.fromString(endorsedByNodeKeyParam));
      } catch (Exception e) {
        throw new IllegalArgumentException("Invalid UUID for node key: " + endorsedByNodeKeyParam);
      }
    }

    return params;
  }
}
