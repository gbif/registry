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
package org.gbif.registry.search.test;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.RefreshRequest;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.FileCopyUtils;

import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;

/**
 * Shared Elasticsearch testcontainer configuration for integration tests.
 * This class provides a single Elasticsearch container instance that can be reused
 * across all test classes to improve performance and resource usage.
 */
@Configuration
@Scope("singleton")
public class ElasticsearchTestContainerConfiguration implements InitializingBean, DisposableBean {

  private static final String CONTAINER_LABEL = "registry_ITs_ES_container";
  private static final String CONTAINER_REUSE_LABEL = "reuse.UUID";

  // Static container instance shared across all tests
  private static ElasticsearchContainer elasticsearchContainer;
  private static ElasticsearchClient elasticsearchClient;
  private static ElasticsearchAsyncClient elasticsearchAsyncClient;

  private final String indexName;
  private final Resource mappingFile;
  private final Resource settingsFile;

  public ElasticsearchTestContainerConfiguration(
      ResourceLoader resourceLoader,
      @Value("${elasticsearch.registry.index}") String indexName,
      @Value("classpath:dataset-es-mapping.json") String mappingPath,
      @Value("classpath:dataset-es-settings.json") String settingsPath) {
    this.indexName = indexName;
    this.mappingFile = resourceLoader.getResource(mappingPath);
    this.settingsFile = resourceLoader.getResource(settingsPath);
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    initializeContainer();
    createIndexIfNotExists();
  }

  @Override
  public void destroy() throws Exception {
    // Container cleanup is handled by testcontainers lifecycle
    // The container will be reused across tests and cleaned up by Ryuk
  }

  /**
   * Initialize the Elasticsearch container if not already initialized.
   */
  private synchronized void initializeContainer() {
    if (elasticsearchContainer == null || !elasticsearchContainer.isRunning()) {
      if (elasticsearchContainer != null) {
        elasticsearchContainer.stop();
      }

      elasticsearchContainer = new ElasticsearchContainer(
          "docker.elastic.co/elasticsearch/elasticsearch:" + getElasticsearchVersion())
          .withEnv("xpack.security.enabled", "false")
          .withEnv("xpack.security.enrollment.enabled", "false");

      // Configure container for reuse
      elasticsearchContainer
          .withReuse(true)
          .withLabel(CONTAINER_REUSE_LABEL, CONTAINER_LABEL);

      // Set wait strategy - just wait for port to be available
      elasticsearchContainer.setWaitStrategy(
          Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(120)));

      // Start the container
      elasticsearchContainer.start();

      // Initialize clients
      initializeClients();
    }
  }

  /**
   * Initialize Elasticsearch clients.
   */
  private void initializeClients() {
    HttpHost host = new HttpHost("localhost", elasticsearchContainer.getMappedPort(9200));
    RestClient restClient = RestClient.builder(host).build();
    ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());

    elasticsearchClient = new ElasticsearchClient(transport);
    elasticsearchAsyncClient = new ElasticsearchAsyncClient(transport);

    // Wait for Elasticsearch to be ready
    waitForElasticsearchReady();
  }

  /**
   * Wait for Elasticsearch to be ready by making a simple HTTP request.
   */
  private void waitForElasticsearchReady() {
    int maxRetries = 30;
    int retryCount = 0;

    while (retryCount < maxRetries) {
      try {
        // Check if container is running first
        if (!elasticsearchContainer.isRunning()) {
          throw new RuntimeException("Elasticsearch container is not running");
        }

        // Try a simple HTTP request to Elasticsearch
        String url = "http://localhost:" + elasticsearchContainer.getMappedPort(9200);
        java.net.URL elasticsearchUrl = new java.net.URL(url);
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) elasticsearchUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        int responseCode = connection.getResponseCode();
        if (responseCode == 200) {
          System.out.println("Elasticsearch is ready! HTTP response: " + responseCode);
          return; // Success
        }

      } catch (Exception e) {
        retryCount++;
        System.out.println("Elasticsearch not ready yet, attempt " + retryCount + "/" + maxRetries + ": " + e.getMessage());

        if (retryCount >= maxRetries) {
          throw new RuntimeException("Elasticsearch failed to become ready after " + maxRetries + " attempts. Last error: " + e.getMessage(), e);
        }

        try {
          Thread.sleep(3000);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          throw new RuntimeException("Interrupted while waiting for Elasticsearch", ie);
        }
      }
    }
  }

  /**
   * Create the index if it doesn't exist.
   */
  private void createIndexIfNotExists() throws IOException {
    if (!indexExists()) {
      String settingsJson = asString(settingsFile);
      String mappingsJson = asString(mappingFile);

      elasticsearchClient.indices().create(CreateIndexRequest.of(c -> c
          .index(indexName)
          .settings(s -> s.withJson(new ByteArrayInputStream(settingsJson.getBytes(StandardCharsets.UTF_8))))
          .mappings(m -> m.withJson(new ByteArrayInputStream(mappingsJson.getBytes(StandardCharsets.UTF_8))))));
    }
  }

  /**
   * Check if the index exists.
   */
  public boolean indexExists() throws IOException {
    return elasticsearchClient.indices().exists(ExistsRequest.of(e -> e.index(indexName))).value();
  }

  /**
   * Delete the index.
   */
  public void deleteIndex() throws IOException {
    if (indexExists()) {
      elasticsearchClient.indices().delete(DeleteIndexRequest.of(d -> d.index(indexName)));
    }
  }

  /**
   * Recreate the index (delete and create).
   */
  public void recreateIndex() throws IOException {
    deleteIndex();
    createIndexIfNotExists();
  }

  /**
   * Refresh the index.
   */
  public void refreshIndex() {
    try {
      elasticsearchClient.indices().refresh(RefreshRequest.of(r -> r.index(indexName)));
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to refresh index", ex);
    }
  }

  /**
   * Get the Elasticsearch client.
   */
  public ElasticsearchClient getElasticsearchClient() {
    return elasticsearchClient;
  }

  /**
   * Get the Elasticsearch async client.
   */
  public ElasticsearchAsyncClient getElasticsearchAsyncClient() {
    return elasticsearchAsyncClient;
  }

  /**
   * Get the server address for configuration.
   */
  public String getServerAddress() {
    return "http://localhost:" + elasticsearchContainer.getMappedPort(9200);
  }

  /**
   * Get the HTTP host address.
   */
  public String getHttpHostAddress() {
    return elasticsearchContainer.getHttpHostAddress();
  }

  /**
   * Convert resource to string.
   */
  @SneakyThrows
  private String asString(Resource resource) {
    try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
      return FileCopyUtils.copyToString(reader);
    }
  }

  /**
   * Get Elasticsearch version from maven properties.
   */
  @SneakyThrows
  private String getElasticsearchVersion() {
    Properties properties = new Properties();
    properties.load(getClass().getClassLoader().getResourceAsStream("maven.properties"));
    return properties.getProperty("elasticsearch.version");
  }

}
