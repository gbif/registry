package org.gbif.registry.ws.filter;

import javax.ws.rs.core.Response;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

/**
 * A filter that exists to overwrite 401 response codes with a 403 if the client instructs it to by using the header
 * gbif-prefer-403-over-401= true.
 * This exists because ajax based clients (e.g. Angular, JQuery etc) do not get the response before the browser pops up
 * the Authentication window, which happens on a 401 response. This is a known limitation and this is relatively common
 * practice.
 */
public class AuthResponseCodeOverwriteFilter implements ContainerResponseFilter {

  private static String REQUEST_HEADER_OVERWRITE = "gbif-prefer-403-over-401";

  @Override
  public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
    if (Boolean.valueOf(request.getHeaderValue(REQUEST_HEADER_OVERWRITE))
      && Response.Status.UNAUTHORIZED.getStatusCode() == response.getStatus()) {
      response.setStatus(Response.Status.FORBIDDEN.getStatusCode());
    }
    return response;
  }
}
