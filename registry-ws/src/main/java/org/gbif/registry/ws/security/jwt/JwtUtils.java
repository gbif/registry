package org.gbif.registry.ws.security.jwt;

import org.gbif.registry.ws.security.jwt.JwtConfiguration.GbifClaims;

import java.util.Date;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;

import com.google.common.annotations.VisibleForTesting;
import com.sun.jersey.spi.container.ContainerRequest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class JwtUtils {

  //Patterns that catches case insensitive versions of word 'bearer'
  private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)bearer");

  public static String generateJwt(String username, JwtConfiguration config) {
    return Jwts.builder()
      .setExpiration(new Date(System.currentTimeMillis() + config.getExpiryTimeInMs()))
      .setIssuedAt(new Date(System.currentTimeMillis()))
      .setIssuer(config.getIssuer())
      .claim(GbifClaims.USERNAME, username)
      .signWith(SignatureAlgorithm.HS256, config.getSigningKey())
      .compact();
  }

  public static Optional<String> findTokenInRequest(ContainerRequest containerRequest, JwtConfiguration config) {
    // check header first
    return Optional.ofNullable(containerRequest.getHeaderValue(HttpHeaders.AUTHORIZATION))
      .filter(JwtUtils::containsBearer)
      .map(s -> Optional.of(removeBearer(s)))
      // if not found check cookie
      .orElseGet(() -> Optional.ofNullable(containerRequest.getCookies().get(config.getCookieName()))
        .map(Cookie::getValue));
  }

  /**
   * Removes 'bearer' token, leading an trailing whitespaces.
   *
   * @param token to be clean
   *
   * @return a token without whitespaces and the word 'bearer'
   */
  @VisibleForTesting
  public static String removeBearer(String token) {
    return BEARER_PATTERN.matcher(token).replaceAll("").trim();
  }

  @VisibleForTesting
  public static boolean containsBearer(String header) {
    return BEARER_PATTERN.matcher(header).find();
  }

}
