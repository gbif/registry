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

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Base test class for Elasticsearch integration tests using testcontainers.
 * This class provides shared setup and utilities for tests that need Elasticsearch.
 *
 * Usage:
 * 1. Extend this class in your test
 * 2. Use @Autowired to inject ElasticsearchTestContainerConfiguration
 * 3. Use the provided methods to manage the Elasticsearch index
 */
@SpringJUnitConfig
public abstract class BaseElasticsearchTest {

  @Autowired
  protected ElasticsearchTestContainerConfiguration elasticsearchTestContainer;

  /**
   * Get the Elasticsearch client for direct operations.
   */
  protected ElasticsearchClient getElasticsearchClient() {
    return elasticsearchTestContainer.getElasticsearchClient();
  }

  /**
   * Get the Elasticsearch async client for asynchronous operations.
   */
  protected ElasticsearchAsyncClient getElasticsearchAsyncClient() {
    return elasticsearchTestContainer.getElasticsearchAsyncClient();
  }

  /**
   * Refresh the Elasticsearch index to make recent changes visible.
   */
  protected void refreshIndex() {
    elasticsearchTestContainer.refreshIndex();
  }

  /**
   * Recreate the Elasticsearch index (useful for test cleanup).
   */
  protected void recreateIndex() {
    try {
      elasticsearchTestContainer.recreateIndex();
    } catch (Exception e) {
      throw new RuntimeException("Failed to recreate index", e);
    }
  }

  /**
   * Check if the index exists.
   */
  protected boolean indexExists() {
    try {
      return elasticsearchTestContainer.indexExists();
    } catch (Exception e) {
      throw new RuntimeException("Failed to check if index exists", e);
    }
  }

  /**
   * Delete the Elasticsearch index.
   */
  protected void deleteIndex() {
    try {
      elasticsearchTestContainer.deleteIndex();
    } catch (Exception e) {
      throw new RuntimeException("Failed to delete index", e);
    }
  }

  /**
   * Get the server address for configuration purposes.
   */
  protected String getServerAddress() {
    return elasticsearchTestContainer.getServerAddress();
  }

  /**
   * JUnit extension for automatically refreshing the index before each test method
   * that contains 'search' in its name.
   */
  public static class ElasticsearchRefreshExtension implements BeforeEachCallback {

    private final ElasticsearchTestContainerConfiguration elasticsearchTestContainer;

    public ElasticsearchRefreshExtension(ElasticsearchTestContainerConfiguration elasticsearchTestContainer) {
      this.elasticsearchTestContainer = elasticsearchTestContainer;
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
      // Method name must contain search in it
      if (extensionContext.getRequiredTestMethod().getName().toLowerCase().contains("search")) {
        elasticsearchTestContainer.refreshIndex();
      }
    }
  }
}
