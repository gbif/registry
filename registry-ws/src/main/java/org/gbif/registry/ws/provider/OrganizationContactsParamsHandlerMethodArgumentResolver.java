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

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.registry.search.ContactsSearchParams;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.GbifRegion;
import org.gbif.ws.server.provider.PageableProvider;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@SuppressWarnings("NullableProblems")
public class OrganizationContactsParamsHandlerMethodArgumentResolver
    implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return ContactsSearchParams.class.equals(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {

    ContactsSearchParams params = new ContactsSearchParams();

    // page
    Pageable page = PageableProvider.getPagingRequest(webRequest, Integer.MAX_VALUE);
    params.setLimit(page.getLimit());
    params.setOffset(page.getOffset());

    String[] countryParams = webRequest.getParameterValues("country");
    if (countryParams != null && countryParams.length > 0) {
      for (int i = 0; i < countryParams.length; i++) {
        String countryParam = countryParams[i];
        Country country = Country.fromIsoCode(countryParam);
        if (country == null) {
          // if nothing found also try by enum name
          country = VocabularyUtils.lookupEnum(countryParam, Country.class);
        }

        if (country != null) {
          params.getCountry().add(country);
        }
      }
    }

    String[] gbifRegionParams = webRequest.getParameterValues("gbifRegion");
    if (gbifRegionParams != null && gbifRegionParams.length > 0) {
      for (int i = 0; i < gbifRegionParams.length; i++) {
        String gbifRegionParam = gbifRegionParams[i];
        GbifRegion gbifRegion = GbifRegion.fromString(gbifRegionParam);
        if (gbifRegion != null) {
          params.getGbifRegion().add(gbifRegion);
        }
      }
    }

    String[] typeParams = webRequest.getParameterValues("type");
    if (typeParams != null && typeParams.length > 0) {
      for (int i = 0; i < typeParams.length; i++) {
        String typeParam = typeParams[i];
        ContactType contactType = ContactType.fromString(typeParam);
        if (contactType != null) {
          params.getType().add(contactType);
        }
      }
    }

    return params;
  }
}
