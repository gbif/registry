package org.gbif.registry.ws.security.jwt;

import org.gbif.api.model.common.GbifUser;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.Cookie;

import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import com.sun.jersey.spi.container.ContainerRequest;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.IncorrectClaimException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JwtUtilsTest {

  @Test
  public void generateTokenTest() {
    String key = generateTestSigningKey("test");
    JwtConfiguration config =
      JwtConfiguration.newBuilder().signingKey(key).expiryTimeInMs(60 * 1000L).issuer("issuer").build();

    GbifUser user = new GbifUser();
    user.setUserName("user");

    String token = JwtUtils.generateJwt(user, config);

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

    GbifUser user = new GbifUser();
    user.setUserName("user");

    JwtUtils.generateJwt(user, config);
  }

  @Test
  public void findTokenInRequestTest() {
    final String token = "abctoken";
    // mock request
    ContainerRequest containerRequest = Mockito.mock(ContainerRequest.class);
    JwtConfiguration config = JwtConfiguration.newBuilder().cookieName("test").build();

    // no token present in request
    assertFalse(JwtUtils.findTokenInRequest(containerRequest, config).isPresent());

    // token in header
    Mockito.when(containerRequest.getHeaderValue(ContainerRequest.AUTHORIZATION)).thenReturn("Bearer " + token);
    assertEquals(token, JwtUtils.findTokenInRequest(containerRequest, config).get());

    // empty bearer
    Mockito.when(containerRequest.getHeaderValue(ContainerRequest.AUTHORIZATION)).thenReturn("Bearer");
    assertEquals("", JwtUtils.findTokenInRequest(containerRequest, config).get());

    // empty header
    Mockito.when(containerRequest.getHeaderValue(ContainerRequest.AUTHORIZATION)).thenReturn("");
    assertFalse(JwtUtils.findTokenInRequest(containerRequest, config).isPresent());

    // null header
    Mockito.when(containerRequest.getHeaderValue(ContainerRequest.AUTHORIZATION)).thenReturn(null);
    assertFalse(JwtUtils.findTokenInRequest(containerRequest, config).isPresent());

    // add cookie
    Map<String, Cookie> cookieMap = new HashMap<>();
    cookieMap.put(config.getCookieName(), new Cookie(config.getCookieName(), token));

    Mockito.when(containerRequest.getCookies()).thenReturn(cookieMap);
    assertEquals(token, JwtUtils.findTokenInRequest(containerRequest, config).get());

    // empty cookie
    cookieMap.put(config.getCookieName(), new Cookie(config.getCookieName(), ""));
    Mockito.when(containerRequest.getCookies()).thenReturn(cookieMap);
    assertTrue(JwtUtils.findTokenInRequest(containerRequest, config).isPresent());

    // null cookies
    Mockito.when(containerRequest.getCookies()).thenReturn(new HashMap<>());
    assertFalse(JwtUtils.findTokenInRequest(containerRequest, config).isPresent());
  }

  private String generateTestSigningKey(String string) {
    return Hashing.sha256().hashString(string, StandardCharsets.UTF_8).toString();
  }

}
