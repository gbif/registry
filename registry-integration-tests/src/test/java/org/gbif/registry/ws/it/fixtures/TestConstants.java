/*
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

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.vocabulary.UserRole;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/** Constants related to unit and integration testing of the registry. */
public class TestConstants {

  public static final BiFunction<Integer, Long, Pageable> PAGE =
      (limit, offset) ->
          new Pageable() {
            @Override
            public int getLimit() {
              return limit;
            }

            @Override
            public long getOffset() {
              return offset;
            }
          };

  public static final String LIQUIBASE_MASTER_FILE = "liquibase/master.xml";

  // static appkeys used for testing
  public static final String IT_APP_KEY = "gbif.app.it";
  public static final String IT_APP_KEY2 = "gbif.app.it2";

  public static final String WS_TEST = "WS TEST";
  public static final String TEST_ADMIN = "admin";
  public static final String TEST_EDITOR = "editor";
  public static final String TEST_USER = "user";
  public static final String TEST_GRSCICOLL_ADMIN = "grscicoll_admin";
  public static final String TEST_PASSWORD = "password";

  public static final DOI TEST_DOI = new DOI("10.21373/abcde");
  public static final String DATASET_NAME = "Test Dataset Registry2 Sjælland";
  public static final String DOI = "http://dx.doi.org/10.1234/timbo";
  public static final String DATASET_DESCRIPTION = "Description of Test Dataset";
  public static final String DATASET_HOMEPAGE_URL = "http://www.homepage.com";
  public static final String DATASET_LOGO_URL = "http://www.logo.com/1";
  public static final String DATASET_PRIMARY_CONTACT_TYPE = "administrative";
  public static final String DATASET_PRIMARY_CONTACT_NAME = "Jan Legind";
  public static final List<String> DATASET_PRIMARY_CONTACT_EMAIL =
      Collections.singletonList("elyk-kaarb@euskadi.eus");
  public static final List<String> DATASET_PRIMARY_CONTACT_PHONE =
      Collections.singletonList("90909090");
  public static final List<String> DATASET_PRIMARY_CONTACT_ADDRESS =
      Collections.singletonList("Universitetsparken 15, 2100, Denmark");
  // GBRDS Datasets only
  public static final String DATASET_PRIMARY_CONTACT_DESCRIPTION = "Data manager";
  public static final String DATASET_NAME_LANGUAGE = "fr";
  public static final String DATASET_DESCRIPTION_LANGUAGE = "es";

  public static final Map<String, UserRole> TEST_USERS_ROLE = new HashMap<>();

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
