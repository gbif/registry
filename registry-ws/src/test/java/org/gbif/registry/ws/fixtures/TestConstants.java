package org.gbif.registry.ws.fixtures;

import org.gbif.api.vocabulary.UserRole;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Maps;

/**
 * Constants related to unit and integration testing of the registry.
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

  public static final String TEST_ADMIN = "admin";
  public static final String TEST_EDITOR = "editor";
  public static final String TEST_USER = "user";


  public static final Map<String, UserRole> TEST_USERS_ROLE = Maps.newHashMap();
  static{
    TEST_USERS_ROLE.put(TEST_ADMIN, UserRole.REGISTRY_ADMIN);
    TEST_USERS_ROLE.put(TEST_EDITOR, UserRole.REGISTRY_EDITOR );
    TEST_USERS_ROLE.put(TEST_USER, UserRole.USER);
  }

  public static String getRegistryServerURL(int port) {
    return "http://localhost:" + port;
  }

  public static Map<String, String> getIntegrationTestAppKeys() {
    return appKeys;
  }

  /**
   * Return the {@link UserRole} of a test user as defined by {@link #TEST_USERS_ROLE}.
   * @param testUsername
   * @return
   */
  public static UserRole getTestUserRole(String testUsername) {
    return TEST_USERS_ROLE.get(testUsername);
  }

}
