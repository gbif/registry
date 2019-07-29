package org.gbif.registry.ws.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Class that handle JWT token issuance.
 */
@Service
public class JwtIssuanceService {

  private long expiryTimeInMs;
  private String issuer;
  private String signingKey;

  public JwtIssuanceService(
      @Value("${jwt.expiryTimeInMs}") long expiryTimeInMs,
      @Value("${jwt.issuer}") String issuer,
      @Value("${jwt.signingKey}") String signingKey) {
    this.expiryTimeInMs = expiryTimeInMs;
    this.issuer = issuer;
    this.signingKey = signingKey;
  }

  /**
   * Generates a JWT with the configuration specified.
   * <p>
   * It always sets the following fields:
   * <ul>
   * <li>expiration: takes the time from the properties</li>
   * <li>Issued time: sets the current time when the token is issued</li>
   * <li>Issuer: takes the issuer from the properties</li>
   * <li>Username claim: custom claim to store the username received as a parameter</li>
   * <li>signature: signs the token using {@link SignatureAlgorithm#HS256} and the key specified in the properties</li>
   * </ul>
   */
  public String generateJwt(final String username) {
    return Jwts.builder()
        .setExpiration(new Date(System.currentTimeMillis() + expiryTimeInMs))
        .setIssuedAt(new Date(System.currentTimeMillis()))
        .setIssuer(issuer)
        .claim("userName", username)
        .signWith(SignatureAlgorithm.HS256, signingKey)
        .compact();
  }
}
