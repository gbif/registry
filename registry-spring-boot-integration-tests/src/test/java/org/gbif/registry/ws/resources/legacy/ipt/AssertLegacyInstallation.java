package org.gbif.registry.ws.resources.legacy.ipt;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Installation;
import org.gbif.registry.ws.model.LegacyInstallation;

import static org.gbif.registry.ws.resources.legacy.ipt.AssertContacts.assertContact;
import static org.gbif.registry.ws.resources.legacy.ipt.AssertEndpoints.assertEndpoint;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AssertLegacyInstallation {

  public static void assertLegacyInstallations(LegacyInstallation expected, Installation actual) {

    assertNotNull("Installation should be present", actual);
    assertEquals(expected.getOrganizationKey(), actual.getOrganizationKey());
    assertEquals(expected.getType(), actual.getType());
    assertEquals(expected.getTitle(), actual.getTitle());
    assertEquals(expected.getDescription(), actual.getDescription());
    assertNotNull(actual.getCreated());
    assertNotNull(actual.getModified());

    for (int i = 0; i < expected.getContacts().size(); i++) {
      // check installation's primary contact was properly persisted
      Contact actualContact = actual.getContacts().get(i);
      Contact expectedContact = expected.getContacts().get(i);

      assertContact(expectedContact, actualContact);
      assertTrue(actualContact.isPrimary());
    }

    for (int i = 0; i < expected.getEndpoints().size(); i++) {
      // check installation's RSS/FEED endpoint was properly persisted
      Endpoint actualEndpoint = actual.getEndpoints().get(i);
      Endpoint expectedEndpoint = expected.getEndpoints().get(i);

      assertEndpoint(expectedEndpoint, actualEndpoint);
    }
  }
}
