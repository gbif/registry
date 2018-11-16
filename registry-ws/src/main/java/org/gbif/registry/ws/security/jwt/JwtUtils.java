package org.gbif.registry.ws.security.jwt;

import org.gbif.api.model.common.GbifUser;
import org.gbif.registry.ws.security.jwt.JwtConfiguration.GbifClaims;

import java.util.Date;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.ws.rs.core.Cookie;

import com.sun.jersey.spi.container.ContainerRequest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class JwtUtils {

  //Patterns that catches case insensitive versions of word 'bearer'
  private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)bearer");

  public static String generateJwt(GbifUser user, JwtConfiguration config) {
    return Jwts.builder()
      .setExpiration(new Date(System.currentTimeMillis() + config.getExpiryTimeInMs()))
      .setIssuedAt(new Date(System.currentTimeMillis()))
      .setIssuer(config.getIssuer())
      .claim(GbifClaims.USERNAME, user.getUserName())
      .signWith(SignatureAlgorithm.HS256, config.getSigningKey())
      .compact();
  }

  public static Optional<String> findTokenInRequest(ContainerRequest containerRequest, JwtConfiguration config) {
    // TODO: ask morten if the token will come always in the header or in the cookie too?
    // check header first
    return Optional.ofNullable(containerRequest.getHeaderValue(ContainerRequest.AUTHORIZATION))
      .filter(header -> BEARER_PATTERN.matcher(header).find())
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
  private static String removeBearer(String token) {
    return BEARER_PATTERN.matcher(token).replaceAll("").trim();
  }

}
