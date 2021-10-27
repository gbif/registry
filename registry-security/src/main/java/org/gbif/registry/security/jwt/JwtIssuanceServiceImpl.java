/*
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
package org.gbif.registry.security.jwt;

import java.util.Date;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/** Class that handle JWT token issuance. */
@Service
@Primary
public class JwtIssuanceServiceImpl implements JwtIssuanceService {

  private JwtConfiguration jwtConfiguration;

  public JwtIssuanceServiceImpl(final JwtConfiguration jwtConfiguration) {
    this.jwtConfiguration = jwtConfiguration;
  }

  /**
   * Generates a JWT with the configuration specified.
   *
   * <p>It always sets the following fields:
   *
   * <ul>
   *   <li>expiration: takes the time from the properties
   *   <li>Issued time: sets the current time when the token is issued
   *   <li>Issuer: takes the issuer from the properties
   *   <li>Username claim: custom claim to store the username received as a parameter
   *   <li>signature: signs the token using {@link SignatureAlgorithm#HS256} and the key specified
   *       in the properties
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
