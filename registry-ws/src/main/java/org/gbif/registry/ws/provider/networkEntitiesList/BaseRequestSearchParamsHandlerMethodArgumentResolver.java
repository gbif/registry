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

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.registry.search.RequestSearchParams;
import org.gbif.api.util.SearchTypeValidator;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.ws.server.provider.PageableProvider;

import java.util.Optional;

import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

@SuppressWarnings("NullableProblems")
public abstract class BaseRequestSearchParamsHandlerMethodArgumentResolver
    implements HandlerMethodArgumentResolver {

  public static final int MAX_PAGE_SIZE = 1000;

  public Object fillCommonParams(RequestSearchParams params, NativeWebRequest webRequest) {

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
    Optional.ofNullable(webRequest.getParameter(RequestSearchParams.MODIFIED_PARAM))
        .ifPresent(v -> params.setModified(SearchTypeValidator.parseDateRange(v)));

    // page
    Pageable page = PageableProvider.getPagingRequest(webRequest, MAX_PAGE_SIZE);
    params.setLimit(page.getLimit());
    params.setOffset(page.getOffset());

    return params;
  }
}
