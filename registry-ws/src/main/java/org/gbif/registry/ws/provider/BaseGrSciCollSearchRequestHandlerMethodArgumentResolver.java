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
import org.gbif.api.vocabulary.CollectionsSortField;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.GbifRegion;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.SortOrder;
import org.gbif.api.vocabulary.collections.MasterSourceType;
import org.gbif.registry.service.collections.utils.SearchUtils;
import org.gbif.ws.server.provider.PageableProvider;

import java.util.ArrayList;
import java.util.UUID;

import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import com.google.common.base.Strings;

public abstract class BaseGrSciCollSearchRequestHandlerMethodArgumentResolver
    implements HandlerMethodArgumentResolver {

  public static final int MAX_PAGE_SIZE = 1000;

  protected <T extends SearchRequest> void fillSearchRequestParams(
      T request, NativeWebRequest webRequest) {
    // page
    Pageable page = PageableProvider.getPagingRequest(webRequest, MAX_PAGE_SIZE);
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

    String[] countryParams = webRequest.getParameterValues("country");
    if (countryParams != null && countryParams.length > 0) {
      request.setCountry(new ArrayList<>());
      for (int i = 0; i < countryParams.length; i++) {
        String countryParam = countryParams[i];
        Country country = Country.fromIsoCode(countryParam);
        if (country == null) {
          // if nothing found also try by enum name
          country = VocabularyUtils.lookupEnum(countryParam, Country.class);
        }

        if (country != null) {
          request.getCountry().add(country);
        }
      }
    }

    String[] gbifRegionParams = webRequest.getParameterValues("gbifRegion");
    if (gbifRegionParams != null && gbifRegionParams.length > 0) {
      request.setGbifRegion(new ArrayList<>());
      for (int i = 0; i < gbifRegionParams.length; i++) {
        String gbifRegionParam = gbifRegionParams[i];
        GbifRegion gbifRegion = GbifRegion.fromString(gbifRegionParam);
        if (gbifRegion != null) {
          request.getGbifRegion().add(gbifRegion);
        }
      }
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
      validateIntegerRange(numberSpecimensParam, "numberSpecimens");
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

    String sortByParam = webRequest.getParameter("sortBy");
    if (!Strings.isNullOrEmpty(sortByParam)) {
      try {
        request.setSortBy(CollectionsSortField.valueOf(sortByParam));
      } catch (Exception e) {
        throw new IllegalArgumentException("Invalid sort by parameter: " + sortByParam);
      }
    }

    String sortOrderParam = webRequest.getParameter("sortOrder");
    if (!Strings.isNullOrEmpty(sortOrderParam)) {
      try {
        request.setSortOrder(SortOrder.valueOf(sortOrderParam));
      } catch (Exception e) {
        throw new IllegalArgumentException("Invalid sort order parameter: " + sortOrderParam);
      }
    }

    String occurrenceCountParam = webRequest.getParameter("occurrenceCount");
    if (!Strings.isNullOrEmpty(occurrenceCountParam)) {
      validateIntegerRange(occurrenceCountParam, "occurrenceCount");
      request.setOccurrenceCount(occurrenceCountParam);
    }

    String typeSpecimenCountParam = webRequest.getParameter("typeSpecimenCount");
    if (!Strings.isNullOrEmpty(typeSpecimenCountParam)) {
      validateIntegerRange(typeSpecimenCountParam, "typeSpecimen");
      request.setTypeSpecimenCount(typeSpecimenCountParam);
    }

    String[] institutionKeysParams = webRequest.getParameterValues("institutionKey");
    if (institutionKeysParams != null && institutionKeysParams.length > 0) {
      request.setInstitutionKeys(new ArrayList<>());
      for (String keyParam : institutionKeysParams) {
        try {
          request.getInstitutionKeys().add(UUID.fromString(keyParam));
        } catch (Exception ex) {
          throw new IllegalArgumentException(
              "Invalid UUID for institution key parameter: " + keyParam);
        }
      }
    }
  }

  private static void validateIntegerRange(String param, String paramName) {
    boolean rangeMatch = SearchUtils.INTEGER_RANGE.matcher(param).find();
    boolean numberMatch = true;
    try {
      Integer.parseInt(param);
    } catch (NumberFormatException ex) {
      numberMatch = false;
    }
    if (!rangeMatch && !numberMatch) {
      throw new IllegalArgumentException(
          "Invalid " + paramName + " parameter.Only a number or a range is accepted: " + param);
    }
  }
}
