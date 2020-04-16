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
package org.gbif.registry.ws.it.fixtures;

import org.gbif.api.vocabulary.UserRole;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Maps;

/** Constants related to unit and integration testing of the registry. */
public class TestConstants {

  public static final String APPLICATION_PROPERTIES = "registry-test.properties";

  // static appkeys used for testing
  public static final String IT_APP_KEY = "gbif.app.it";
  public static final String IT_APP_SECRET =
      "6a55ca16c053e269a9602c02922b30ce49c49be3a68bb2d8908b24d7c1";

  public static final String IT_APP_KEY2 = "gbif.app.it2";
  public static final String IT_APP_SECRET2 =
      "6a55ca16c053e269a9602c02922b30ce49c49be3a68bb2d8908b24d7c2";

  private static final Map<String, String> APP_KEYS = new HashMap<>();

  static {
    APP_KEYS.put(IT_APP_KEY, IT_APP_SECRET);
    APP_KEYS.put(IT_APP_KEY2, IT_APP_SECRET2);
  }

  public static final String TEST_ADMIN = "admin";
  public static final String TEST_EDITOR = "editor";
  public static final String TEST_USER = "user";
  public static final String TEST_GRSCICOLL_ADMIN = "grscicollAdmin";

  public static final Map<String, UserRole> TEST_USERS_ROLE = Maps.newHashMap();

  static {
    TEST_USERS_ROLE.put(TEST_ADMIN, UserRole.REGISTRY_ADMIN);
    TEST_USERS_ROLE.put(TEST_EDITOR, UserRole.REGISTRY_EDITOR);
    TEST_USERS_ROLE.put(TEST_USER, UserRole.USER);
    TEST_USERS_ROLE.put(TEST_GRSCICOLL_ADMIN, UserRole.GRSCICOLL_ADMIN);
  }

  /** Return the {@link UserRole} of a test user as defined by {@link #TEST_USERS_ROLE}. */
  public static UserRole getTestUserRole(String testUsername) {
    return TEST_USERS_ROLE.get(testUsername);
  }
}
