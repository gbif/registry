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
package org.gbif.registry.ws.it.collections;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.identity.service.IdentityService;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.google.common.collect.Sets;

import static org.gbif.registry.ws.it.fixtures.TestConstants.TEST_GRSCICOLL_ADMIN;
import static org.gbif.registry.ws.it.fixtures.TestConstants.TEST_PASSWORD;

/** DB initialization needed for collections tests. */
public class CollectionsDatabaseInitializer implements BeforeEachCallback {

  private final IdentityService identityService;

  public CollectionsDatabaseInitializer(IdentityService identityService) {
    this.identityService = identityService;
  }

  @Override
  public void beforeEach(ExtensionContext extensionContext) {
    GbifUser user = new GbifUser();
    user.setUserName(TEST_GRSCICOLL_ADMIN);
    user.setFirstName(TEST_GRSCICOLL_ADMIN);
    user.setLastName(TEST_GRSCICOLL_ADMIN);
    user.setEmail(TEST_GRSCICOLL_ADMIN + "@test.com");
    user.getSettings().put("language", "en");
    user.getSettings().put("country", "dk");
    user.setRoles(Sets.newHashSet(UserRole.GRSCICOLL_ADMIN));

    // password equals to username
    identityService.create(user, TEST_PASSWORD);

    Integer key = identityService.get(TEST_GRSCICOLL_ADMIN).getKey();
    identityService.updateLastLogin(key);
  }
}
