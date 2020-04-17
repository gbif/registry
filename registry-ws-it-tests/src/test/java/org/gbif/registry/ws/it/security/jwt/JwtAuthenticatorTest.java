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
import org.gbif.registry.identity.service.IdentityService;
import org.gbif.registry.security.jwt.GbifJwtException;
import org.gbif.registry.security.jwt.JwtAuthenticateService;
import org.gbif.registry.security.jwt.JwtConfiguration;
import org.gbif.registry.ws.jwt.JwtUtils;

import java.nio.charset.StandardCharsets;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.google.common.hash.Hashing;

public class JwtAuthenticatorTest {

  private static final String USER_TEST = "test";
  private static final String ISSUER = "issuer";
  private static final long EXPIRY_TIME = 60 * 1000L;

  @Rule public ExpectedException thrown = ExpectedException.none();

  private static IdentityService identityService;
  private static GbifUser user;
  private static String signingKey;

  @BeforeClass
  public static void setup() {
    identityService = Mockito.mock(IdentityService.class);
    user = new GbifUser();
    user.setUserName(USER_TEST);
    Mockito.when(identityService.get(USER_TEST)).thenReturn(user);
    signingKey = generateTestSigningKey("test");
  }

  @Test
  public void validAuthenticationTest() throws GbifJwtException {
    JwtConfiguration config = new JwtConfiguration();
    config.setSigningKey(signingKey);
    config.setExpiryTimeInMs(EXPIRY_TIME);
    config.setIssuer(ISSUER);
    String token = JwtUtils.generateJwt(user.getUserName(), config);

    JwtAuthenticateService jwtAuthenticator = new JwtAuthenticateService(config, identityService);

    GbifUser userAuthenticated = jwtAuthenticator.authenticate(token);
    Assert.assertEquals(user.getUserName(), userAuthenticated.getUserName());
  }

  @Test
  public void expiredTokenTest() throws GbifJwtException {
    thrown.expect(GbifJwtException.class);
    thrown.expect(
        Matchers.hasProperty(
            "errorCode", CoreMatchers.is(GbifJwtException.JwtErrorCode.EXPIRED_TOKEN)));

    JwtConfiguration config = new JwtConfiguration();
    config.setSigningKey(signingKey);
    config.setExpiryTimeInMs(EXPIRY_TIME);
    config.setIssuer(ISSUER);

    String token = JwtUtils.generateJwt(user.getUserName(), config);

    JwtAuthenticateService jwtAuthenticator = new JwtAuthenticateService(config, identityService);

    jwtAuthenticator.authenticate(token);
  }

  @Test
  public void invalidSignatureTest() throws GbifJwtException {
    thrown.expect(GbifJwtException.class);
    thrown.expect(
        Matchers.hasProperty(
            "errorCode", CoreMatchers.is(GbifJwtException.JwtErrorCode.INVALID_TOKEN)));

    JwtConfiguration config = new JwtConfiguration();
    config.setSigningKey(signingKey);
    config.setExpiryTimeInMs(EXPIRY_TIME);
    config.setIssuer(ISSUER);
    String token = JwtUtils.generateJwt(user.getUserName(), config);

    JwtConfiguration configParsing = new JwtConfiguration();
    config.setSigningKey(generateTestSigningKey("fake"));
    config.setIssuer(ISSUER);

    JwtAuthenticateService jwtAuthenticator =
        new JwtAuthenticateService(configParsing, identityService);

    jwtAuthenticator.authenticate(token);
  }

  @Test
  public void invalidIssuerTest() throws GbifJwtException {
    thrown.expect(GbifJwtException.class);
    thrown.expect(
        Matchers.hasProperty(
            "errorCode", CoreMatchers.is(GbifJwtException.JwtErrorCode.INVALID_TOKEN)));

    JwtConfiguration config = new JwtConfiguration();
    config.setSigningKey(signingKey);
    config.setExpiryTimeInMs(EXPIRY_TIME);
    config.setIssuer(ISSUER);

    String token = JwtUtils.generateJwt(user.getUserName(), config);

    JwtConfiguration configParsing = new JwtConfiguration();
    config.setSigningKey(signingKey);
    config.setIssuer("fake issuer");

    JwtAuthenticateService jwtAuthenticator =
        new JwtAuthenticateService(configParsing, identityService);

    jwtAuthenticator.authenticate(token);
  }

  @Test
  public void fakeUserTest() throws GbifJwtException {
    thrown.expect(GbifJwtException.class);
    thrown.expect(
        Matchers.hasProperty(
            "errorCode", CoreMatchers.is(GbifJwtException.JwtErrorCode.INVALID_USERNAME)));

    JwtConfiguration config = new JwtConfiguration();
    config.setSigningKey(signingKey);
    config.setExpiryTimeInMs(EXPIRY_TIME);
    config.setIssuer(ISSUER);

    String token = JwtUtils.generateJwt("fake user", config);

    JwtAuthenticateService jwtAuthenticator = new JwtAuthenticateService(config, identityService);

    jwtAuthenticator.authenticate(token);
  }

  private static String generateTestSigningKey(String string) {
    return Hashing.sha256().hashString(string, StandardCharsets.UTF_8).toString();
  }
}
