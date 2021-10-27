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

import org.gbif.registry.domain.ws.AuthenticationDataParameters;
import org.gbif.registry.identity.model.ExtendedLoggedUser;
import org.gbif.registry.identity.service.IdentityService;
import org.gbif.registry.security.jwt.JwtIssuanceService;
import org.gbif.ws.security.GbifAuthentication;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserResourceTest {

  @Mock private GbifAuthentication mockAuth;
  @Mock private IdentityService mockIdentityService;
  @Mock private JwtIssuanceService mockJwtService;
  @InjectMocks private UserResource userResource;

  @Test
  public void testLoginWhenUserNotExistResponseShouldBeBadRequest() {
    // GIVEN
    when(mockAuth.getAuthenticationScheme()).thenReturn("BASIC");
    when(mockAuth.getName()).thenReturn("wrong_user");

    // WHEN
    ResponseEntity<ExtendedLoggedUser> response = userResource.login(mockAuth);

    // THEN
    assertEquals(400, response.getStatusCode().value());
    verify(mockAuth).getAuthenticationScheme();
    verify(mockAuth, times(2)).getName();
  }

  @Test
  public void testWhoamiWhenUserNotExistResponseShouldBeBadRequest() {
    // GIVEN
    when(mockAuth.getAuthenticationScheme()).thenReturn("BASIC");
    when(mockAuth.getName()).thenReturn("wrong_user");

    // WHEN
    ResponseEntity<ExtendedLoggedUser> response = userResource.whoAmI(mockAuth);

    // THEN
    assertEquals(400, response.getStatusCode().value());
    verify(mockAuth).getAuthenticationScheme();
    verify(mockAuth, times(2)).getName();
  }

  @Test
  public void testChangePasswordWhenUserNotExistResponseShouldBeNoContent() {
    // GIVEN
    when(mockAuth.getAuthenticationScheme()).thenReturn("BASIC");
    when(mockAuth.getName()).thenReturn("wrong_user");
    AuthenticationDataParameters parameters = new AuthenticationDataParameters();
    parameters.setPassword("newPassword");
    parameters.setChallengeCode(UUID.randomUUID());

    // WHEN
    ResponseEntity response = userResource.changePassword(parameters, mockAuth);

    // THEN
    assertEquals(204, response.getStatusCode().value());
    verify(mockAuth).getAuthenticationScheme();
    verify(mockAuth, times(2)).getName();
  }
}
