package org.gbif.registry.ws.security.jwt;

import java.nio.charset.StandardCharsets;

import com.google.common.hash.Hashing;
import com.sun.jersey.spi.container.ContainerRequest;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class JwtUtilsTest {

  @Test
  public void generateTokenTest() {
    String key = generateTestSigningKey("test");
    JwtConfiguration config =
      JwtConfiguration.newBuilder().signingKey(key).expiryTimeInMs(60 * 1000L).issuer("issuer").build();

    String token = JwtUtils.generateJwt("user", config);

    Claims claims = Jwts.parser()
      .requireIssuer(config.getIssuer())
      .setSigningKey(config.getSigningKey())
      .parseClaimsJws(token)
      .getBody();

    assertEquals("user", claims.get(JwtConfiguration.GbifClaims.USERNAME));
  }

  @Test(expected = IllegalArgumentException.class)
  public void generateUnsignedTokenTest() {
    JwtConfiguration config = JwtConfiguration.newBuilder().expiryTimeInMs(60 * 1000L).issuer("issuer").build();

    JwtUtils.generateJwt("user", config);
  }

  @Test
  public void findTokenInRequestTest() {
    final String token = "abctoken";
    // mock request
    ContainerRequest containerRequest = Mockito.mock(ContainerRequest.class);

    // no token present in request
    assertFalse(JwtUtils.findTokenInRequest(containerRequest).isPresent());

    // token in header
    Mockito.when(containerRequest.getHeaderValue(ContainerRequest.AUTHORIZATION)).thenReturn("Bearer " + token);
    assertEquals(token, JwtUtils.findTokenInRequest(containerRequest).get());

    // empty bearer
    Mockito.when(containerRequest.getHeaderValue(ContainerRequest.AUTHORIZATION)).thenReturn("Bearer");
    assertEquals("", JwtUtils.findTokenInRequest(containerRequest).get());

    // empty header
    Mockito.when(containerRequest.getHeaderValue(ContainerRequest.AUTHORIZATION)).thenReturn("");
    assertFalse(JwtUtils.findTokenInRequest(containerRequest).isPresent());

    // null header
    Mockito.when(containerRequest.getHeaderValue(ContainerRequest.AUTHORIZATION)).thenReturn(null);
    assertFalse(JwtUtils.findTokenInRequest(containerRequest).isPresent());
  }

  private String generateTestSigningKey(String string) {
    return Hashing.sha256().hashString(string, StandardCharsets.UTF_8).toString();
  }

}
