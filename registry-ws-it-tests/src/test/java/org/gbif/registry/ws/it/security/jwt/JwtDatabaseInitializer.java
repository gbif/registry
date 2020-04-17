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
package org.gbif.registry.ws.it.security.jwt;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.identity.service.IdentityService;

import java.util.Set;

import javax.sql.DataSource;

import org.junit.jupiter.api.extension.ExtensionContext;

import com.google.common.collect.Sets;

/** DB initialization needed for JWT tests. */
public class JwtDatabaseInitializer extends DatabaseInitializer {

  static final String ADMIN_USER = "administrator";
  static final String TEST_USER = "testuser";
  static final String GRSCICOLL_ADMIN = "grscicolladmin";

  private final IdentityService identityService;

  public JwtDatabaseInitializer(DataSource dataSource, IdentityService identityService) {
    super(dataSource);
    this.identityService = identityService;
  }

  @Override
  public void beforeEach(ExtensionContext extensionContext) throws Exception {
    // clean db
    super.beforeEach(extensionContext);

    // add users
    createUser(
        ADMIN_USER,
        Sets.newHashSet(UserRole.USER, UserRole.REGISTRY_ADMIN, UserRole.REGISTRY_EDITOR));
    createUser(TEST_USER, Sets.newHashSet(UserRole.USER));
    createUser(GRSCICOLL_ADMIN, Sets.newHashSet(UserRole.GRSCICOLL_ADMIN));
  }

  private void createUser(String username, Set<UserRole> roles) {
    GbifUser user = new GbifUser();
    user.setUserName(username);
    user.setFirstName(username);
    user.setLastName(username);
    user.setEmail(username + "@test.com");
    user.getSettings().put("language", "en");
    user.getSettings().put("country", "dk");
    user.setRoles(roles);

    // password equals to username
    identityService.create(user, username);

    Integer key = identityService.get(username).getKey();
    identityService.updateLastLogin(key);
  }
}
