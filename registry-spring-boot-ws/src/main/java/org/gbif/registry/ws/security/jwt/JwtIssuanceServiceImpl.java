package org.gbif.registry.ws.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Class that handle JWT token issuance.
 */
@Service
@Primary
public class JwtIssuanceServiceImpl implements JwtIssuanceService {

  private JwtConfiguration jwtConfiguration;

  public JwtIssuanceServiceImpl(final JwtConfiguration jwtConfiguration) {
    this.jwtConfiguration = jwtConfiguration;
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
  @Override
  public String generateJwt(final String username) {
    return Jwts.builder()
        .setExpiration(new Date(System.currentTimeMillis() + jwtConfiguration.getExpiryTimeInMs()))
        .setIssuedAt(new Date(System.currentTimeMillis()))
        .setIssuer(jwtConfiguration.getIssuer())
        .claim("userName", username)
        .signWith(SignatureAlgorithm.HS256, jwtConfiguration.getSigningKey())
        .compact();
  }
}
