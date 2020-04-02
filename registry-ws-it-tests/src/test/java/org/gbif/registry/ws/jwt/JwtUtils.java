/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.registry.ws.jwt;

import org.gbif.registry.security.jwt.JwtConfiguration;

import java.util.Date;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class JwtUtils {

  // Patterns that catches case insensitive versions of word 'bearer'
  private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)bearer");

  private JwtUtils() {}

  /**
   * Generates a JWT with the configuration specified.
   *
   * <p>It always sets the following fields:
   *
   * <ul>
   *   <li>expiration: takes the time from the {@link JwtConfiguration}
   *   <li>Issued time: sets the current time when the token is issued
   *   <li>Issuer: takes the issuer from the {@link JwtConfiguration}
   *   <li>Username claim: custom claim to store the username received as a parameter
   *   <li>signature: signs the token using {@link SignatureAlgorithm#HS256} and the key specified
   *       in the {@link JwtConfiguration}
   * </ul>
   */
  public static String generateJwt(String username, JwtConfiguration config) {
    return Jwts.builder()
        .setExpiration(new Date(System.currentTimeMillis() + config.getExpiryTimeInMs()))
        .setIssuedAt(new Date(System.currentTimeMillis()))
        .setIssuer(config.getIssuer())
        .claim("userName", username)
        .signWith(SignatureAlgorithm.HS256, config.getSigningKey())
        .compact();
  }

  /** Tries to find the token in the {@link HttpHeaders#AUTHORIZATION} header. */
  public static Optional<String> findTokenInRequest(HttpServletRequest request) {
    // check header first
    return Optional.ofNullable(request.getHeader(HttpHeaders.AUTHORIZATION))
        .filter(JwtUtils::containsBearer)
        .map(JwtUtils::removeBearer);
  }

  /**
   * Removes 'bearer' token, leading an trailing whitespaces.
   *
   * @param token to be clean
   * @return a token without whitespaces and the word 'bearer'
   */
  private static String removeBearer(String token) {
    return BEARER_PATTERN.matcher(token).replaceAll("").trim();
  }

  private static boolean containsBearer(String header) {
    return BEARER_PATTERN.matcher(header).find();
  }
}
