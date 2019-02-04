package org.gbif.registry.ws.security.jwt;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Configuration for JWT authentication.
 * <p>
 * All the values are read from the application properties file but only the signing key is required.
 * The other fields have default values.
 */
public class JwtConfiguration {

  public static final String TOKEN_FIELD_RESPONSE = "token";
  public static final String TOKEN_HEADER_RESPONSE = "token";

  // property keys
  private static final String SIGNING_KEY_PROP = "signingKey";
  private static final String EXPIRY_TIME_PROP = "expiryTimeInMs";
  private static final String ISSUER_PROP = "issuer";
  private static final String SECURITY_CONTEXT_PROP = "securityContext";

  // defaults
  private static final long DEFAULT_EXPIRY = TimeUnit.MINUTES.toMillis(30);
  private static final String DEFAULT_ISSUER = "GBIF-REGISTRY";
  private static final String DEFAULT_SECURITY_CONTEXT = "JWT";

  private final String signingKey;
  private final long expiryTimeInMs;
  private final String issuer;
  private final String securityContext;

  private JwtConfiguration(Properties properties) {
    this.signingKey = properties.getProperty(SIGNING_KEY_PROP);
    this.expiryTimeInMs = Long.parseLong(properties.getProperty(EXPIRY_TIME_PROP, String.valueOf(DEFAULT_EXPIRY)));
    this.issuer = properties.getProperty(ISSUER_PROP, DEFAULT_ISSUER);
    this.securityContext = properties.getProperty(SECURITY_CONTEXT_PROP, DEFAULT_SECURITY_CONTEXT);
  }

  private JwtConfiguration(Builder builder) {
    this.signingKey = builder.signingKey;
    this.expiryTimeInMs = builder.expiryTimeInMs;
    this.issuer = builder.issuer;
    this.securityContext = builder.securityContext;
  }

  public static JwtConfiguration from(Properties properties) {
    checkArgument(properties != null);
    return new JwtConfiguration(properties);
  }

  public String getSigningKey() {
    return signingKey;
  }

  public long getExpiryTimeInMs() {
    return expiryTimeInMs;
  }

  public String getIssuer() {
    return issuer;
  }

  public String getSecurityContext() {
    return securityContext;
  }

  // custom GBIF claims
  public class GbifClaims {

    public static final String USERNAME = "userName";
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  static class Builder {

    private String signingKey;
    private long expiryTimeInMs;
    private String issuer;
    private String securityContext;

    public Builder signingKey(String signingKey) {
      this.signingKey = signingKey;
      return this;
    }

    public Builder expiryTimeInMs(long expiryTimeInMs) {
      this.expiryTimeInMs = expiryTimeInMs;
      return this;
    }

    public Builder issuer(String issuer) {
      this.issuer = issuer;
      return this;
    }

    public Builder securityContext(String securityContext) {
      this.securityContext = securityContext;
      return this;
    }

    public JwtConfiguration build() {
      return new JwtConfiguration(this);
    }

  }

}
