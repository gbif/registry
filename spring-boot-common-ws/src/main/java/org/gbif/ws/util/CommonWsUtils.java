package org.gbif.ws.util;

import com.google.common.base.Strings;
import org.springframework.http.MediaType;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
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
  public static String getResponseTypeByExtension(@Nullable String extension, @NotNull String defaultMediaType) {
    if (Strings.isNullOrEmpty(extension)) {
      return defaultMediaType;
    } else if (".xml".equals(extension)) {
      return MediaType.APPLICATION_XML_VALUE;
    } else if (".json".equals(extension)) {
      return MediaType.APPLICATION_JSON_VALUE;
    } else {
      return null;
    }
  }
}
