package org.gbif.registry.ws.security.jwt;

/**
 * The service for JWT issuance.
 */
public interface JwtIssuanceService {

  /**
   * Generate a new token.
   */
  String generateJwt(String username);
}
