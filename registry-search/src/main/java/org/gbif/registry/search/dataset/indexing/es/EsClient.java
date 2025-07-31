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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.http.HttpHost;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.GetAliasRequest;
import co.elastic.clients.elasticsearch.indices.GetAliasResponse;
import co.elastic.clients.elasticsearch.indices.UpdateAliasesRequest;
import co.elastic.clients.elasticsearch.indices.UpdateSettingsRequest;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.io.CharStreams;

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
      GetAliasRequest getAliasesRequest = new GetAliasRequest.Builder()
          .name(alias)
          .build();
      GetAliasResponse getAliasesResponse = elasticsearchClient.indices().getAlias(getAliasesRequest);
      Set<String> idxsToDelete = getAliasesResponse.result().keySet();
      
      UpdateAliasesRequest.Builder updateAliasesRequestBuilder = new UpdateAliasesRequest.Builder();
      updateAliasesRequestBuilder.actions(a -> a.addAlias(alias, indexName));
      
      if (!idxsToDelete.isEmpty()) {
        for (String idx : idxsToDelete) {
          updateAliasesRequestBuilder.actions(a -> a.removeAlias(idx, alias));
        }
        elasticsearchClient.indices().updateAliases(updateAliasesRequestBuilder.build());
        
        // Delete old indices
        for (String idx : idxsToDelete) {
          DeleteIndexRequest deleteRequest = new DeleteIndexRequest.Builder()
              .index(idx)
              .build();
          elasticsearchClient.indices().delete(deleteRequest);
        }
      } else {
        elasticsearchClient.indices().updateAliases(updateAliasesRequestBuilder.build());
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
      elasticsearchClient.indices().refresh(r -> r.index(indexName));
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  /** Creates a new index using the indexName, recordType and settings provided. */
  public void createIndex(
      String indexName,
      String recordType,
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
      
      String settingsJson = CharStreams.toString(settingsFileReader);
      String mappingJson = CharStreams.toString(mappingFileReader);
      
      // Merge settings
      Map<String, JsonData> settingsMap = new java.util.HashMap<>();
      settings.forEach((k, v) -> {
        if (List.class.isAssignableFrom(v.getClass())) {
          settingsMap.put(k, JsonData.of((List<String>) v));
        } else {
          settingsMap.put(k, JsonData.of(v.toString()));
        }
      });
      
      CreateIndexRequest createIndexRequest = new CreateIndexRequest.Builder()
          .index(indexName)
          .settings(s -> s.withJson(settingsJson))
          .mappings(m -> m.withJson(mappingJson))
          .build();
      
      elasticsearchClient.indices().create(createIndexRequest);
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }

  /** Updates the settings of an existing index. */
  public void updateSettings(String indexName, Map<String, ?> settings) {
    try {
      Map<String, JsonData> settingsMap = new java.util.HashMap<>();
      settings.forEach((k, v) -> settingsMap.put(k, JsonData.of(v)));
      
      UpdateSettingsRequest updateSettingsRequest = new UpdateSettingsRequest.Builder()
          .index(indexName)
          .settings(s -> s.withJson(settingsMap))
          .build();
      
      elasticsearchClient.indices().putSettings(updateSettingsRequest);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  /** Performs a ElasticSearch {@link BulkRequest}. */
  public BulkResponse bulk(BulkRequest bulkRequest) throws IOException {
    return elasticsearchClient.bulk(bulkRequest);
  }

  /** Creates ElasticSearch client using default connection settings. */
  public static ElasticsearchClient provideElasticsearchClient(EsClientConfiguration esClientConfiguration) {
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

    RestClient restClient = RestClient.builder(hosts)
        .setRequestConfigCallback(
            requestConfigBuilder ->
                requestConfigBuilder
                    .setConnectTimeout(esClientConfiguration.getConnectionTimeOut())
                    .setSocketTimeout(esClientConfiguration.getSocketTimeOut())
                    .setConnectionRequestTimeout(
                        esClientConfiguration.getConnectionRequestTimeOut()))
        .build();

    ElasticsearchTransport transport = new RestClientTransport(restClient, new co.elastic.clients.json.jackson.JacksonJsonpMapper());
    return new ElasticsearchClient(transport);
  }

  @Override
  public void close() {
    // shuts down the ES client
    if (Objects.nonNull(elasticsearchClient)) {
      try {
        elasticsearchClient.shutdown();
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }
  }
}
