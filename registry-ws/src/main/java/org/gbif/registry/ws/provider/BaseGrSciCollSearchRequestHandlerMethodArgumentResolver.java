/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
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

import org.gbif.api.model.collections.request.SearchRequest;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.ws.server.provider.PageableProvider;

import java.util.UUID;

import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import com.google.common.base.Strings;

public abstract class BaseGrSciCollSearchRequestHandlerMethodArgumentResolver
    implements HandlerMethodArgumentResolver {

  protected <T extends SearchRequest> void fillSearchRequestParams(
      T request, NativeWebRequest webRequest) {
    // page
    Pageable page = PageableProvider.getPagingRequest(webRequest, Integer.MAX_VALUE);
    request.setLimit(page.getLimit());
    request.setOffset(page.getOffset());

    request.setAlternativeCode(webRequest.getParameter("alternativeCode"));
    request.setCode(webRequest.getParameter("code"));
    request.setName(webRequest.getParameter("name"));

    String contact = webRequest.getParameter("contact");
    if (!Strings.isNullOrEmpty(contact)) {
      try {
        request.setContact(UUID.fromString(contact));
      } catch (Exception e) {
        throw new IllegalArgumentException("Invalid UUID for contact: " + contact);
      }
    }

    request.setIdentifier(webRequest.getParameter("identifier"));
    request.setIdentifierType(
        VocabularyUtils.lookupEnum(
            webRequest.getParameter("identifierType"), IdentifierType.class));
    request.setMachineTagName(webRequest.getParameter("machineTagName"));
    request.setMachineTagNamespace(webRequest.getParameter("machineTagNamespace"));
    request.setMachineTagValue(webRequest.getParameter("machineTagValue"));
    request.setQ(webRequest.getParameter("q"));
  }
}
