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
package org.gbif.registry.search.test;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;

import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;
import pl.allegro.tech.embeddedelasticsearch.PopularProperties;

public class EsManageServer implements InitializingBean, DisposableBean {

  private static final String CLUSTER_NAME = "EsITCluster";

  private EmbeddedElastic embeddedElastic;

  private final Resource mappingFile;

  private final String indexName;

  private final String typeName;

  // needed to assert results against ES server directly
  private RestHighLevelClient restClient;

  public EsManageServer(Resource mappingFile, String indexName, String typeName) {
    this.mappingFile = mappingFile;
    this.indexName = indexName;
    this.typeName = typeName;
  }

  @Override
  public void destroy() throws Exception {
    if (embeddedElastic != null) {
      embeddedElastic.stop();
    }
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    start();
  }

  public void start() throws Exception {
    embeddedElastic =
        EmbeddedElastic.builder()
            .withElasticVersion(getEsVersion())
            .withEsJavaOpts("-Xms128m -Xmx512m")
            .withSetting(PopularProperties.HTTP_PORT, getAvailablePort())
            .withSetting(PopularProperties.TRANSPORT_TCP_PORT, getAvailablePort())
            .withSetting(PopularProperties.CLUSTER_NAME, CLUSTER_NAME)
            .withStartTimeout(120, TimeUnit.SECONDS)
            .build();

    embeddedElastic.start();
    restClient = buildRestClient();
    createIndex();
  }

  public RestHighLevelClient getRestClient() {
    return restClient;
  }

  public String getServerAddress() {
    return "http://localhost:" + embeddedElastic.getHttpPort();
  }

  public void refresh() {
    try {
      restClient.indices().refresh(new RefreshRequest().indices(indexName), RequestOptions.DEFAULT);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  /** Utility method to create an index. */
  private void createIndex() throws IOException {
    String mapping = IOUtils.toString(mappingFile.getInputStream(), StandardCharsets.UTF_8);
    restClient
        .indices()
        .create(
            new CreateIndexRequest().index(indexName).mapping(typeName, mapping, XContentType.JSON),
            RequestOptions.DEFAULT);
  }

  private RestHighLevelClient buildRestClient() {
    HttpHost host = new HttpHost("localhost", embeddedElastic.getHttpPort());
    return new RestHighLevelClient(RestClient.builder(host));
  }

  private static int getAvailablePort() throws IOException {
    ServerSocket serverSocket = new ServerSocket(0);
    int port = serverSocket.getLocalPort();
    serverSocket.close();

    return port;
  }

  private String getEsVersion() throws IOException {
    Properties properties = new Properties();
    properties.load(this.getClass().getClassLoader().getResourceAsStream("maven.properties"));
    return properties.getProperty("elasticsearch.version");
  }

  public void reCreateIndex() {
    try {
      restClient
          .indices()
          .delete(new DeleteIndexRequest().indices(indexName), RequestOptions.DEFAULT);
      createIndex();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}
