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

import java.util.Map;
import java.util.UUID;
import org.gbif.api.model.collections.request.CollectionSearchRequest;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

@SuppressWarnings("NullableProblems")
public class CollectionSearchRequestHandlerMethodArgumentResolver
    extends BaseGrSciCollSearchRequestHandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return CollectionSearchRequest.class.equals(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {

    CollectionSearchRequest searchRequest = CollectionSearchRequest.builder().build();
    fillCollectionSearchRequest(searchRequest, webRequest);

    return searchRequest;
  }

  protected void fillCollectionSearchRequest(
      CollectionSearchRequest searchRequest, NativeWebRequest webRequest) {
    fillSearchRequestParams(searchRequest, webRequest);

    Map<String, String[]> params = toCaseInsensitiveParams(webRequest);
    extractMultivalueParam(params, "institution", UUID::fromString)
        .ifPresent(searchRequest::setInstitution);
    extractMultivalueParam(params, "contentType").ifPresent(searchRequest::setContentTypes);
    extractMultivalueParam(params, "preservationType")
        .ifPresent(searchRequest::setPreservationTypes);
    extractMultivalueParam(params, "accessionStatus").ifPresent(searchRequest::setAccessionStatus);
    extractMultivalueParam(params, "personalCollection", Boolean::parseBoolean)
        .ifPresent(searchRequest::setPersonalCollection);
  }
}
