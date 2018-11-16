package org.gbif.registry.ws.security.jwt;

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
