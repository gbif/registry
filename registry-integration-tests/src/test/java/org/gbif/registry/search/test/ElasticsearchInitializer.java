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

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Test callback to initialize ES index.
 * This callback applies changes only to methods that has 'search' in its name.
 */
public class ElasticsearchInitializer implements BeforeEachCallback {

  private final EsManageServer esServer;

  public ElasticsearchInitializer(EsManageServer esServer) {
    this.esServer = esServer;
  }

  @Override
  public void beforeEach(ExtensionContext extensionContext) throws Exception {
    // Method name must contain search in it
    if (extensionContext.getRequiredTestMethod().getName().toLowerCase().contains("search")) {
      esServer.reCreateIndex();
    }
  }
}
