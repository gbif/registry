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

import org.gbif.api.model.collections.request.SearchRequest;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.collections.MasterSourceType;
import org.gbif.registry.service.collections.utils.SearchUtils;
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
    request.setCity(webRequest.getParameter("city"));
    request.setFuzzyName(webRequest.getParameter("fuzzyName"));

    String active = webRequest.getParameter("active");
    if (!Strings.isNullOrEmpty(active)) {
      try {
        request.setActive(Boolean.parseBoolean(active));
      } catch (Exception e) {
        throw new IllegalArgumentException("Invalid boolean for active: " + active);
      }
    }

    String countryParam = webRequest.getParameter("country");
    if (!Strings.isNullOrEmpty(countryParam)) {
      Country country = Country.fromIsoCode(countryParam);

      if (country == null) {
        // if nothing found also try by enum name
        country = VocabularyUtils.lookupEnum(countryParam, Country.class);
      }

      request.setCountry(country);
    }

    String masterSourceTypeParam = webRequest.getParameter("masterSourceType");
    if (!Strings.isNullOrEmpty(masterSourceTypeParam)) {
      try {
        request.setMasterSourceType(MasterSourceType.valueOf(masterSourceTypeParam));
      } catch (Exception e) {
        throw new IllegalArgumentException("Invalid master source type: " + masterSourceTypeParam);
      }
    }

    String numberSpecimensParam = webRequest.getParameter("numberSpecimens");
    if (!Strings.isNullOrEmpty(numberSpecimensParam)) {
      boolean rangeMatch = SearchUtils.NUMBER_SPECIMENS_RANGE.matcher(numberSpecimensParam).find();
      boolean numberMatch = true;
      try {
        Integer.parseInt(numberSpecimensParam);
      } catch (NumberFormatException ex) {
        numberMatch = false;
      }
      if (!rangeMatch && !numberMatch) {
        throw new IllegalArgumentException(
            "Invalid numberSpecimens parameter. Only a number or a range is accepted: "
                + numberSpecimensParam);
      }
      request.setNumberSpecimens(numberSpecimensParam);
    }

    String displayOnNHCPortal = webRequest.getParameter("displayOnNHCPortal");
    if (!Strings.isNullOrEmpty(displayOnNHCPortal)) {
      try {
        request.setDisplayOnNHCPortal(Boolean.parseBoolean(displayOnNHCPortal));
      } catch (Exception e) {
        throw new IllegalArgumentException(
            "Invalid boolean for displayOnNHCPortal: " + displayOnNHCPortal);
      }
    }
  }
}
