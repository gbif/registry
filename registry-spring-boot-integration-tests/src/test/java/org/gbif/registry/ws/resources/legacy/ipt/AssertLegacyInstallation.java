package org.gbif.registry.ws.resources.legacy.ipt;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.EndpointType;
import org.gbif.registry.ws.model.LegacyInstallation;

import static org.gbif.registry.utils.LegacyInstallations.IPT_PRIMARY_CONTACT_EMAIL;
import static org.gbif.registry.utils.LegacyInstallations.IPT_PRIMARY_CONTACT_NAME;
import static org.gbif.registry.utils.LegacyInstallations.IPT_SERVICE_URL;
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

    // check installation's primary contact was properly persisted
    Contact contact = actual.getContacts().get(0);
    assertNotNull("Installation primary contact should be present", contact);
    assertNotNull(contact.getKey());
    assertTrue(contact.isPrimary());
    assertEquals(IPT_PRIMARY_CONTACT_NAME, contact.getFirstName());
    assertEquals(IPT_PRIMARY_CONTACT_EMAIL, contact.getEmail());
    assertEquals(ContactType.TECHNICAL_POINT_OF_CONTACT, contact.getType());
    assertNotNull(contact.getCreated());
    assertNotNull(contact.getCreatedBy());
    assertNotNull(contact.getModified());
    assertNotNull(contact.getModifiedBy());

    // check installation's RSS/FEED endpoint was properly persisted
    Endpoint endpoint = actual.getEndpoints().get(0);
    assertNotNull("Installation FEED endpoint should be present", endpoint);
    assertNotNull(endpoint.getKey());
    assertEquals(IPT_SERVICE_URL, endpoint.getUrl());
    assertEquals(EndpointType.FEED, endpoint.getType());
    assertNotNull(endpoint.getCreated());
    assertNotNull(endpoint.getCreatedBy());
    assertNotNull(endpoint.getModified());
    assertNotNull(endpoint.getModifiedBy());
  }
}
