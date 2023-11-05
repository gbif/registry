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
package org.gbif.registry.search.dataset.indexing.es;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.http.HttpHost;
import org.elasticsearch.client.NodeSelector;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsAliasRequest;
import co.elastic.clients.elasticsearch.indices.FlushRequest;
import co.elastic.clients.elasticsearch.indices.GetAliasRequest;
import co.elastic.clients.elasticsearch.indices.GetAliasResponse;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.elasticsearch.indices.PutIndicesSettingsRequest;
import co.elastic.clients.elasticsearch.indices.UpdateAliasesRequest;
import co.elastic.clients.elasticsearch.indices.update_aliases.Action;
import co.elastic.clients.elasticsearch.indices.update_aliases.AddAction;
import co.elastic.clients.elasticsearch.indices.update_aliases.RemoveAction;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.Data;

/** Generic ElasticSearch wrapper client to encapsulate indexing and admin operations. */
@Component
public class EsClient implements Closeable {

  @Data
  public static class EsClientConfiguration {
    private String hosts;
    private int connectionTimeOut;
    private int socketTimeOut;
    private int connectionRequestTimeOut;
  }

  private final ElasticsearchClient elasticsearchClient;

  @Autowired
  public EsClient(ElasticsearchClient elasticsearchClient) {
    this.elasticsearchClient = elasticsearchClient;
  }

  /**
   * Points the indexName to the alias, and deletes all the indices that were pointing to the alias.
   */
  public void swapAlias(String alias, String indexName) {
    try {
      ExistsAliasRequest existsAliasRequest = new ExistsAliasRequest.Builder().name(alias).build();
      BooleanResponse response = elasticsearchClient.indices().existsAlias(existsAliasRequest);
      List<Action> actions = new ArrayList<>();
      List<String> idxsToDelete = new ArrayList<>();
      actions.add(new Action(new AddAction.Builder().aliases(alias).index(indexName).build()));
      if (response.value()) {
        GetAliasRequest getAliasesRequest = new GetAliasRequest.Builder().name(alias).build();
        GetAliasResponse getAliasesResponse = elasticsearchClient.indices().getAlias(getAliasesRequest);
        idxsToDelete = new ArrayList<>(getAliasesResponse.result().keySet());

        List<Action> deleteActions = idxsToDelete.stream()
          .map(idx -> new Action(new RemoveAction.Builder().alias(alias).index(idx).build()))
          .collect(Collectors.toList());
        if (!deleteActions.isEmpty()) {
          actions.addAll(deleteActions);
        }
      }
      UpdateAliasesRequest updateAliasesRequest = new UpdateAliasesRequest.Builder().actions(actions).build();
      elasticsearchClient.indices().updateAliases(updateAliasesRequest);
      if (!idxsToDelete.isEmpty()) {
        elasticsearchClient
            .indices()
            .delete(
                new DeleteIndexRequest.Builder().index(idxsToDelete).build());
      }
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Flush changes of an index.
   * @param indexName to flush
   */
  public void flushIndex(String indexName) {
    try {
      elasticsearchClient
          .indices()
          .flush(new FlushRequest.Builder().index(indexName).build());
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  /** Creates a new index using the indexName, recordType and settings provided. */
  public void createIndex(
      String indexName,
      Map<String, Object> settings,
      String mappingFile,
      String settingsFile) {
    try (final Reader mappingFileReader =
            new InputStreamReader(
                new BufferedInputStream(
                    getClass().getClassLoader().getResourceAsStream(mappingFile)));
        final Reader settingsFileReader =
            new InputStreamReader(
                new BufferedInputStream(
                    getClass().getClassLoader().getResourceAsStream(settingsFile)))) {


      IndexSettings indexSettings = IndexSettings.of(b -> b.withJson(settingsFileReader)
        .otherSettings(settings.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e  -> JsonData.of(e.getValue())))));
      CreateIndexRequest createIndexRequest = CreateIndexRequest.of(b -> b.index(indexName)
                                                                          .settings(indexSettings)
                                                                          .mappings(m -> m.withJson(mappingFileReader)));
      elasticsearchClient.indices().create(createIndexRequest);
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }

  /** Updates the settings of an existing index. */
  public void updateSettings(String indexName, Map<String, String> settings) {
    try {
      IndexSettings indexSettings = new IndexSettings.Builder().otherSettings(settings.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e  -> JsonData.of(e.getValue())))).build();
      PutIndicesSettingsRequest updateSettingsRequest = new PutIndicesSettingsRequest.Builder().index(indexName).settings(indexSettings).build();
      elasticsearchClient.indices().putSettings(updateSettingsRequest);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  /** Performs a ElasticSearch {@link BulkRequest}. */
  public BulkResponse bulk(BulkRequest bulkRequest) throws IOException {
    return elasticsearchClient.bulk(bulkRequest);
  }

  public static RestHighLevelClient provideRestHighLevelClient(EsClientConfiguration esClientConfiguration) {
    String[] hostsUrl = esClientConfiguration.hosts.split(",");
    HttpHost[] hosts = new HttpHost[hostsUrl.length];
    int i = 0;
    for (String host : hostsUrl) {
      try {
        URL url = new URL(host);
        hosts[i] = new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
        i++;
      } catch (MalformedURLException e) {
        throw new IllegalArgumentException(e.getMessage(), e);
      }
    }

    return new RestHighLevelClient(
      RestClient.builder(hosts)
        .setRequestConfigCallback(
          requestConfigBuilder ->
            requestConfigBuilder
              .setConnectTimeout(esClientConfiguration.getConnectionTimeOut())
              .setSocketTimeout(esClientConfiguration.getSocketTimeOut())
              .setConnectionRequestTimeout(
                esClientConfiguration.getConnectionRequestTimeOut()))
        .setNodeSelector(NodeSelector.SKIP_DEDICATED_MASTERS));
  }

  public static ElasticsearchClient provideEsClient(EsClientConfiguration esClientConfiguration) {
    String[] hostsUrl = esClientConfiguration.hosts.split(",");
    HttpHost[] hosts = new HttpHost[hostsUrl.length];
    int i = 0;
    for (String host : hostsUrl) {
      try {
        URL url = new URL(host);
        hosts[i] = new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
        i++;
      } catch (MalformedURLException e) {
        throw new IllegalArgumentException(e.getMessage(), e);
      }
    }

    ElasticsearchTransport transport = new RestClientTransport(
      RestClient.builder(hosts)
        .setRequestConfigCallback(
          requestConfigBuilder ->
            requestConfigBuilder
              .setConnectTimeout(esClientConfiguration.getConnectionTimeOut())
              .setSocketTimeout(esClientConfiguration.getSocketTimeOut())
              .setConnectionRequestTimeout(
                esClientConfiguration.getConnectionRequestTimeOut()))
        .setNodeSelector(NodeSelector.SKIP_DEDICATED_MASTERS).build(), new JacksonJsonpMapper());

    return new ElasticsearchClient(transport);
  }

  @Override
  public void close() {
    // shuts down the ES client

  }
}
