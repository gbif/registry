package org.gbif.registry.ws.security.jwt;

import org.gbif.registry.ws.security.jwt.JwtConfiguration.GbifClaims;

import java.util.Date;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.ws.rs.core.HttpHeaders;

import com.sun.jersey.spi.container.ContainerRequest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class JwtUtils {

  //Patterns that catches case insensitive versions of word 'bearer'
  private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)bearer");

  private JwtUtils() {}

  /**
   * Generates a JWT with the configuration specified.
   * <p>
   * It always sets the following fields:
   * <ul>
   * <li>expiration: takes the time from the {@link JwtConfiguration}</li>
   * <li>Issued time: sets the current time when the token is issued</li>
   * <li>Issuer: takes the issuer from the {@link JwtConfiguration}</li>
   * <li>Username claim: custom claim to store the username received as a parameter</li>
   * <li>signature: signs the token using {@link SignatureAlgorithm#HS256} and the key specified in the {@link JwtConfiguration}</li>
   * </ul>
   */
  public static String generateJwt(String username, JwtConfiguration config) {
    return Jwts.builder()
      .setExpiration(new Date(System.currentTimeMillis() + config.getExpiryTimeInMs()))
      .setIssuedAt(new Date(System.currentTimeMillis()))
      .setIssuer(config.getIssuer())
      .claim(GbifClaims.USERNAME, username)
      .signWith(SignatureAlgorithm.HS256, config.getSigningKey())
      .compact();
  }

  /**
   * Tries to find the token in the {@link HttpHeaders#AUTHORIZATION} header.
   */
  public static Optional<String> findTokenInRequest(ContainerRequest containerRequest) {
    // check header first
    return Optional.ofNullable(containerRequest.getHeaderValue(HttpHeaders.AUTHORIZATION))
      .filter(JwtUtils::containsBearer)
      .map(JwtUtils::removeBearer);
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

  private static boolean containsBearer(String header) {
    return BEARER_PATTERN.matcher(header).find();
  }

}
