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
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.collections.Discipline;
import org.gbif.api.vocabulary.collections.InstitutionGovernance;
import org.gbif.api.vocabulary.collections.InstitutionType;
import org.gbif.api.vocabulary.collections.Source;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.google.common.base.Strings;

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

    searchRequest.setType(
        VocabularyUtils.lookupEnum(webRequest.getParameter("type"), InstitutionType.class));

    searchRequest.setInstitutionalGovernance(
        VocabularyUtils.lookupEnum(
            webRequest.getParameter("institutionalGovernance"), InstitutionGovernance.class));

    String[] disciplines = webRequest.getParameterValues("discipline");
    if (disciplines != null && disciplines.length > 0) {
      searchRequest.setDisciplines(
          Arrays.stream(disciplines)
              .map(v -> VocabularyUtils.lookupEnum(v, Discipline.class))
              .collect(Collectors.toList()));
    }

    String sourceParam = webRequest.getParameter("source");
    if (!Strings.isNullOrEmpty(sourceParam))
      searchRequest.setSource(VocabularyUtils.lookupEnum(sourceParam, Source.class));

    String sourceIdParam = webRequest.getParameter("sourceId");
    if (!Strings.isNullOrEmpty(sourceIdParam)) searchRequest.setSourceId(sourceIdParam);

    return searchRequest;
  }
}
