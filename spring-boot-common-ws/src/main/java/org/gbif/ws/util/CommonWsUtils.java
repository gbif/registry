package org.gbif.ws.util;

import org.springframework.http.MediaType;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Class with util methods for WS.
 */
public final class CommonWsUtils {

  private CommonWsUtils() {
  }

  /**
   * Retrieve the first occurrence of the param in params.
   * Can be applied for HttpHeaders.
   */
  public static String getFirst(Map<String, String[]> params, String param) {
    final String[] values = params.get(param);
    String resultValue = null;

    if (values != null && values[0] != null) {
      resultValue = values[0];
    }

    return resultValue;
  }

  /**
   * MediaType by string extension.
   */
  @Nullable
  public static String getResponseTypeByExtension(String extension) {
    if (".xml".equals(extension)) {
      return MediaType.APPLICATION_XML_VALUE;
    } else if (".json".equals(extension)) {
      return MediaType.APPLICATION_JSON_VALUE;
    } else {
      return null;
    }
  }
}
