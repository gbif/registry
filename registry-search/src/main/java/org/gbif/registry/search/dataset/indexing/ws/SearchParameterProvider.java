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
package org.gbif.registry.search.dataset.indexing.ws;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.search.FacetedSearchRequest;
import org.gbif.api.model.common.search.SearchParameter;
import org.gbif.api.model.common.search.SearchRequest;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import lombok.experimental.UtilityClass;

import static org.gbif.registry.search.dataset.indexing.ws.WebserviceParameter.DEFAULT_SEARCH_PARAM_VALUE;
import static org.gbif.registry.search.dataset.indexing.ws.WebserviceParameter.PARAM_FACET;
import static org.gbif.registry.search.dataset.indexing.ws.WebserviceParameter.PARAM_FACET_LIMIT;
import static org.gbif.registry.search.dataset.indexing.ws.WebserviceParameter.PARAM_FACET_MINCOUNT;
import static org.gbif.registry.search.dataset.indexing.ws.WebserviceParameter.PARAM_FACET_MULTISELECT;
import static org.gbif.registry.search.dataset.indexing.ws.WebserviceParameter.PARAM_FACET_OFFSET;
import static org.gbif.registry.search.dataset.indexing.ws.WebserviceParameter.PARAM_HIGHLIGHT;
import static org.gbif.registry.search.dataset.indexing.ws.WebserviceParameter.PARAM_QUERY_STRING;
import static org.gbif.registry.search.dataset.indexing.ws.WebserviceParameter.PARAM_SPELLCHECK;
import static org.gbif.registry.search.dataset.indexing.ws.WebserviceParameter.PARAM_SPELLCHECK_COUNT;

@UtilityClass
public class SearchParameterProvider {

  public static <P extends SearchParameter, R extends FacetedSearchRequest<P>>
      ProxyRetrofitQueryMap getParameterFromFacetedRequest(@Nullable R searchRequest) {
    // The searchRequest is transformed in a parameter map
    ProxyRetrofitQueryMap parameters = getParameterFromSearchRequest(searchRequest);

    if (searchRequest != null) {
      parameters.put(
          PARAM_FACET_MULTISELECT, Boolean.toString(searchRequest.isMultiSelectFacets()));
      if (searchRequest.getFacetMinCount() != null) {
        parameters.put(PARAM_FACET_MINCOUNT, Integer.toString(searchRequest.getFacetMinCount()));
      }
      if (searchRequest.getFacetLimit() != null) {
        parameters.put(PARAM_FACET_LIMIT, Integer.toString(searchRequest.getFacetLimit()));
      }
      if (searchRequest.getFacetOffset() != null) {
        parameters.put(PARAM_FACET_OFFSET, Integer.toString(searchRequest.getFacetOffset()));
      }
      if (searchRequest.getFacets() != null) {
        parameters.put(
            PARAM_FACET,
            searchRequest.getFacets().stream()
                .map(SearchParameter::name)
                .collect(Collectors.toList()));
        for (P facet : searchRequest.getFacets()) {
          Pageable facetPage = searchRequest.getFacetPage(facet);
          if (facetPage != null) {
            parameters.put(
                facet.name() + '.' + PARAM_FACET_OFFSET, Long.toString(facetPage.getOffset()));
            parameters.put(
                facet.name() + '.' + PARAM_FACET_LIMIT, Long.toString(facetPage.getLimit()));
          }
        }
      }
    }

    return parameters;
  }

  public static <P extends SearchParameter> ProxyRetrofitQueryMap getParameterFromSearchRequest(
      @Nullable SearchRequest<P> searchRequest) {

    // The searchRequest is transformed in a parameter map
    ProxyRetrofitQueryMap parameters = new ProxyRetrofitQueryMap();

    if (searchRequest == null) {
      parameters.put(PARAM_QUERY_STRING, DEFAULT_SEARCH_PARAM_VALUE);
    } else {
      String searchParamValue = searchRequest.getQ();
      if (!Strings.isNullOrEmpty(searchParamValue)) {
        parameters.put(PARAM_QUERY_STRING, searchParamValue);
      }
      parameters.put(PARAM_HIGHLIGHT, Boolean.toString(searchRequest.isHighlight()));
      parameters.put(PARAM_SPELLCHECK, Boolean.toString(searchRequest.isSpellCheck()));
      parameters.put(PARAM_SPELLCHECK_COUNT, Integer.toString(searchRequest.getSpellCheckCount()));

      Map<P, Set<String>> requestParameters = searchRequest.getParameters();
      if (requestParameters != null) {
        for (P param : requestParameters.keySet()) {
          parameters.put(param.name(), Lists.newArrayList(requestParameters.get(param)));
        }
      }
    }
    return parameters;
  }

  public static class ProxyRetrofitQueryMap extends HashMap<String, Object> {
    public ProxyRetrofitQueryMap() {
      super(new HashMap<>());
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
      Set<Entry<String, Object>> originSet = super.entrySet();
      Set<Entry<String, Object>> newSet = new HashSet<>();

      for (Entry<String, Object> entry : originSet) {
        String entryKey = entry.getKey();
        if (entryKey == null) {
          throw new IllegalArgumentException("Query map contained null key.");
        }
        Object entryValue = entry.getValue();
        if (entryValue == null) {
          throw new IllegalArgumentException(
              "Query map contained null value for key '" + entryKey + "'.");
        } else if (entryValue instanceof List) {
          for (Object arrayValue : (List) entryValue) {
            if (arrayValue != null) { // Skip null values
              Entry<String, Object> newEntry = new AbstractMap.SimpleEntry<>(entryKey, arrayValue);
              newSet.add(newEntry);
            }
          }
        } else {
          Entry<String, Object> newEntry = new AbstractMap.SimpleEntry<>(entryKey, entryValue);
          newSet.add(newEntry);
        }
      }
      return newSet;
    }
  }
}
