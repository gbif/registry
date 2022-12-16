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
package org.gbif.registry.ws.it.legacy;

import org.gbif.api.model.registry.Network;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.registry.ws.it.fixtures.RequestTestFixture;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.core.type.TypeReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class IptNetworkResourceIT extends BaseItTest {

  private final TestDataFactory testDataFactory;
  private final RequestTestFixture requestTestFixture;

  private TestCaseDatabaseInitializer databaseInitializer = new TestCaseDatabaseInitializer();

  @Autowired
  public IptNetworkResourceIT(
      TestDataFactory testDataFactory,
      RequestTestFixture requestTestFixture,
      SimplePrincipalProvider principalProvider,
      EsManageServer esServer) {
    super(principalProvider, esServer);
    this.testDataFactory = testDataFactory;
    this.requestTestFixture = requestTestFixture;
  }

  /**
   * The test sends a get all networks (GET) request, the JSON response having a key and name
   * for each network in the list.
   */
  @Test
  public void testGetNetworksJSON() throws Exception {
    // persist two new networks
    Network network1 = testDataFactory.newPersistedNetwork();
    assertNotNull(network1.getKey());
    Network network2 = testDataFactory.newPersistedNetwork();
    assertNotNull(network2.getKey());

    // construct request uri
    String uri = "/registry/network.json";

    // send GET request with no credentials
    ResultActions actions =
        requestTestFixture.getRequest(uri).andExpect(status().is2xxSuccessful());

    // JSON array expected, with single entry
    List<Network> response =
        requestTestFixture.extractJsonResponse(actions, new TypeReference<List<Network>>() {});

    assertEquals(2, response.size());
    assertNotNull(response.get(0).getKey());
    assertNull(response.get(0).getTitle());
    assertNotNull(response.get(1).getKey());
    assertNull(response.get(1).getTitle());
  }
}
