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
package org.gbif.registry.ws.resources;

import org.gbif.registry.domain.ws.UserAdminView;
import org.gbif.registry.identity.service.IdentityService;
import org.gbif.ws.security.AppkeysConfigurationProperties;
import org.gbif.ws.security.GbifAuthentication;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class UserManagementResourceTest {

  @Mock private GbifAuthentication mockAuth;
  @Mock private IdentityService mockIdentityService;
  @Mock private AppkeysConfigurationProperties appkeysConfiguration;
  @InjectMocks private UserManagementResource resource;

  @Test
  public void testGetUserWhenUserNotExistResponseShouldBeNull() {
    // WHEN
    UserAdminView response = resource.getUser("user");

    // THEN
    assertNull(response);
    verify(mockIdentityService).get("user");
  }

  @Test
  public void testGetUserBySystemSettingsWhenUserNotExistResponseShouldBeNull() {
    // WHEN
    UserAdminView response = resource.getUserBySystemSetting(Collections.emptyMap());

    // THEN
    assertNull(response);
  }
}
