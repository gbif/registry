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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;
import pl.allegro.tech.embeddedelasticsearch.PopularProperties;

/**
 * ES server used for testing purposes.
 *
 * <p>This class is intended to be used as a {@link org.junit.ClassRule}.
 */
public class EsServer implements BeforeAllCallback, AfterAllCallback {

  private static final Logger LOG = LoggerFactory.getLogger(EsServer.class);

  private static final String CLUSTER_NAME = "EsITCluster";

  private EmbeddedElastic embeddedElastic;

  // needed to assert results against ES server directly
  private RestHighLevelClient restClient;

  private final Path mappingFile;

  private final String indexName;

  private final String typeName;

  public EsServer(Path mappingFile, String indexName, String typeName) {
    this.mappingFile = mappingFile;
    this.indexName = indexName;
    this.typeName = typeName;
  }

  @Override
  public void beforeAll(ExtensionContext extensionContext) throws Exception {
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

  @Override
  public void afterAll(ExtensionContext extensionContext) throws Exception {
    if (embeddedElastic != null) {
      embeddedElastic.stop();
    }
    if (restClient != null) {
      try {
        restClient.close();
      } catch (IOException e) {
        LOG.error("Could not close rest client for testing", e);
        throw e;
      }
    }
  }

  /** Utility method to create an index. */
  private void createIndex() throws IOException {
    String mapping = new String(Files.readAllBytes(mappingFile));
    restClient
        .indices()
        .create(
            new CreateIndexRequest().index(indexName).mapping(typeName, mapping, XContentType.JSON),
            RequestOptions.DEFAULT);
  }

  private static int getAvailablePort() throws IOException {
    ServerSocket serverSocket = new ServerSocket(0);
    int port = serverSocket.getLocalPort();
    serverSocket.close();

    return port;
  }

  public String getServerAddress() {
    return "http://localhost:" + embeddedElastic.getHttpPort();
  }

  private RestHighLevelClient buildRestClient() {
    HttpHost host = new HttpHost("localhost", embeddedElastic.getHttpPort());
    return new RestHighLevelClient(RestClient.builder(host));
  }

  private String getEsVersion() throws IOException {
    Properties properties = new Properties();
    properties.load(this.getClass().getClassLoader().getResourceAsStream("maven.properties"));
    return properties.getProperty("elasticsearch.version");
  }

  RestHighLevelClient getRestClient() {
    return restClient;
  }
}
