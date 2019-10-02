package org.gbif.registry.doi.util;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RegistryDoiUtilsTest {

  @Test
  public void testGetYear() {
    // given
    Date date = new Date(1418340702253L);

    // when
    String actualNonNull = RegistryDoiUtils.getYear(date);
    String actualNull = RegistryDoiUtils.getYear(null);

    // then
    assertEquals("2014", actualNonNull);
    assertNull(actualNull);
  }

  @Test
  public void testFdate() {
    // given
    Date date = new Date(1418340702253L);

    // when
    String actual = RegistryDoiUtils.fdate(date);

    // then
    assertEquals("2014-12-12", actual);
  }
}
