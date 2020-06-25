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
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.NodeSelector;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
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
    private int maxRetryTimeOut;
  }

  private final RestHighLevelClient restHighLevelClient;

  @Autowired
  public EsClient(RestHighLevelClient restHighLevelClient) {
    this.restHighLevelClient = restHighLevelClient;
  }

  /**
   * Points the indexName to the alias, and deletes all the indices that were pointing to the alias.
   */
  public void swapAlias(String alias, String indexName) {
    try {
      GetAliasesRequest getAliasesRequest = new GetAliasesRequest();
      getAliasesRequest.aliases(alias);
      GetAliasesResponse getAliasesResponse =
          restHighLevelClient.indices().getAlias(getAliasesRequest, RequestOptions.DEFAULT);
      Set<String> idxsToDelete = getAliasesResponse.getAliases().keySet();
      IndicesAliasesRequest indicesAliasesRequest = new IndicesAliasesRequest();
      indicesAliasesRequest.addAliasAction(
          IndicesAliasesRequest.AliasActions.add().alias(alias).index(indexName));
      restHighLevelClient.indices().updateAliases(indicesAliasesRequest, RequestOptions.DEFAULT);
      if (!idxsToDelete.isEmpty()) {
        idxsToDelete.forEach(
            idx ->
                indicesAliasesRequest.addAliasAction(
                    IndicesAliasesRequest.AliasActions.remove().index(idx).alias(alias)));
        restHighLevelClient
            .indices()
            .delete(
                new DeleteIndexRequest().indices(idxsToDelete.toArray(new String[0])),
                RequestOptions.DEFAULT);
      }
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
      Settings.Builder settingsBuilder = Settings.builder();
      settings.forEach(
          (k, v) -> {
            if (List.class.isAssignableFrom(v.getClass())) {
              settingsBuilder.putList(k, (List<String>) v);
            } else {
              settingsBuilder.put(k, v.toString());
            }
          });
      settingsBuilder.loadFromSource(CharStreams.toString(settingsFileReader), XContentType.JSON);

      CreateIndexRequest createIndexRequest = new CreateIndexRequest();
      createIndexRequest
          .index(indexName)
          .settings(settingsBuilder.build())
          .mapping(recordType, CharStreams.toString(mappingFileReader), XContentType.JSON);
      restHighLevelClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }

  /** Updates the settings of an existing index. */
  public void updateSettings(String indexName, Map<?, ?> settings) {
    try {
      UpdateSettingsRequest updateSettingsRequest = new UpdateSettingsRequest();
      updateSettingsRequest.indices(indexName).settings(settings);
      restHighLevelClient.indices().putSettings(updateSettingsRequest, RequestOptions.DEFAULT);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  /** Performs a ElasticSearch {@link BulkRequest}. */
  public BulkResponse bulk(BulkRequest bulkRequest) throws IOException {
    return restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
  }

  /** Creates ElasticSearch client using default connection settings. */
  public static RestHighLevelClient provideEsClient(EsClientConfiguration esClientConfiguration) {
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
            .setMaxRetryTimeoutMillis(esClientConfiguration.getMaxRetryTimeOut())
            .setNodeSelector(NodeSelector.SKIP_DEDICATED_MASTERS));
  }

  @Override
  public void close() {
    // shuts down the ES client
    if (Objects.nonNull(restHighLevelClient)) {
      try {
        restHighLevelClient.close();
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }
  }
}
