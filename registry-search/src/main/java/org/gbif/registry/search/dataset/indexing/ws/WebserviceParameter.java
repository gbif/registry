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

/** Parameters used in the API webservices. */
public class WebserviceParameter {

  /** The query string for searches. Repeated in SearchConstants, couldnt resolve dependencies. */
  public static final String PARAM_QUERY_STRING = "q";

  /** The query fields to search within. */
  public static final String PARAM_QUERY_FIELD = "qField";

  /** Facet param name. */
  public static final String PARAM_FACET = "facet";

  /** Facet multiselect parameter. */
  public static final String PARAM_FACET_MULTISELECT = "facetMultiselect";

  public static final String PARAM_FACET_LIMIT = "facetLimit";

  public static final String PARAM_FACET_OFFSET = "facetOffset";

  /**
   * Parameter min count of facets, facets with less than this valued sholdn't be included in the
   * response.
   */
  public static final String PARAM_FACET_MINCOUNT = "facetMincount";

  public static final String PARAM_HIGHLIGHT = "hl";

  public static final String PARAM_HIGHLIGHT_FIELD = "hlField";

  public static final String PARAM_HIGHLIGHT_CONTEXT = "hlContext";

  /** spellCheck parameter. */
  public static final String PARAM_SPELLCHECK = "spellCheck";

  /** spellCheckCount parameter. */
  public static final String PARAM_SPELLCHECK_COUNT = "spellCheckCount";

  /** */
  public static final String PARAM_EXTENDED = "extended";

  public static final String DEFAULT_SEARCH_PARAM_VALUE = "*";

  private WebserviceParameter() {
    throw new UnsupportedOperationException("Can't initialize utils class");
  }
}
