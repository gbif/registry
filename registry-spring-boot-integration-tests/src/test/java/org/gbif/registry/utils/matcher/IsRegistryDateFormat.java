package org.gbif.registry.utils.matcher;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class IsRegistryDateFormat extends TypeSafeMatcher<String> {

  @Override
  protected boolean matchesSafely(String s) {
    try {
      OffsetDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
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
