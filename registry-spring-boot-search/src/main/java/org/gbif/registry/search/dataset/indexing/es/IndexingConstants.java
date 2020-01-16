package org.gbif.registry.search.dataset.indexing.es;

import java.util.HashMap;
import java.util.Map;

import lombok.experimental.UtilityClass;

/**
 * Constants used for indexing into Elastisearch.
 */
@UtilityClass
public class IndexingConstants {

  public static final String DATASET_RECORD_TYPE = "dataset";

  public static final String ALIAS = "dataset";

  public static final String MAPPING_FILE = "dataset-es-mapping.json";

  /**
   * Default/Recommended indexing settings.
   */
  public static final Map<String, String> DEFAULT_INDEXING_SETTINGS = new HashMap<>();

  static {
    DEFAULT_INDEXING_SETTINGS.put("index.refresh_interval", "-1");
    DEFAULT_INDEXING_SETTINGS.put("index.number_of_shards", "3");
    DEFAULT_INDEXING_SETTINGS.put("index.number_of_replicas", "0");
    DEFAULT_INDEXING_SETTINGS.put("index.translog.durability", "async");
  }

  /**
   * Default/recommended setting for search/production mode.
   */
  public static final Map<String, String> DEFAULT_SEARCH_SETTINGS = new HashMap<>();

  static {
    DEFAULT_SEARCH_SETTINGS.put("index.refresh_interval", "1s");
    DEFAULT_SEARCH_SETTINGS.put("index.number_of_replicas", "1");
  }

}
