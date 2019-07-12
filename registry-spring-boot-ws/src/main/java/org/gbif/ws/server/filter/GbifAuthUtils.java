package org.gbif.ws.server.filter;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

// analogue of GbifAuthService from gbif-common-ws
public final class GbifAuthUtils {

  private static final String ALGORITHM = "HmacSHA1";
  private static final String CHAR_ENCODING = "UTF8";
  public static final String HEADER_AUTHORIZATION = "Authorization";
  public static final String HEADER_CONTENT_TYPE = "Content-Type";
  public static final String HEADER_CONTENT_MD5 = "Content-MD5";
  public static final String GBIF_SCHEME = "GBIF";
  public static final String HEADER_GBIF_USER = "x-gbif-user";
  public static final String HEADER_ORIGINAL_REQUEST_URL = "x-url";
  private static final char NEWLINE = '\n';
  private static final Pattern COLON_PATTERN = Pattern.compile(":");

  private GbifAuthUtils() {}

  /**
   * Tries to get the appkey from the request header.
   * @param authorizationHeader 'Authorization' header.
   * @return the appkey found or null
   */
  public static String getAppKeyFromRequest(final String authorizationHeader) {
    if(StringUtils.startsWith(authorizationHeader, GBIF_SCHEME + " ")) {
      String[] values = COLON_PATTERN.split(authorizationHeader.substring(5), 2);
      if (values.length == 2) {
        return values[0];
      }
    }
    return null;
  }
}
