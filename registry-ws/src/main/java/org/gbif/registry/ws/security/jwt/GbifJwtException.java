package org.gbif.registry.ws.security.jwt;

/**
 * Exception to handle all the possible JWT error cases.
 */
public class GbifJwtException extends Exception {

  private final JwtErrorCode errorCode;

  public GbifJwtException(JwtErrorCode errorCode) {
    this.errorCode = errorCode;
  }

  public JwtErrorCode getErrorCode() {
    return errorCode;
  }

  enum JwtErrorCode {
    EXPIRED_TOKEN, INVALID_TOKEN, INVALID_USERNAME
  }

}
