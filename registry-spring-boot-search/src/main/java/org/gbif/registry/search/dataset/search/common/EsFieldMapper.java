package org.gbif.registry.search.dataset.search.common;

import org.gbif.api.model.common.search.SearchParameter;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

public interface EsFieldMapper<P extends SearchParameter> {


  String get(P searchParameter);

  P get(String esFieldName);

  /**
   * @return a list of fields to be excluded in the _source field.
   */
  String[] excludeFields();

  /**
   * Fields to be included in a suggest response.
   * By default only the requested parameter field is returned.
   */
  default String[] includeSuggestFields(P searchParameter) {
    return new String[]{get(searchParameter)};
  }

  /**
   * Fields used during to highlight in results.
   */
  default String[] highlightingFields() {
    return new String[]{};
  }

  /**
   * Builds a full text search query builder.
   */
  default QueryBuilder fullTextQuery(String q) {
    return QueryBuilders.matchQuery("all", q);
  }
}
