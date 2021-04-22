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

import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension;
import io.zonky.test.db.postgres.junit5.SingleInstancePostgresExtension;

@DisabledIfSystemProperty(named = "test.indexer", matches = "false")
public class DatasetBatchIndexerIT {

  private static final int DATASETS_TO_INDEX = 5;

  private static final String OCCURRENCE_INDEX_NAME = "occurrence";

  private static final String API_URL = "http://api.gbif-dev.org/v1/";

  private static final String INDEX_NAME = IndexingConstants.ALIAS + '_' + new Date().getTime();

  private static final String INDEX_ALIAS = INDEX_NAME + "_a" ;

  @RegisterExtension
  public static SingleInstancePostgresExtension database =
      EmbeddedPostgresExtension.singleInstance();

  private static ElasticsearchContainer embeddedElastic;

  private static String getEsVersion() throws IOException {
    Properties properties = new Properties();
    properties.load(
        DatasetBatchIndexerIT.class.getClassLoader().getResourceAsStream("maven.properties"));
    return properties.getProperty("elasticsearch.version");
  }

  private RestHighLevelClient buildRestClient() {
    HttpHost host = new HttpHost("localhost", embeddedElastic.getMappedPort(9200));
    return new RestHighLevelClient(RestClient.builder(host));
  }

  @BeforeAll
  public static void init() throws Exception {
    embeddedElastic =
        new ElasticsearchContainer(
            "docker.elastic.co/elasticsearch/elasticsearch:" + getEsVersion());
    embeddedElastic.withReuse(true);
    embeddedElastic.start();
  }

  DatasetBatchIndexerConfiguration getConfiguration() {

    DatasetBatchIndexerConfiguration configuration = new DatasetBatchIndexerConfiguration();

    ElasticsearchConfig elasticsearchConfigDataset = new ElasticsearchConfig();
    elasticsearchConfigDataset.setHosts("http://" + embeddedElastic.getHttpHostAddress());
    elasticsearchConfigDataset.setAlias(INDEX_ALIAS);
    elasticsearchConfigDataset.setIndex(INDEX_NAME);
    configuration.setDatasetEs(elasticsearchConfigDataset);

    ElasticsearchConfig elasticsearchConfigOccurrence = new ElasticsearchConfig();
    elasticsearchConfigOccurrence.setHosts("http://" + embeddedElastic.getHttpHostAddress());
    elasticsearchConfigOccurrence.setAlias(OCCURRENCE_INDEX_NAME);
    configuration.setOccurrenceEs(elasticsearchConfigOccurrence);

    configuration.setApiRootUrl(API_URL);

    configuration.setIndexClb(false);

    DbConfiguration dbConfiguration = new DbConfiguration();
    dbConfiguration.serverName = "localhost:" + database.getEmbeddedPostgres().getPort();
    dbConfiguration.databaseName = "postgres";
    dbConfiguration.user = "postgres";
    dbConfiguration.password = "";

    configuration.setClbDb(dbConfiguration);

    // Only 10 dataset must be indexed
    configuration.setStopAfter(DATASETS_TO_INDEX);
    return configuration;
  }

  @Test
  public void indexTests() throws IOException {

    DatasetBatchIndexerCommand datasetBatchIndexerCommand =
        new DatasetBatchIndexerCommand(getConfiguration());
    datasetBatchIndexerCommand.doRun();

    RestHighLevelClient restHighLevelClient = buildRestClient();

    SearchResponse searchResponse =
      restHighLevelClient
            .search(
                new SearchRequest()
                    .indices(INDEX_ALIAS)
                    .source(new SearchSourceBuilder().size(0)),
                RequestOptions.DEFAULT);

    Assertions.assertEquals(
        DATASETS_TO_INDEX,
        searchResponse.getHits().getTotalHits().value,
        "Wrong amount of indexed dataset");
  }

  @AfterEach
  public void deleteIndex() throws IOException {
    buildRestClient().indices().delete(new DeleteIndexRequest(INDEX_NAME), RequestOptions.DEFAULT);
  }

  @AfterAll
  public static void finish() throws Exception {
    if (embeddedElastic != null && !embeddedElastic.isShouldBeReused()) {
      embeddedElastic.stop();
    }
  }
}
