package org.gbif.registry.ws.security.jwt;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.service.common.IdentityService;
import org.gbif.registry.ws.security.jwt.JwtConfiguration.GbifClaims;

import java.util.Optional;

import com.google.inject.Inject;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;

public class JwtAuthenticator {

  private final JwtConfiguration jwtConfiguration;
  private final IdentityService identityService;

  @Inject
  public JwtAuthenticator(JwtConfiguration jwtConfiguration, IdentityService identityService) {
    this.jwtConfiguration = jwtConfiguration;
    this.identityService = identityService;
  }

  public GbifUser authenticate(String token) throws GbifJwtException {
    // validate and parse the token
    Claims claims;
    try {
      claims = Jwts.parser()
        .requireIssuer(jwtConfiguration.getIssuer())
        .setSigningKey(jwtConfiguration.getSigningKey())
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

    String username = Optional.ofNullable(claims.get(GbifClaims.USERNAME, String.class))
      .filter(v -> !v.isEmpty())
      .orElseThrow(() -> new GbifJwtException(GbifJwtException.JwtErrorCode.INVALID_TOKEN));

    return Optional.ofNullable(identityService.get(username))
      .orElseThrow(() -> new GbifJwtException(GbifJwtException.JwtErrorCode.INVALID_USERNAME));
  }

}
