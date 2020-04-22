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

import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.service.registry.EndpointService;
import org.gbif.registry.test.TestDataFactory;

import java.util.List;

import static org.gbif.registry.ws.it.LenientAssert.assertLenientEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EndpointTests {

  public static <T extends NetworkEntity> void testAddDelete(
      EndpointService service, T entity, TestDataFactory testDataFactory) {

    // check there are none on a newly created entity
    List<Endpoint> endpoints = service.listEndpoints(entity.getKey());
    assertNotNull(endpoints, "Endpoint list should be empty, not null when no endpoints exist");
    assertTrue(endpoints.isEmpty(), "Endpoint should be empty when none added");

    // test additions
    service.addEndpoint(entity.getKey(), testDataFactory.newEndpoint());
    service.addEndpoint(entity.getKey(), testDataFactory.newEndpoint());
    endpoints = service.listEndpoints(entity.getKey());
    assertNotNull(endpoints);
    assertEquals(2, endpoints.size(), "2 endpoints have been added");
    assertEquals(
        1, endpoints.get(0).getMachineTags().size(), "The endpoint should have 1 machine tag");

    // test deletion, ensuring correct one is deleted
    service.deleteEndpoint(entity.getKey(), endpoints.get(0).getKey());
    endpoints = service.listEndpoints(entity.getKey());
    assertNotNull(endpoints);
    assertEquals(1, endpoints.size(), "1 endpoint should remain after the deletion");
    Endpoint expected = testDataFactory.newEndpoint();
    Endpoint created = endpoints.get(0);
    assertLenientEquals("Created entity does not read as expected", expected, created);
  }
}
