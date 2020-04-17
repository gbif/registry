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
package org.gbif.registry.ws.it;

import org.gbif.api.model.registry.LenientEquals;

import static org.junit.jupiter.api.Assertions.fail;

/** Provides a lenient equals assertion, borrowing shamelessly from {@link org.junit.Assert}. */
public class LenientAssert {

  /**
   * Asserts that two objects are leniently equal. If they are not, an {@link AssertionError} is
   * thrown with the given message. If <code>expected</code> and <code>actual</code> are <code>null
   * </code>, they are considered equal.
   *
   * @param message the identifying message for the {@link AssertionError} (<code>null</code> okay)
   * @param expected expected value
   * @param actual actual value
   */
  public static <T, L extends LenientEquals<T>> void assertLenientEquals(
      String message, L expected, T actual) {
    if (!lenientlyEqualRegardingNull(expected, actual)) {
      failNotEquals(message, expected, actual);
    }
  }

  private static <T, L extends LenientEquals<T>> boolean lenientlyEqualRegardingNull(
      L expected, T actual) {
    if (expected == null) {
      return actual == null;
    }

    return expected.lenientEquals(actual);
  }

  private static void failNotEquals(String message, Object expected, Object actual) {
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
      return formatted
          + "expected: "
          + formatClassAndValue(expected, expectedString)
          + " but was: "
          + formatClassAndValue(actual, actualString);
    } else {
      return formatted + "expected:<" + expectedString + "> but was:<" + actualString + ">";
    }
  }

  private static String formatClassAndValue(Object value, String valueString) {
    String className = value == null ? "null" : value.getClass().getName();
    return className + "<" + valueString + ">";
  }
}
