package org.gbif.registry.search.dataset.search.common;

import org.gbif.api.model.common.search.SearchParameter;

public interface EsFieldParameterMapper<P extends SearchParameter> {


  String get(P searchParameter);

  P get(String esFieldName);
}
