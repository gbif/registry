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
package org.gbif.registry.ws.it;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.server.LocalServerPort;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Simple test to make sure we can produce the Enumeration response. We use a simple Jersey Client
 * since it's not available in the Java client.
 */
public class EnumerationResourceIT {

  @LocalServerPort private int localServerPort;

  private Client publicClient;

  @BeforeEach
  public void setupBase() {
    publicClient = buildPublicClient();
  }

  @AfterEach
  public void destroyBase() {
    publicClient.destroy();
  }

  private Client buildPublicClient() {
    ClientConfig clientConfig = new DefaultClientConfig();
    clientConfig.getFeatures().put("com.sun.jersey.api.json.POJOMappingFeature", Boolean.TRUE);
    return Client.create(clientConfig);
  }

  @Test
  public void testTermEnumeration() {
    ClientResponse res =
        publicClient
            .resource("http://localhost:" + localServerPort)
            .path("enumeration/basic")
            .get(ClientResponse.class);

    List<String> responseContent = res.getEntity(new GenericType<List<String>>() {});
    assertNotNull(responseContent);
    assertTrue(responseContent.size() > 0);
  }
}
