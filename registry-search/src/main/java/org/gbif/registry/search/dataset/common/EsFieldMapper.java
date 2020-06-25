/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.registry.search.dataset.common;

import org.gbif.api.model.common.search.SearchParameter;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilder;

public interface EsFieldMapper<P extends SearchParameter> {

  /**
   * Looks-up the Elasticsearch field name linked to a search parameter.
   *
   * @param searchParameter to lookup-up
   * @return the associated Elasticsearch field or null otherwise
   */
  String get(P searchParameter);

  /**
   * Looks-up the {@link SearchParameter} linked to a ElasticSearch field.
   *
   * @param esFieldName to look-up
   * @return the search parameter associated to the field
   */
  P get(String esFieldName);

  /**
   * Looks-up for the estimate cardinality of ElasticSearch field.
   *
   * @param esFieldName to look-up
   * @return the estimated cardinality
   */
  Integer getCardinality(String esFieldName);

  /**
   * Checks if a ElasticSearch fields is mapped to date data type.
   *
   * @param esFieldName to look-up
   * @return true of the field is date type field, false otherwise
   */
  boolean isDateField(String esFieldName);

  /** @return a list of fields to be excluded in the _source field. */
  String[] excludeFields();

  /** @return the default sorting of results */
  SortBuilder<? extends SortBuilder>[] sorts();

  /**
   * Fields to be included in a suggest response. By default only the requested parameter field is
   * returned.
   */
  default String[] includeSuggestFields(P searchParameter) {
    return new String[] {get(searchParameter)};
  }

  /**
   * Gets the autocomplete field associated to a parameter. By default returns the mapped es field +
   * the "Autocomplete" word.
   */
  default String getAutocompleteField(P searchParameter) {
    return get(searchParameter) + "Autocomplete";
  }

  /** Fields used during to highlight in results. */
  default String[] highlightingFields() {
    return new String[] {};
  }

  /** Builds a full text search query builder. */
  default QueryBuilder fullTextQuery(String q) {
    return QueryBuilders.matchQuery("all", q);
  }

  /**
   * List of all ES fields mapped to API responses. Only these fields will be included a in _source
   * field. An empty array means, all fields are mapped and must be included in the _source field.
   *
   * @return
   */
  default String[] getMappedFields() {
    return new String[0];
  }
}
