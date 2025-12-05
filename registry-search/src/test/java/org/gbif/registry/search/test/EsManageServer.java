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

import co.elastic.clients.elasticsearch.indices.RefreshRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.Properties;

import org.apache.http.HttpHost;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import lombok.SneakyThrows;

import org.testcontainers.utility.DockerImageName;

public class EsManageServer implements InitializingBean, DisposableBean {

  public static final ElasticsearchContainer embeddedElastic;

  static {
    embeddedElastic =
      new ElasticsearchContainer(DockerImageName.parse(
        "docker.elastic.co/elasticsearch/elasticsearch").withTag(getEsVersion()));
    embeddedElastic.withReuse(true).withLabel("reuse.UUID", "registry_ITs_ES_container");
    embeddedElastic.setWaitStrategy(
      Wait.defaultWaitStrategy().withStartupTimeout(Duration.ofSeconds(60)));
    embeddedElastic.start();
    elasticsearchClient = buildElasticsearchClient();
  }

  private final Resource mappingFile;

  private final Resource settingsFile;

  private final String indexName;

  // needed to assert results against ES server directly
  private static ElasticsearchClient elasticsearchClient;

  public EsManageServer(Resource mappingFile, Resource settingsFile, String indexName) {
    this.mappingFile = mappingFile;
    this.settingsFile = settingsFile;
    this.indexName = indexName;
  }

  @Override
  public void destroy() throws Exception {
    //    if (embeddedElastic != null && !embeddedElastic.isShouldBeReused()) {
    //      embeddedElastic.stop();
    //    }
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    start();
  }

  public String getHttpHostAddress() {
    return embeddedElastic.getHttpHostAddress();
  }

  public InetSocketAddress getTcpPort() {
    return embeddedElastic.getTcpHost();
  }

  @SneakyThrows
  private static String getEsVersion() {
    Properties properties = new Properties();
    properties.load(EsManageServer.class.getClassLoader().getResourceAsStream("maven.properties"));
    return properties.getProperty("elasticsearch.version");
  }

  public void start() throws Exception {
    //    embeddedElastic =
    //        new ElasticsearchContainer(
    //            "docker.elastic.co/elasticsearch/elasticsearch:" + getEsVersion());
    //    embeddedElastic.withReuse(true).withLabel("reuse.UUID", "registry_ITs_ES_container");
    //    embeddedElastic.setWaitStrategy(
    //        Wait.defaultWaitStrategy().withStartupTimeout(Duration.ofSeconds(60)));
    //    embeddedElastic.start();
    //    restClient = buildRestClient();

    createIndex();
  }

  private static String asString(Resource resource) {
    try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
      return FileCopyUtils.copyToString(reader);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean indexExists() throws IOException {
    return elasticsearchClient.indices().exists(ExistsRequest.of(e -> e.index(indexName))).value();
  }

  public void createIndex() throws IOException {
    if (!indexExists()) {
      String settingsJson = asString(settingsFile);
      String mappingsJson = asString(mappingFile);

      elasticsearchClient.indices().create(CreateIndexRequest.of(c -> c
          .index(indexName)
          .settings(s -> s.withJson(new ByteArrayInputStream(settingsJson.getBytes(StandardCharsets.UTF_8))))
          .mappings(m -> m.withJson(new ByteArrayInputStream(mappingsJson.getBytes(StandardCharsets.UTF_8))))));
    }
  }

  public void deleteIndex() throws IOException {
    elasticsearchClient.indices().delete(DeleteIndexRequest.of(d -> d.index(indexName)));
  }

  public ElasticsearchClient getElasticsearchClient() {
    return elasticsearchClient;
  }

  public String getServerAddress() {
    return "http://localhost:" + embeddedElastic.getMappedPort(9200);
  }

  public void refresh() {
    try {
      elasticsearchClient.indices().refresh(RefreshRequest.of(r -> r.index(indexName)));
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static ElasticsearchClient buildElasticsearchClient() {
    HttpHost host = new HttpHost("localhost", embeddedElastic.getMappedPort(9200));
    RestClient restClient = RestClient.builder(host).build();
    ElasticsearchTransport transport = new RestClientTransport(restClient, new co.elastic.clients.json.jackson.JacksonJsonpMapper());
    return new ElasticsearchClient(transport);
  }

  private static Optional<Integer> getEnvIntVariable(String name) {
    return Optional.ofNullable(System.getenv(name)).map(Integer::new);
  }

  private static int getAvailablePort() throws IOException {
    ServerSocket serverSocket = new ServerSocket(0);
    int port = serverSocket.getLocalPort();
    serverSocket.close();

    return port;
  }

  public void reCreateIndex() {
    try {
      deleteIndex();
      createIndex();
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }
}
