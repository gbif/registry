package org.gbif.ws.util;

import java.util.Map;

public final class CommonWsUtils {

  private CommonWsUtils() {
  }

  public static String getFirst(Map<String, String[]> params, String param) {
    final String[] values = params.get(param);
    String resultValue = null;

    if (values != null && values[0] != null) {
      resultValue = values[0];
    }

    return resultValue;
  }
}
