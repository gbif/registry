package org.gbif.registry;

import org.gbif.api.model.registry.Contact;
import org.gbif.registry.utils.Contacts;

import org.junit.Test;

import static org.gbif.registry.LenientAssert.assertLenientEquals;

public class LenientAssertTest {

  @Test
  public void testLenientAssert() {
    Contact c = Contacts.newInstance();
    assertLenientEquals("Same object should be the same", c, c);
    Contact c2 = Contacts.newInstance();
    assertLenientEquals("Equivalent object should be the same", c, c2);
    c2.setKey(1);
    assertLenientEquals("Key should be ignored", c, c2);
    c2.setCity("should fail");
    try {
      assertLenientEquals("Different objects should through assertion error", c, c2);
    } catch (AssertionError e) {
      // expected
    }
  }
}
