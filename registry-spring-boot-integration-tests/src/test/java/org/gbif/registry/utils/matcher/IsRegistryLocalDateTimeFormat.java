package org.gbif.registry.utils.matcher;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;

public class IsRegistryLocalDateTimeFormat extends TypeSafeMatcher<String> {

  private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
    .appendPattern("yyyy-MM-dd HH:mm:ss")
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
