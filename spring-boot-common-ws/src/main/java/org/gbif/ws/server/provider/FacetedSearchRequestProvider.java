package org.gbif.ws.server.provider;

import com.google.common.base.Strings;
import org.gbif.api.model.common.search.FacetedSearchRequest;
import org.gbif.api.model.common.search.SearchParameter;
import org.springframework.web.context.request.WebRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.gbif.ws.util.CommonWsUtils.getFirst;
import static org.gbif.ws.util.WebserviceParameter.PARAM_FACET;
import static org.gbif.ws.util.WebserviceParameter.PARAM_FACET_LIMIT;
import static org.gbif.ws.util.WebserviceParameter.PARAM_FACET_MINCOUNT;
import static org.gbif.ws.util.WebserviceParameter.PARAM_FACET_MULTISELECT;
import static org.gbif.ws.util.WebserviceParameter.PARAM_FACET_OFFSET;

/**
 * Provider class that transforms a set of HTTP parameters into a FacetedSearchRequest class instance.
 * This assumes the existence of the following parameters in the HTTP request:
 * 'page_size', 'offset', 'facet', 'q' and any of the search parameter enum member names case insensitively.
 */
public class FacetedSearchRequestProvider<RT extends FacetedSearchRequest<P>, P extends Enum<?> & SearchParameter>
    extends SearchRequestProvider<RT, P> {

  private static final int DEFAULT_FACET_LIMIT = 10;

  public FacetedSearchRequestProvider(Class<RT> requestType, Class<P> searchParameterClass) {
    super(requestType, searchParameterClass);
  }

  @Override
  protected RT getSearchRequest(WebRequest webRequest, RT searchRequest) {
    RT request = super.getSearchRequest(webRequest, searchRequest);

    final Map<String, String[]> params = webRequest.getParameterMap();

    final String facetMultiSelectValue = getFirstIgnoringCase(PARAM_FACET_MULTISELECT, params);
    if (facetMultiSelectValue != null) {
      searchRequest.setMultiSelectFacets(Boolean.parseBoolean(facetMultiSelectValue));
    }

    final String facetMinCountValue = getFirstIgnoringCase(PARAM_FACET_MINCOUNT, params);
    if (facetMinCountValue != null) {
      searchRequest.setFacetMinCount(Integer.parseInt(facetMinCountValue));
    }

    final String facetLimit = getFirstIgnoringCase(PARAM_FACET_LIMIT, params);
    if (facetLimit != null) {
      searchRequest.setFacetLimit(Integer.parseInt(facetLimit));
    }

    final String facetOffset = getFirstIgnoringCase(PARAM_FACET_OFFSET, params);
    if (facetOffset != null) {
      searchRequest.setFacetOffset(Integer.parseInt(facetOffset));
    }

    final List<String> facets = params.get(PARAM_FACET) != null ? Arrays.asList(params.get(PARAM_FACET)) : Collections.emptyList();
    if (!facets.isEmpty()) {
      for (String f : facets) {
        P p = findSearchParam(f);
        if (p != null) {
          searchRequest.addFacets(p);
          String pFacetOffset = getFirstIgnoringCase(f + '.' + PARAM_FACET_OFFSET, params);
          String pFacetLimit = getFirstIgnoringCase(f + '.' + PARAM_FACET_LIMIT, params);
          if (pFacetLimit != null) {
            if (pFacetOffset != null) {
              searchRequest.addFacetPage(p, Integer.parseInt(pFacetOffset), Integer.parseInt(pFacetLimit));
            } else {
              searchRequest.addFacetPage(p, 0, Integer.parseInt(pFacetLimit));
            }
          } else if (pFacetOffset != null) {
            searchRequest.addFacetPage(p, Integer.parseInt(pFacetOffset), DEFAULT_FACET_LIMIT);
          }
        }
      }
    }

    return request;
  }

  /**
   * Get the first parameter value, the parameter is searched in a case-insensitive manner.
   * First tries with the exact match, then the lowercase and finally the uppercase value of the parameter.
   */
  private static String getFirstIgnoringCase(String parameter, Map<String, String[]> params) {
    String value = getFirst(params, parameter);
    if (!Strings.isNullOrEmpty(value)) {
      return value;
    }
    value = getFirst(params, parameter.toLowerCase());
    if (!Strings.isNullOrEmpty(value)) {
      return value;
    }
    value = getFirst(params, parameter.toUpperCase());
    if (!Strings.isNullOrEmpty(value)) {
      return value;
    }
    return null;
  }
}
