package org.gbif.ws.util;

/**
 * Parameters used in the API webservices.
 */
public final class WebserviceParameter {

  /**
   * The query string for searches.
   * Repeated in SearchConstants, couldnt resolve dependencies.
   */
  public static final String PARAM_QUERY_STRING = "q";

  /**
   * The query fields to search within.
   */
  public static final String PARAM_QUERY_FIELD = "qField";

  /**
   * Facet param name.
   */
  public static final String PARAM_FACET = "facet";

  /**
   * Facet multiselect parameter.
   */
  public static final String PARAM_FACET_MULTISELECT = "facetMultiselect";

  public static final String PARAM_FACET_LIMIT = "facetLimit";

  public static final String PARAM_FACET_OFFSET = "facetOffset";

  /**
   * Parameter min count of facets, facets with less than this valued sholdn't be included in the response.
   */
  public static final String PARAM_FACET_MINCOUNT = "facetMincount";

  public static final String PARAM_HIGHLIGHT = "hl";

  public static final String PARAM_HIGHLIGHT_FIELD = "hlField";

  public static final String PARAM_HIGHLIGHT_CONTEXT = "hlContext";

  /**
   * spellCheck parameter.
   */
  public static final String PARAM_SPELLCHECK = "spellCheck";

  /**
   * spellCheckCount parameter.
   */
  public static final String PARAM_SPELLCHECK_COUNT = "spellCheckCount";

  /**
   *
   */
  public static final String PARAM_EXTENDED = "extended";

  public static final String DEFAULT_SEARCH_PARAM_VALUE = "*";


  private WebserviceParameter() {
    throw new UnsupportedOperationException("Can't initialize utils class");
  }
}