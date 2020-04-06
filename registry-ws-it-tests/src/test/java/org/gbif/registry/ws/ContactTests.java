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
package org.gbif.registry.ws;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.service.registry.ContactService;
import org.gbif.api.service.registry.NetworkEntityService;
import org.gbif.registry.test.TestDataFactory;

import java.util.List;
import java.util.UUID;

import static org.gbif.registry.ws.LenientAssert.assertLenientEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ContactTests {

  public static <T extends NetworkEntity> void testAddDeleteUpdate(
      ContactService service, T entity, TestDataFactory testDataFactory) {

    // check there are none on a newly created entity
    List<Contact> contacts = service.listContacts(entity.getKey());
    assertNotNull("Contact list should be empty, not null when no contacts exist", contacts);
    assertTrue("Contact should be empty when none added", contacts.isEmpty());

    // test additions, both being primary
    Integer key1 = service.addContact(entity.getKey(), testDataFactory.newContact());
    Integer key2 = service.addContact(entity.getKey(), testDataFactory.newContact());

    // ordered by ascending created date (first contact appears first)
    contacts = service.listContacts(entity.getKey());
    assertNotNull(contacts);
    assertEquals("2 contacts have been added", 2, contacts.size());

    assertEquals(key1, contacts.get(0).getKey());
    assertEquals(key2, contacts.get(1).getKey());

    assertFalse(
        "Older contact (added first) should not be primary anymore", contacts.get(0).isPrimary());
    assertTrue("Newer contact (added second) should now be primary", contacts.get(1).isPrimary());

    // test deletion, ensuring non-primary contact is deleted, and primary contact remains
    service.deleteContact(entity.getKey(), contacts.get(0).getKey());
    contacts = service.listContacts(entity.getKey());
    assertNotNull(contacts);
    assertEquals("1 primary contact should remain after the deletion", 1, contacts.size());
    Contact expected = testDataFactory.newContact();
    Contact created = contacts.get(0);
    assertLenientEquals("Created contact does not read as expected", expected, created);

    // try and update a contact
    contacts = service.listContacts(entity.getKey());
    contacts.get(0).setFirstName("Timmay");
    service.updateContact(entity.getKey(), contacts.get(0));
    contacts = service.listContacts(entity.getKey());
    assertNotNull(contacts);
    assertEquals(
        "The update does not reflect the change", "Timmay", contacts.get(0).getFirstName());

    try {
      service.updateContact(UUID.randomUUID(), contacts.get(0));
      fail("Contact update supplied an illegal entity key but was not caught");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  /** Tests that adding a contact means the entity is found in the search. */
  public static <T extends NetworkEntity> void testSimpleSearch(
      ContactService service,
      NetworkEntityService<T> networkService,
      T entity,
      TestDataFactory testDataFactory) {
    assertEquals(
        "There should be no results for this search",
        Long.valueOf(0),
        networkService.search("Frankie", null).getCount());
    Contact c = testDataFactory.newContact();
    c.setLastName("Frankie");
    service.addContact(entity.getKey(), c);
    assertEquals(
        "There should a search result for Frankie",
        Long.valueOf(1),
        networkService.search("Frankie", null).getCount());
  }
}
