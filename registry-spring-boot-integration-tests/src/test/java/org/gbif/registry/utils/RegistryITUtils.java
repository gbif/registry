package org.gbif.registry.utils;

public class RegistryITUtils {

  public static final String REGISTRY_ADMIN_USERNAME = "registry_admin";
  public static final String REGISTRY_ADMIN_PASSWORD = "welcome";
  public static final String REGISTRY_USER_USERNAME = "registry_user";
  public static final String REGISTRY_USER_PASSWORD = "welcome";

  public static String removeQuotes(String input) {
    return input.replace("\"", "");
  }
}
