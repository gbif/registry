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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Properties;

import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import lombok.SneakyThrows;

public class EsManageServer implements InitializingBean, DisposableBean {

  private ElasticsearchContainer embeddedElastic;

  private final Resource mappingFile;

  private final Resource settingsFile;

  private final String indexName;

  // needed to assert results against ES server directly
  private RestHighLevelClient restClient;

  public EsManageServer(Resource mappingFile, Resource settingsFile, String indexName) {
    this.mappingFile = mappingFile;
    this.settingsFile = settingsFile;
    this.indexName = indexName;
  }

  @Override
  public void destroy() throws Exception {
    if (embeddedElastic != null && !embeddedElastic.isShouldBeReused()) {
      embeddedElastic.stop();
    }
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
    embeddedElastic =
        new ElasticsearchContainer(
            "docker.elastic.co/elasticsearch/elasticsearch:" + getEsVersion());
    embeddedElastic.withReuse(true);
    embeddedElastic.start();
    restClient = buildRestClient();

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
    return restClient.indices().exists(new GetIndexRequest(indexName), RequestOptions.DEFAULT);
  }

  public void createIndex() throws IOException {
    if (!indexExists()) {
      CreateIndexRequest createIndexRequest = new CreateIndexRequest(indexName);
      createIndexRequest.settings(asString(settingsFile), XContentType.JSON);
      createIndexRequest.mapping(asString(mappingFile), XContentType.JSON);
      restClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
    }
  }

  public void deleteIndex() throws IOException {
    DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest();
    deleteIndexRequest.indices(indexName);
    restClient.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
  }

  public RestHighLevelClient getRestClient() {
    return restClient;
  }

  public String getServerAddress() {
    return "http://localhost:" + embeddedElastic.getMappedPort(9200);
  }

  public void refresh() {
    try {
      RefreshRequest refreshRequest = new RefreshRequest();
      refreshRequest.indices(indexName);
      restClient.indices().refresh(refreshRequest, RequestOptions.DEFAULT);
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private RestHighLevelClient buildRestClient() {
    HttpHost host = new HttpHost("localhost", embeddedElastic.getMappedPort(9200));
    return new RestHighLevelClient(RestClient.builder(host));
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
