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
package org.gbif.registry.security.jwt;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.service.common.IdentityAccessService;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;

/** Class that handle all the authentication coming from JWT tokens. */
@Service
public class JwtAuthenticateService {

  private final String issuer;
  private final String signingKey;
  private final IdentityAccessService identityService;

  public JwtAuthenticateService(
      JwtConfiguration jwtConfiguration,
      @Qualifier("baseIdentityAccessService") IdentityAccessService identityService) {
    this.issuer = jwtConfiguration.getIssuer();
    this.signingKey = jwtConfiguration.getSigningKey();
    this.identityService = identityService;
  }

  public GbifUser authenticate(String token) throws GbifJwtException {
    // validate and parse the token
    Claims claims;
    try {
      claims =
          Jwts.parser()
              .requireIssuer(issuer)
              .setSigningKey(signingKey)
              .parseClaimsJws(token)
              .getBody();
    } catch (ExpiredJwtException e) {
      throw new GbifJwtException(GbifJwtException.JwtErrorCode.EXPIRED_TOKEN);
    } catch (Exception e) {
      throw new GbifJwtException(GbifJwtException.JwtErrorCode.INVALID_TOKEN);
    }

    if (claims == null) {
      throw new GbifJwtException(GbifJwtException.JwtErrorCode.INVALID_TOKEN);
    }

    String username =
        Optional.ofNullable(claims.get("userName", String.class))
            .filter(v -> !v.isEmpty())
            .orElseThrow(() -> new GbifJwtException(GbifJwtException.JwtErrorCode.INVALID_TOKEN));

    return Optional.ofNullable(identityService.get(username))
        .orElseThrow(() -> new GbifJwtException(GbifJwtException.JwtErrorCode.INVALID_USERNAME));
  }
}
