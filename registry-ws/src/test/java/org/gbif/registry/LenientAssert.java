package org.gbif.registry;

import org.gbif.api.model.registry.LenientEquals;

import static org.junit.Assert.fail;

/**
 * Provides a lenient equals assertion, borrowing shamelessly from {@link org.junit.Assert}.
 */
public class LenientAssert {

  /**
   * Asserts that two objects are leniently equal. If they are not, an {@link AssertionError} is thrown with the given
   * message. If <code>expected</code> and <code>actual</code> are <code>null</code>,
   * they are considered equal.
   *
   * @param message the identifying message for the {@link AssertionError} (<code>null</code> okay)
   * @param expected expected value
   * @param actual actual value
   */
  public static <T, L extends LenientEquals<T>> void assertLenientEquals(String message, L expected, T actual) {
    if (!lenientlyEqualRegardingNull(expected, actual)) {
      failNotEquals(message, expected, actual);
    }
  }

  private static <T, L extends LenientEquals<T>> boolean lenientlyEqualRegardingNull(L expected, T actual) {
    if (expected == null) {
      return actual == null;
    }

    return expected.lenientEquals(actual);
  }

  static private void failNotEquals(String message, Object expected, Object actual) {
    fail(format(message, expected, actual));
  }

  private static String format(String message, Object expected, Object actual) {
    String formatted = "";
    if (message != null && !message.equals("")) {
      formatted = message + " ";
    }
    String expectedString = String.valueOf(expected);
    String actualString = String.valueOf(actual);
    if (expectedString.equals(actualString)) {
      return formatted + "expected: "
        + formatClassAndValue(expected, expectedString)
        + " but was: " + formatClassAndValue(actual, actualString);
    } else {
      return formatted + "expected:<" + expectedString + "> but was:<"
        + actualString + ">";
    }
  }

  private static String formatClassAndValue(Object value, String valueString) {
    String className = value == null ? "null" : value.getClass().getName();
    return className + "<" + valueString + ">";
  }

}
