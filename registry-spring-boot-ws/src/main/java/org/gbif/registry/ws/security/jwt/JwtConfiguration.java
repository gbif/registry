package org.gbif.registry.ws.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfiguration {

  @NotNull
  private long expiryTimeInMs;
  @NotNull
  private String issuer;
  @NotNull
  private String signingKey;

  public long getExpiryTimeInMs() {
    return expiryTimeInMs;
  }

  public void setExpiryTimeInMs(long expiryTimeInMs) {
    this.expiryTimeInMs = expiryTimeInMs;
  }

  public String getIssuer() {
    return issuer;
  }

  public void setIssuer(String issuer) {
    this.issuer = issuer;
  }

  public String getSigningKey() {
    return signingKey;
  }

  public void setSigningKey(String signingKey) {
    this.signingKey = signingKey;
  }
}
