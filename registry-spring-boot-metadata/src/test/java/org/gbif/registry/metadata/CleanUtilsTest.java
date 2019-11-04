package org.gbif.registry.metadata;

import com.google.common.collect.Lists;
import org.gbif.api.model.registry.Citation;
import org.gbif.api.model.registry.Contact;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class CleanUtilsTest {

  @Test
  public void testRemoveEmptyStrings() throws Exception {
    Contact c = new Contact();
    c.setFirstName("");
    c.setLastName("  ");
    c.setCity("Berlin");
    c.setPhone(Lists.newArrayList("12345"));
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
  public void testRemoveEmptyStringsCitation() throws Exception {
    Citation c = new Citation();
    c.setText("");
    c.setIdentifier("");
    CleanUtils.removeEmptyStrings(c);

    assertNotNull(c);
    assertNull(c.getText());
    assertNull(c.getIdentifier());
  }
}
