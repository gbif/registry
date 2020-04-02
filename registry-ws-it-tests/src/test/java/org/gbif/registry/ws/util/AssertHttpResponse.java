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
package org.gbif.registry.ws.util;

import javax.ws.rs.core.Response;

import com.sun.jersey.api.client.ClientResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/** Simple assertions on HTTP codes. */
public class AssertHttpResponse {

  /**
   * Assert the response from a {@link ClientResponse}.
   *
   * @param expected
   * @param cr
   */
  public static void assertResponse(Response.Status expected, ClientResponse cr) {
    assertNotNull("ClientResponse is not null", cr);
    assertEquals(expected.getStatusCode(), cr.getStatus());
  }
}
