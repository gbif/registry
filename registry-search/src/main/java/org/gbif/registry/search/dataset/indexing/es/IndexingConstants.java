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
package org.gbif.registry.search.dataset.indexing.es;

import java.util.HashMap;
import java.util.Map;

import lombok.experimental.UtilityClass;

/** Constants used for indexing into Elastisearch. */
@UtilityClass
public class IndexingConstants {

  public static final String DATASET_RECORD_TYPE = "dataset";

  public static final String ALIAS = "dataset";

  public static final String MAPPING_FILE = "dataset-es-mapping.json";

  public static final String SETTINGS_FILE = "dataset-es-settings.json";

  /** Default/Recommended indexing settings. */
  public static final Map<String, String> DEFAULT_INDEXING_SETTINGS = new HashMap<>();

  static {
    DEFAULT_INDEXING_SETTINGS.put("index.refresh_interval", "-1");
    DEFAULT_INDEXING_SETTINGS.put("index.number_of_shards", "1");
    DEFAULT_INDEXING_SETTINGS.put("index.number_of_replicas", "0");
    DEFAULT_INDEXING_SETTINGS.put("index.translog.durability", "async");
  }

  /** Default/recommended setting for search/production mode. */
  public static final Map<String, String> DEFAULT_SEARCH_SETTINGS = new HashMap<>();

  static {
    DEFAULT_SEARCH_SETTINGS.put("index.refresh_interval", "1s");
    DEFAULT_SEARCH_SETTINGS.put("index.number_of_replicas", "1");
    DEFAULT_SEARCH_SETTINGS.put("index.max_result_window", "100000");
  }
}
