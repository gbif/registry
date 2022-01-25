package org.gbif.registry.ws.it.legacy;

import org.gbif.api.model.registry.Network;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.registry.ws.it.fixtures.RequestTestFixture;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.List;

import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.core.type.TypeReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Ignore("Ignore to test API version")
public class IptNetworkResourceIT extends BaseItTest {

  private final TestDataFactory testDataFactory;
  private final RequestTestFixture requestTestFixture;

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
        requestTestFixture.extractJsonResponse(
            actions, new TypeReference<List<Network>>() {});

    assertEquals(2, response.size());
    assertNotNull(response.get(0).getKey());
    assertNotNull(response.get(0).getTitle());
    assertNotNull(response.get(1).getKey());
    assertNotNull(response.get(1).getTitle());
  }
}
