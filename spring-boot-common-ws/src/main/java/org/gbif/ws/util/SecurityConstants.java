package org.gbif.ws.util;

public final class SecurityConstants {

  private SecurityConstants() {
  }

  public static final String GBIF_SCHEME = "GBIF";
  public static final String GBIF_SCHEME_PREFIX = GBIF_SCHEME + " ";
  public static final String BASIC_SCHEME_PREFIX = "Basic ";
  public static final String BEARER_SCHEME_PREFIX = "Bearer ";
  public static final String BASIC_AUTH = "BASIC";

  public static final String HEADER_TOKEN = "token";
  public static final String HEADER_GBIF_USER = "x-gbif-user";
  public static final String HEADER_CONTENT_MD5 = "Content-MD5";
  public static final String HEADER_ORIGINAL_REQUEST_URL = "x-url";
}
