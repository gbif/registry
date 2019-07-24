package org.gbif.registry.ws.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.regex.Pattern;

@Service
public class JwtService {
  //Patterns that catches case insensitive versions of word 'bearer'
  private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)bearer");

  @Value("${jwt.expiryTimeInMs}")
  private long expiryTimeInMs;

  @Value("${jwt.issuer}")
  private String issuer;

  @Value("${jwt.signingKey}")
  private String signingKey;

  // TODO: 2019-07-15 update comment
  // TODO: 2019-07-15 remove config from the parameters?
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
  public String generateJwt(String username) {
    return Jwts.builder()
        .setExpiration(new Date(System.currentTimeMillis() + expiryTimeInMs))
        .setIssuedAt(new Date(System.currentTimeMillis()))
        .setIssuer(issuer)
        .claim("userName", username)
        .signWith(SignatureAlgorithm.HS256, signingKey)
        .compact();
  }
}
