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

import java.util.Arrays;
import org.gbif.api.model.collections.request.InstitutionSearchRequest;
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

    InstitutionSearchRequest searchRequest = new InstitutionSearchRequest();
    fillSearchRequestParams(searchRequest, webRequest);

    String[] types = webRequest.getParameterValues("type");
    if (types != null && types.length > 0) {
      searchRequest.setType(Arrays.asList(types));
    }

    String[] governances = webRequest.getParameterValues("institutionalGovernance");
    if (governances != null && governances.length > 0) {
      searchRequest.setInstitutionalGovernance(Arrays.asList(governances));
    }

    String[] disciplines = webRequest.getParameterValues("discipline");
    if (disciplines != null && disciplines.length > 0) {
      searchRequest.setDisciplines(Arrays.asList(disciplines));
    }

    return searchRequest;
  }
}
