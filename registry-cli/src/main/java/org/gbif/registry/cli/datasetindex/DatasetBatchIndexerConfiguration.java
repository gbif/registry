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
package org.gbif.registry.cli.datasetindex;

import org.gbif.registry.cli.common.DbConfiguration;
import org.gbif.registry.search.dataset.indexing.es.IndexingConstants;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

/** A configuration exclusively for DatasetUpdater. */
@Data
public class DatasetBatchIndexerConfiguration {

  private String apiRootUrl;

  private String registryWsUrl;

  private DbConfiguration clbDb;

  private ElasticsearchConfig datasetEs;

  private ElasticsearchConfig occurrenceEs;

  private Integer stopAfter = -1;

  private Integer pageSize = 50;

  private Map<String, String> indexingSettings =
      new HashMap<>(IndexingConstants.DEFAULT_INDEXING_SETTINGS);

  private Map<String, String> searchSettings =
      new HashMap<>(IndexingConstants.DEFAULT_SEARCH_SETTINGS);
}
