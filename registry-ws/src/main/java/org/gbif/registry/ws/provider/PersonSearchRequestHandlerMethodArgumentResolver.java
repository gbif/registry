/*
 * Copyright 2020-2021 Global Biodiversity Information Facility (GBIF)
 *
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

import org.gbif.api.model.collections.request.PersonSearchRequest;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.ws.server.provider.PageableProvider;

import java.util.UUID;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.google.common.base.Strings;

@SuppressWarnings("NullableProblems")
public class PersonSearchRequestHandlerMethodArgumentResolver
    implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return PersonSearchRequest.class.equals(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {

    PersonSearchRequest searchRequest = new PersonSearchRequest();

    // page
    Pageable page = PageableProvider.getPagingRequest(webRequest, Integer.MAX_VALUE);
    searchRequest.setLimit(page.getLimit());
    searchRequest.setOffset(page.getOffset());

    searchRequest.setIdentifier(webRequest.getParameter("identifier"));
    searchRequest.setIdentifierType(
        VocabularyUtils.lookupEnum(
            webRequest.getParameter("identifierType"), IdentifierType.class));
    searchRequest.setMachineTagName(webRequest.getParameter("machineTagName"));
    searchRequest.setMachineTagNamespace(webRequest.getParameter("machineTagNamespace"));
    searchRequest.setMachineTagValue(webRequest.getParameter("machineTagValue"));
    searchRequest.setQ(webRequest.getParameter("q"));

    String primaryInstitution = webRequest.getParameter("primaryInstitution");
    if (!Strings.isNullOrEmpty(primaryInstitution)) {
      try {
        searchRequest.setPrimaryInstitution(UUID.fromString(primaryInstitution));
      } catch (Exception e) {
        throw new IllegalArgumentException(
            "Invalid UUID for primary institution: " + primaryInstitution);
      }
    }

    String primaryCollection = webRequest.getParameter("primaryCollection");
    if (!Strings.isNullOrEmpty(primaryCollection)) {
      try {
        searchRequest.setPrimaryCollection(UUID.fromString(primaryCollection));
      } catch (Exception e) {
        throw new IllegalArgumentException(
            "Invalid UUID for primary collection: " + primaryCollection);
      }
    }

    return searchRequest;
  }
}
