package org.gbif.registry.ws.util;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class LegacyResourceUtilsTest {

  private String text;
  private String expectedTextAfterValidation;
  private int minLength = 10;

  @Parameterized.Parameters
  public static Iterable<Object[]> data() {
    return ImmutableList.<Object[]>of(
      new Object[] {"desc", 10, "desc [Field must be at least 10 characters long]"},
      new Object[] {"", 50, ""}, new Object[] {null, 50, null});
  }

  public LegacyResourceUtilsTest(String text, int minLength, String expectedTextAfterValidation) {
    this.text = text;
    this.minLength = minLength;
    this.expectedTextAfterValidation = expectedTextAfterValidation;
  }

  @Test
  public void testValidateField() {
    String validated = LegacyResourceUtils.validateField(text, minLength);
    assertEquals(expectedTextAfterValidation, validated);
  }
}
