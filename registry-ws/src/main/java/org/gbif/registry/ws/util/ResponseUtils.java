package org.gbif.registry.ws.util;

import javax.annotation.Nullable;
import javax.ws.rs.core.Response;

/**
 * Simple utility to build Jersey {@link Response}
 */
public class ResponseUtils {

  private ResponseUtils() {
  }

  public static Response buildResponse(Response.Status status) {
    return buildResponse(status, null);
  }

  public static Response buildResponse(Response.Status status, @Nullable Object entity) {
    return buildResponse(status.getStatusCode(), entity);
  }

  public static Response buildResponse(int returnStatusCode, @Nullable Object entity) {
    Response.ResponseBuilder bldr = Response.status(returnStatusCode);
    if (entity != null) {
      bldr.entity(entity);
    }
    return bldr.build();
  }
}
