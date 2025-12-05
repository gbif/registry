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
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test to verify that the Elasticsearch testcontainer configuration works correctly.
 */
@SpringBootTest(classes = {DatasetElasticsearchConfiguration.class})
@ActiveProfiles("test")
public class ElasticsearchTestContainerConfigurationTest {

  @Autowired
  private ElasticsearchTestContainerConfiguration elasticsearchTestContainer;

  @Autowired
  private ElasticsearchClient elasticsearchClient;

  @Test
  public void testElasticsearchContainerIsRunning() {
    assertNotNull(elasticsearchTestContainer);
    assertNotNull(elasticsearchClient);

    // Verify the container is accessible
    String serverAddress = elasticsearchTestContainer.getServerAddress();
    assertNotNull(serverAddress);
    assertTrue(serverAddress.startsWith("http://localhost:"));
  }

  @Test
  public void testElasticsearchClientCanConnect() throws Exception {
    // Test that we can perform a basic search operation
    SearchRequest searchRequest = SearchRequest.of(s -> s
        .index("dataset")
        .query(q -> q.matchAll(m -> m)));

    SearchResponse<Object> response = elasticsearchClient.search(searchRequest, Object.class);
    assertNotNull(response);
    assertNotNull(response.hits());
  }

  @Test
  public void testIndexExists() throws Exception {
    // Test that the index was created successfully
    boolean indexExists = elasticsearchTestContainer.indexExists();
    assertTrue(indexExists, "Dataset index should exist");
  }
}
