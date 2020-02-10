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
package org.gbif.registry.events.search.dataset.search.common;

import org.gbif.api.model.common.search.SearchParameter;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

public interface EsFieldMapper<P extends SearchParameter> {

  String get(P searchParameter);

  P get(String esFieldName);

  /** @return a list of fields to be excluded in the _source field. */
  String[] excludeFields();

  /**
   * Fields to be included in a suggest response. By default only the requested parameter field is
   * returned.
   */
  default String[] includeSuggestFields(P searchParameter) {
    return new String[] {get(searchParameter)};
  }

  /** Fields used during to highlight in results. */
  default String[] highlightingFields() {
    return new String[] {};
  }

  /** Builds a full text search query builder. */
  default QueryBuilder fullTextQuery(String q) {
    return QueryBuilders.matchQuery("all", q);
  }
}
