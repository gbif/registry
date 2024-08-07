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

import com.google.common.base.Strings;
import java.util.Arrays;
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

    String institution = webRequest.getParameter("institution");
    if (!Strings.isNullOrEmpty(institution)) {
      try {
        searchRequest.setInstitution(UUID.fromString(institution));
      } catch (Exception e) {
        throw new IllegalArgumentException("Invalid UUID for institution: " + institution);
      }
    }

    String[] contentTypes = webRequest.getParameterValues("contentType");
    if (contentTypes != null && contentTypes.length > 0) {
      searchRequest.setContentTypes(Arrays.asList(contentTypes));
    }

    String[] preservationTypes = webRequest.getParameterValues("preservationType");
    if (preservationTypes != null && preservationTypes.length > 0) {
      searchRequest.setPreservationTypes(Arrays.asList(preservationTypes));
    }

    String[] accessionStatuses = webRequest.getParameterValues("accessionStatus");
    if (accessionStatuses != null && accessionStatuses.length > 0) {
      searchRequest.setAccessionStatus(Arrays.asList(accessionStatuses));
    }

    String personalCollection = webRequest.getParameter("personalCollection");
    if (!Strings.isNullOrEmpty(personalCollection)) {
      try {
        searchRequest.setPersonalCollection(Boolean.parseBoolean(personalCollection));
      } catch (Exception e) {
        throw new IllegalArgumentException(
            "Invalid boolean for personalCollection: " + personalCollection);
      }
    }
  }
}
