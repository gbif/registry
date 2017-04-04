package org.gbif.registry;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class TestConstants {

  public static final String APPLICATION_PROPERTIES = "registry-test.properties";

  //static appkeys used for testing
  public static final String IT_APP_KEY = "gbif.app.it";
  public static final String IT_APP_SECRET = "6a55ca16c053e269a9602c02922b30ce49c49be3a68bb2d8908b24d7c1";
  private final static Map<String, String> appKeys = new HashMap<>();
  static{
    appKeys.put(IT_APP_KEY, IT_APP_SECRET);
  }

  public static String getRegistryServerURL(int port) {
    return "http://localhost:" + port;
  }

  public static Map<String, String> getIntegrationTestAppKeys() {
    return appKeys;
  }

}
