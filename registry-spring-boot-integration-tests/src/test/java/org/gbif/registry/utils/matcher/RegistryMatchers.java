package org.gbif.registry.utils.matcher;

import org.hamcrest.Matcher;

public class RegistryMatchers {

  public static Matcher<String> isDoi() {
    return new IsDoi();
  }

  public static Matcher<String> isDownloadDoi() {
    return new IsDownloadDoi();
  }

  public static Matcher<String> isRegistryDateFormat() {
    return new IsRegistryDateFormat();
  }
}
