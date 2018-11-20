package org.gbif.registry.ws.security.jwt;

import org.gbif.utils.file.properties.PropertiesUtil;

import java.util.Properties;

import static com.google.common.base.Preconditions.checkArgument;

public class JwtConfiguration {

  // property keys
  private static final String JWT_PREFIX = "jwt";
  private static final String SIGNING_KEY_PROP = ".signingKey";
  private static final String EXPIRY_TIME_PROP = ".expiryTimeInMs";
  private static final String ISSUER_PROP = ".issuer";
  private static final String COOKIE_NAME_PROP = ".cookieName";
  private static final String SECURITY_CONTEXT_PROP = ".securityContext";

  // defaults
  private static final long DEFAULT_EXPIRY = 7 * 24 * 60 * 60 * 1000L; // 7 days
  private static final String DEFAULT_ISSUER = "GBIF-REGISTRY";
  private static final String DEFAULT_COOKIE_NAME = "token";
  private static final String DEFAULT_SECURITY_CONTEXT = "JWT";

  private final String signingKey;
  private final long expiryTimeInMs;
  private final String issuer;
  private final String cookieName;
  private final String securityContext;

  private JwtConfiguration(Properties properties) {
    this.signingKey = properties.getProperty(SIGNING_KEY_PROP);
    this.expiryTimeInMs = Long.parseLong(properties.getProperty(EXPIRY_TIME_PROP, String.valueOf(DEFAULT_EXPIRY)));
    this.issuer = properties.getProperty(ISSUER_PROP, DEFAULT_ISSUER);
    this.cookieName = properties.getProperty(COOKIE_NAME_PROP, DEFAULT_COOKIE_NAME);
    this.securityContext = properties.getProperty(SECURITY_CONTEXT_PROP, DEFAULT_SECURITY_CONTEXT);
  }

  private JwtConfiguration(Builder builder) {
    this.signingKey = builder.signingKey;
    this.expiryTimeInMs = builder.expiryTimeInMs;
    this.issuer = builder.issuer;
    this.cookieName = builder.cookieName;
    this.securityContext = builder.securityContext;
  }

  public static JwtConfiguration from(Properties properties) {
    checkArgument(properties != null);
    return new JwtConfiguration(PropertiesUtil.filterProperties(properties, JWT_PREFIX));
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

  public String getCookieName() {
    return cookieName;
  }

  public String getSecurityContext() {
    return securityContext;
  }

  public class GbifClaims {

    public static final String USERNAME = "username";
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  static class Builder {

    private String signingKey;
    private long expiryTimeInMs;
    private String issuer;
    private String cookieName;
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

    public Builder cookieName(String cookieName) {
      this.cookieName = cookieName;
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
