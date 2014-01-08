/*
 * Copyright 2013 Global Biodiversity Information Facility (GBIF)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry;

import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.service.registry.EndpointService;
import org.gbif.registry.utils.Endpoints;

import java.util.List;

import static org.gbif.registry.LenientAssert.assertLenientEquals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EndpointTests {

  public static <T extends NetworkEntity> void testAddDelete(EndpointService service, T entity) {

    // check there are none on a newly created entity
    List<Endpoint> endpoints = service.listEndpoints(entity.getKey());
    assertNotNull("Endpoint list should be empty, not null when no endpoints exist", endpoints);
    assertTrue("Endpoint should be empty when none added", endpoints.isEmpty());

    // test additions
    service.addEndpoint(entity.getKey(), Endpoints.newInstance());
    service.addEndpoint(entity.getKey(), Endpoints.newInstance());
    endpoints = service.listEndpoints(entity.getKey());
    assertNotNull(endpoints);
    assertEquals("2 endpoints have been added", 2, endpoints.size());
    assertEquals("The endpoint should have 1 machine tag", 1, endpoints.get(0).getMachineTags().size());

    // test deletion, ensuring correct one is deleted
    service.deleteEndpoint(entity.getKey(), endpoints.get(0).getKey());
    endpoints = service.listEndpoints(entity.getKey());
    assertNotNull(endpoints);
    assertEquals("1 endpoint should remain after the deletion", 1, endpoints.size());
    Endpoint expected = Endpoints.newInstance();
    Endpoint created = endpoints.get(0);
    assertLenientEquals("Created entity does not read as expected", expected, created);
  }

}
