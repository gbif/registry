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
package org.gbif.registry.ws.it.security.jwt;

import org.gbif.api.model.common.GbifUser;
import org.gbif.registry.identity.service.IdentityService;
import org.gbif.registry.security.jwt.GbifJwtException;
import org.gbif.registry.security.jwt.GbifJwtException.JwtErrorCode;
import org.gbif.registry.security.jwt.JwtAuthenticateService;
import org.gbif.registry.security.jwt.JwtConfiguration;
import org.gbif.registry.ws.jwt.JwtUtils;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.hash.Hashing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JwtAuthenticatorTest {

  private static final String USER_TEST = "test";
  private static final String ISSUER = "issuer";
  private static final long EXPIRY_TIME = 60 * 1000L;

  @Mock IdentityService identityServiceMock;
  String signingKey;
  GbifUser user;

  @BeforeEach
  public void setup() {
    user = new GbifUser();
    user.setUserName(USER_TEST);
    signingKey = generateTestSigningKey("test");
  }

  @Test
  public void validAuthenticationTest() throws GbifJwtException {
    when(identityServiceMock.get(USER_TEST)).thenReturn(user);
    JwtConfiguration config = new JwtConfiguration();
    config.setSigningKey(signingKey);
    config.setExpiryTimeInMs(EXPIRY_TIME);
    config.setIssuer(ISSUER);
    String token = JwtUtils.generateJwt(user.getUserName(), config);

    JwtAuthenticateService jwtAuthenticator =
        new JwtAuthenticateService(config, identityServiceMock);

    GbifUser userAuthenticated = jwtAuthenticator.authenticate(token);
    assertEquals(user.getUserName(), userAuthenticated.getUserName());
  }

  @Test
  public void expiredTokenTest() {
    JwtConfiguration config = new JwtConfiguration();
    config.setSigningKey(signingKey);
    config.setExpiryTimeInMs(10L);
    config.setIssuer(ISSUER);

    String token = JwtUtils.generateJwt(user.getUserName(), config);

    JwtAuthenticateService jwtAuthenticator =
        new JwtAuthenticateService(config, identityServiceMock);

    GbifJwtException exception =
        assertThrows(GbifJwtException.class, () -> jwtAuthenticator.authenticate(token));

    assertEquals(JwtErrorCode.EXPIRED_TOKEN, exception.getErrorCode());
  }

  @Test
  public void invalidSignatureTest() {
    JwtConfiguration config = new JwtConfiguration();
    config.setSigningKey(signingKey);
    config.setExpiryTimeInMs(EXPIRY_TIME);
    config.setIssuer(ISSUER);
    String token = JwtUtils.generateJwt(user.getUserName(), config);

    JwtConfiguration configParsing = new JwtConfiguration();
    config.setSigningKey(generateTestSigningKey("fake"));
    config.setIssuer(ISSUER);

    JwtAuthenticateService jwtAuthenticator =
        new JwtAuthenticateService(configParsing, identityServiceMock);

    GbifJwtException exception =
        assertThrows(GbifJwtException.class, () -> jwtAuthenticator.authenticate(token));

    assertEquals(JwtErrorCode.INVALID_TOKEN, exception.getErrorCode());
  }

  @Test
  public void invalidIssuerTest() {
    JwtConfiguration config = new JwtConfiguration();
    config.setSigningKey(signingKey);
    config.setExpiryTimeInMs(EXPIRY_TIME);
    config.setIssuer(ISSUER);

    String token = JwtUtils.generateJwt(user.getUserName(), config);

    JwtConfiguration configParsing = new JwtConfiguration();
    config.setSigningKey(signingKey);
    config.setIssuer("fake issuer");

    JwtAuthenticateService jwtAuthenticator =
        new JwtAuthenticateService(configParsing, identityServiceMock);

    GbifJwtException exception =
        assertThrows(GbifJwtException.class, () -> jwtAuthenticator.authenticate(token));

    assertEquals(JwtErrorCode.INVALID_TOKEN, exception.getErrorCode());
  }

  @Test
  public void fakeUserTest() {
    JwtConfiguration config = new JwtConfiguration();
    config.setSigningKey(signingKey);
    config.setExpiryTimeInMs(EXPIRY_TIME);
    config.setIssuer(ISSUER);

    String token = JwtUtils.generateJwt("fake user", config);

    JwtAuthenticateService jwtAuthenticator =
        new JwtAuthenticateService(config, identityServiceMock);

    GbifJwtException exception =
        assertThrows(GbifJwtException.class, () -> jwtAuthenticator.authenticate(token));

    assertEquals(JwtErrorCode.INVALID_USERNAME, exception.getErrorCode());
  }

  @SuppressWarnings("UnstableApiUsage")
  private static String generateTestSigningKey(String string) {
    return Hashing.sha256().hashString(string, StandardCharsets.UTF_8).toString();
  }
}
