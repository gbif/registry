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

import org.gbif.api.model.collections.request.InstitutionFacetedSearchRequest;
import org.gbif.api.vocabulary.collections.InstitutionFacetParameter;

import java.util.function.Function;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

@SuppressWarnings("NullableProblems")
public class InstitutionFacetedSearchRequestHandlerMethodArgumentResolver
    extends InstitutionSearchRequestHandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return InstitutionFacetedSearchRequest.class.equals(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {

    InstitutionFacetedSearchRequest searchRequest =
        InstitutionFacetedSearchRequest.builder().build();
    fillInstitutionSearchParameters(searchRequest, webRequest);
    fillFacetParams(searchRequest, webRequest, facetParamParser());

    return searchRequest;
  }

  private Function<String, InstitutionFacetParameter> facetParamParser() {
    return s -> {
      for (InstitutionFacetParameter value : InstitutionFacetParameter.values()) {
        if (normalizeFacet(value.name()).equals(normalizeFacet(s))) {
          return value;
        }
      }
      return null;
    };
  }
}
