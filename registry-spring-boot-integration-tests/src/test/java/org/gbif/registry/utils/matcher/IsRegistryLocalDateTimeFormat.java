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
package org.gbif.registry.utils.matcher;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class IsRegistryLocalDateTimeFormat extends TypeSafeMatcher<String> {

  private static final DateTimeFormatter FORMATTER =
      new DateTimeFormatterBuilder()
          .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
          .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
          .toFormatter();

  @Override
  protected boolean matchesSafely(String s) {
    try {
      LocalDateTime.parse(s, FORMATTER);
      return true;
    } catch (DateTimeParseException e) {
      return false;
    }
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("Registry date format");
  }
}
