package org.gbif.ws.server.provider;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.gbif.api.model.common.search.SearchParameter;
import org.gbif.api.model.common.search.SearchRequest;
import org.gbif.api.util.SearchTypeValidator;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.ws.CommonRuntimeException;
import org.springframework.web.context.request.WebRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static org.gbif.ws.util.CommonWsUtils.getFirst;
import static org.gbif.ws.util.WebserviceParameter.PARAM_HIGHLIGHT;
import static org.gbif.ws.util.WebserviceParameter.PARAM_QUERY_STRING;
import static org.gbif.ws.util.WebserviceParameter.PARAM_SPELLCHECK;
import static org.gbif.ws.util.WebserviceParameter.PARAM_SPELLCHECK_COUNT;

/**
 * Provider class that transforms a set of HTTP parameters into a SearchRequest class instance.
 * This assumes the existence of the following parameters in the HTTP request:
 * 'page_size', 'offset', 'q' and any of the search parameter enum member names case insensitively.
 */
public class SearchRequestProvider<RT extends SearchRequest<P>, P extends Enum<?> & SearchParameter> implements ContextProvider<RT> {

  private final Class<P> searchParameterClass;
  private final Class<RT> requestType;
  private static final int NON_SPELL_CHECK_COUNT = -1;

  public SearchRequestProvider(Class<RT> requestType, Class<P> searchParameterClass) {
    this.requestType = requestType;
    this.searchParameterClass = searchParameterClass;
  }

  @Override
  public RT getValue(WebRequest webRequest) {
    try {
      RT req = requestType.newInstance();
      return getSearchRequest(webRequest, req);
    } catch (InstantiationException | IllegalAccessException e) {
      // should never happen
      throw new CommonRuntimeException(e);
    }
  }

  protected P findSearchParam(String name) {
    try {
      return VocabularyUtils.lookupEnum(name, searchParameterClass);
    } catch (IllegalArgumentException e) {
      // we have all params here, not only the enum ones, so this is ok to end up here a few times
    }
    return null;
  }

  protected RT getSearchRequest(WebRequest webRequest, RT searchRequest) {
    searchRequest.copyPagingValues(PageableProvider.getPagingRequest(webRequest));

    final Map<String, String[]> params = webRequest.getParameterMap();

    getSearchRequestFromQueryParams(searchRequest, params);

    return searchRequest;
  }

  /**
   * Override this method for populating specific search/suggest requests
   */
  protected void getSearchRequestFromQueryParams(RT searchRequest, final Map<String, String[]> params) {
    final String q = getFirst(params, PARAM_QUERY_STRING);
    final String highlightValue = getFirst(params, PARAM_HIGHLIGHT);
    final String spellCheck = getFirst(params, PARAM_SPELLCHECK);
    final String spellCheckCount = getFirst(params, PARAM_SPELLCHECK_COUNT);

    if (!Strings.isNullOrEmpty(q)) {
      searchRequest.setQ(q);
    }

    if (!Strings.isNullOrEmpty(highlightValue)) {
      searchRequest.setHighlight(Boolean.parseBoolean(highlightValue));
    }

    if (!Strings.isNullOrEmpty(spellCheck)) {
      searchRequest.setSpellCheck(Boolean.parseBoolean(spellCheck));
    }

    if (!Strings.isNullOrEmpty(spellCheckCount)) {
      searchRequest.setSpellCheckCount(Integer.parseInt(spellCheckCount));
    } else {
      searchRequest.setSpellCheckCount(NON_SPELL_CHECK_COUNT);
    }

    // find search parameter enum based filters
    setSearchParams(searchRequest, params);
  }

  /**
   * Removes all empty and null parameters from the list.
   * Each value is trimmed(String.trim()) in order to remove all sizes of empty parameters.
   */
  private static List<String> removeEmptyParameters(List<String> parameters) {
    List<String> cleanParameters = Lists.newArrayListWithCapacity(parameters.size());
    for (String param : parameters) {
      String cleanParam = Strings.nullToEmpty(param).trim();
      if (!cleanParam.isEmpty()) {
        cleanParameters.add(cleanParam);
      }
    }
    return cleanParameters;
  }

  /**
   * Iterates over the params map and adds to the search request the recognized parameters (i.e.: those that have a
   * correspondent value in the P generic parameter).
   * Empty (of all size) and null parameters are discarded.
   */
  private void setSearchParams(RT searchRequest, Map<String, String[]> params) {
    for (Entry<String, String[]> entry : params.entrySet()) {
      P p = findSearchParam(entry.getKey());
      if (p != null) {
        final List<String> list = entry.getValue() != null ? Arrays.asList(entry.getValue()) : Collections.emptyList();
        for (String val : removeEmptyParameters(list)) {
          // validate value for certain types
          SearchTypeValidator.validate(p, val);
          searchRequest.addParameter(p, val);
        }
      }
    }
  }
}
