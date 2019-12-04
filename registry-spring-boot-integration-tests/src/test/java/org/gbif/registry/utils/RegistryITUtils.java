package org.gbif.registry.utils;

public class RegistryITUtils {

  public static String removeQuotes(String input) {
    return input.replace("\"", "");
  }
}
