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

import org.gbif.api.util.SearchTypeValidator;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.domain.ws.DatasetRequestSearchParams;
import org.gbif.registry.domain.ws.RequestSearchParams;

import java.util.Optional;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@SuppressWarnings("NullableProblems")
public class DatasetRequestSearchParamsHandlerMethodArgumentResolver
    implements HandlerMethodArgumentResolver {
  private static final String WILDCARD_SEARCH = "*";

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return DatasetRequestSearchParams.class.equals(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {
    DatasetRequestSearchParams params = new DatasetRequestSearchParams();
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
    params.setType(
        VocabularyUtils.lookupEnum(
            webRequest.getParameter(DatasetRequestSearchParams.TYPE_PARAM), DatasetType.class));
    Optional.ofNullable(webRequest.getParameter(RequestSearchParams.MODIFIED_PARAM))
        .ifPresent(v -> params.setModified(SearchTypeValidator.parseDateRange(v)));

    return params;
  }
}
