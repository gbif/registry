package org.gbif.ws.security;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

import static org.gbif.ws.util.SecurityConstants.GBIF_SCHEME_PREFIX;

// analogue of GbifAuthService from gbif-common-ws
public final class GbifAuthUtils {

  private static final Pattern COLON_PATTERN = Pattern.compile(":");

  private GbifAuthUtils() {}

  /**
   * Tries to get the appkey from the request header.
   * @param authorizationHeader 'Authorization' header.
   * @return the appkey found or null
   */
  public static String getAppKeyFromRequest(final String authorizationHeader) {
    if(StringUtils.startsWith(authorizationHeader, GBIF_SCHEME_PREFIX)) {
      String[] values = COLON_PATTERN.split(authorizationHeader.substring(5), 2);
      if (values.length == 2) {
        return values[0];
      }
    }
    return null;
  }
}
