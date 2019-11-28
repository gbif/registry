package org.gbif.registry.ws.resources;

import org.gbif.api.service.common.IdentityService;
import org.gbif.api.service.common.LoggedUserWithToken;
import org.gbif.registry.ws.model.AuthenticationDataParameters;
import org.gbif.registry.ws.security.jwt.JwtIssuanceService;
import org.gbif.ws.security.GbifAuthentication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserResourceTest {

  @Mock
  private GbifAuthentication mockAuth;
  @Mock
  private IdentityService mockIdentityService;
  @Mock
  private JwtIssuanceService mockJwtService;
  @InjectMocks
  private UserResource userResource;

  @Test
  public void testLoginWhenUserNotExistResponseShouldBeBadRequest() {
    // GIVEN
    when(mockAuth.getAuthenticationScheme()).thenReturn("BASIC");
    when(mockAuth.getName()).thenReturn("wrong_user");

    // WHEN
    ResponseEntity<LoggedUserWithToken> response = userResource.login(mockAuth);

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
    ResponseEntity<LoggedUserWithToken> response = userResource.whoAmI(mockAuth);

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
