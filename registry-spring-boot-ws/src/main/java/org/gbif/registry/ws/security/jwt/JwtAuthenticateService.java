package org.gbif.registry.ws.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.service.common.IdentityService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Class that handle all the authentication coming from JWT tokens.
 */
@Service
public class JwtAuthenticateService {

  private final String issuer;
  private final String signingKey;
  private final IdentityService identityService;

  public JwtAuthenticateService(
      @Value("${jwt.issuer}") String issuer,
      @Value("${jwt.signingKey}") String signingKey,
      IdentityService identityService) {
    this.issuer = issuer;
    this.signingKey = signingKey;
    this.identityService = identityService;
  }

  public GbifUser authenticate(String token) throws GbifJwtException {
    // validate and parse the token
    Claims claims;
    try {
      claims = Jwts.parser()
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

    String username = Optional.ofNullable(claims.get("userName", String.class))
        .filter(v -> !v.isEmpty())
        .orElseThrow(() -> new GbifJwtException(GbifJwtException.JwtErrorCode.INVALID_TOKEN));

    return Optional.ofNullable(identityService.get(username))
        .orElseThrow(() -> new GbifJwtException(GbifJwtException.JwtErrorCode.INVALID_USERNAME));
  }
}
