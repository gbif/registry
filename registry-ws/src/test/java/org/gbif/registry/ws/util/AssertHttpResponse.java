package org.gbif.registry.ws.util;

import javax.ws.rs.core.Response;

import com.sun.jersey.api.client.ClientResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Simple assertions on HTTP codes.
 */
public class AssertHttpResponse {

  /**
   * Assert the response from a {@link ClientResponse}.
   * @param expected
   * @param cr
   */
  public static void assertResponse(Response.Status expected, ClientResponse cr) {
    assertNotNull("ClientResponse is not null", cr);
    assertEquals(expected.getStatusCode(), cr.getStatus());
  }

}
