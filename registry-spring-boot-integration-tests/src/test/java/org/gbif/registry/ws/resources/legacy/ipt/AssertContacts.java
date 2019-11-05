package org.gbif.registry.ws.resources.legacy.ipt;

import org.gbif.api.model.registry.Contact;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AssertContacts {

  public static void assertContact(Contact expected, Contact actual) {
    assertNotNull("Installation primary contact should be present", actual);
    assertNotNull(actual.getKey());
    assertEquals(expected.getFirstName(), actual.getFirstName());
    assertEquals(expected.getEmail(), actual.getEmail());
    assertEquals(expected.getType(), actual.getType());
    assertNotNull(actual.getCreated());
    assertNotNull(actual.getCreatedBy());
    assertNotNull(actual.getModified());
    assertNotNull(actual.getModifiedBy());
  }
}
