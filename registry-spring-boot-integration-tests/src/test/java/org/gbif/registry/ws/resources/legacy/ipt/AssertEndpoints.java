package org.gbif.registry.ws.resources.legacy.ipt;

import org.gbif.api.model.registry.Endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AssertEndpoints {

  public static void assertEndpoint(Endpoint expected, Endpoint actual) {
    assertNotNull("Installation FEED endpoint should be present", actual);
    assertNotNull(actual.getKey());
    assertEquals(expected.getUrl(), actual.getUrl());
    assertEquals(expected.getType(), actual.getType());
    assertNotNull(actual.getCreated());
    assertNotNull(actual.getCreatedBy());
    assertNotNull(actual.getModified());
    assertNotNull(actual.getModifiedBy());
  }
}
