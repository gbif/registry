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

import org.gbif.api.model.collections.request.InstitutionSearchRequest;

import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

@SuppressWarnings("NullableProblems")
public class InstitutionSearchRequestHandlerMethodArgumentResolver
    extends BaseGrSciCollSearchRequestHandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return InstitutionSearchRequest.class.equals(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {

    InstitutionSearchRequest searchRequest = InstitutionSearchRequest.builder().build();
    fillInstitutionSearchParameters(searchRequest, webRequest);

    return searchRequest;
  }

  protected void fillInstitutionSearchParameters(
      InstitutionSearchRequest searchRequest, NativeWebRequest webRequest) {
    fillSearchRequestParams(searchRequest, webRequest);

    Map<String, String[]> params = toCaseInsensitiveParams(webRequest);
    extractMultivalueParam(params, "type").ifPresent(searchRequest::setType);
    extractMultivalueParam(params, "institutionalGovernance")
        .ifPresent(searchRequest::setInstitutionalGovernance);
    extractMultivalueParam(params, "discipline").ifPresent(searchRequest::setDisciplines);
  }
}
