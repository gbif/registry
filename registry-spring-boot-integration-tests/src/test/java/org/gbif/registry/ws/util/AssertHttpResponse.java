package org.gbif.registry.ws.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

// TODO: 2019-07-03 perhaps it's redundant
/**
 * Simple assertions on HTTP codes.
 */
public class AssertHttpResponse {

  /**
   * Assert the response from a {@link ResponseEntity}.
   * @param expected
   * @param cr
   */
  public static void assertResponse(HttpStatus expected, ResponseEntity cr) {
    assertNotNull("ClientResponse is not null", cr);
    assertEquals(expected, cr.getStatusCode());
  }

}
