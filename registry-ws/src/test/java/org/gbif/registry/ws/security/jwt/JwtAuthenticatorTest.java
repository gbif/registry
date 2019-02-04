package org.gbif.registry.ws.security.jwt;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.service.common.IdentityService;
import org.gbif.registry.guice.RegistryTestModules;

import java.nio.charset.StandardCharsets;

import com.google.common.hash.Hashing;
import com.google.inject.Injector;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

public class JwtAuthenticatorTest {

  private static final String USER_TEST = "test";
  private static final String ISSUER = "issuer";
  private static final long EXPIRY_TIME = 60 * 1000L;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

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
    JwtConfiguration config =
      JwtConfiguration.newBuilder().signingKey(signingKey).expiryTimeInMs(EXPIRY_TIME).issuer(ISSUER).build();
    String token = JwtUtils.generateJwt(user.getUserName(), config);

    JwtAuthenticator jwtAuthenticator = new JwtAuthenticator(config, identityService);

    GbifUser userAuthenticated = jwtAuthenticator.authenticate(token);
    Assert.assertEquals(user.getUserName(), userAuthenticated.getUserName());
  }

  @Test
  public void expiredTokenTest() throws GbifJwtException {
    thrown.expect(GbifJwtException.class);
    thrown.expect(Matchers.hasProperty("errorCode", CoreMatchers.is(GbifJwtException.JwtErrorCode.EXPIRED_TOKEN)));

    JwtConfiguration config =
      JwtConfiguration.newBuilder().signingKey(signingKey).expiryTimeInMs(0L).issuer(ISSUER).build();
    String token = JwtUtils.generateJwt(user.getUserName(), config);

    JwtAuthenticator jwtAuthenticator = new JwtAuthenticator(config, identityService);

    jwtAuthenticator.authenticate(token);
  }

  @Test
  public void invalidSignatureTest() throws GbifJwtException {
    thrown.expect(GbifJwtException.class);
    thrown.expect(Matchers.hasProperty("errorCode", CoreMatchers.is(GbifJwtException.JwtErrorCode.INVALID_TOKEN)));

    JwtConfiguration config =
      JwtConfiguration.newBuilder().signingKey(signingKey).expiryTimeInMs(EXPIRY_TIME).issuer(ISSUER).build();
    String token = JwtUtils.generateJwt(user.getUserName(), config);

    JwtConfiguration configParsing =
      JwtConfiguration.newBuilder().signingKey(generateTestSigningKey("fake")).issuer(ISSUER).build();
    JwtAuthenticator jwtAuthenticator = new JwtAuthenticator(configParsing, identityService);

    jwtAuthenticator.authenticate(token);
  }

  @Test
  public void invalidIssuerTest() throws GbifJwtException {
    thrown.expect(GbifJwtException.class);
    thrown.expect(Matchers.hasProperty("errorCode", CoreMatchers.is(GbifJwtException.JwtErrorCode.INVALID_TOKEN)));

    JwtConfiguration config =
      JwtConfiguration.newBuilder().signingKey(signingKey).expiryTimeInMs(EXPIRY_TIME).issuer(ISSUER).build();
    String token = JwtUtils.generateJwt(user.getUserName(), config);

    JwtConfiguration configParsing = JwtConfiguration.newBuilder().signingKey(signingKey).issuer("fake issuer").build();
    JwtAuthenticator jwtAuthenticator = new JwtAuthenticator(configParsing, identityService);

    jwtAuthenticator.authenticate(token);
  }

  @Test
  public void fakeUserTest() throws GbifJwtException {
    thrown.expect(GbifJwtException.class);
    thrown.expect(Matchers.hasProperty("errorCode", CoreMatchers.is(GbifJwtException.JwtErrorCode.INVALID_USERNAME)));

    JwtConfiguration config =
      JwtConfiguration.newBuilder().signingKey(signingKey).expiryTimeInMs(EXPIRY_TIME).issuer(ISSUER).build();
    String token = JwtUtils.generateJwt("fake user", config);

    JwtAuthenticator jwtAuthenticator = new JwtAuthenticator(config, identityService);

    jwtAuthenticator.authenticate(token);
  }

  private static String generateTestSigningKey(String string) {
    return Hashing.sha256().hashString(string, StandardCharsets.UTF_8).toString();
  }

}
