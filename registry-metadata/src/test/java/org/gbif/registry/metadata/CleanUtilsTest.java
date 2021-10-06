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
package org.gbif.registry.metadata;

import org.gbif.api.model.registry.Citation;
import org.gbif.api.model.registry.Contact;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CleanUtilsTest {

  @Test
  public void testRemoveEmptyStrings() {
    Contact c = new Contact();
    c.setFirstName("");
    c.setLastName("  ");
    c.setCity("Berlin");
    c.setPhone(Collections.singletonList("12345"));
    c.addEmail("   ");
    c.addEmail("   my email");
    c.addEmail(null);

    CleanUtils.removeEmptyStrings(c);

    assertEquals("Berlin", c.getCity());
    assertEquals(1, c.getEmail().size());
    assertEquals("   my email", c.getEmail().get(0));
    assertNull(c.getFirstName());
    assertNull(c.getLastName());
    assertNull(c.getCountry());
  }

  @Test
  public void testRemoveEmptyStringsCitation() {
    Citation c = new Citation();
    c.setText("");
    c.setIdentifier("");
    CleanUtils.removeEmptyStrings(c);

    assertNotNull(c);
    assertNull(c.getText());
    assertNull(c.getIdentifier());
  }
}
