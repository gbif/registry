package org.gbif.registry.ws.security.jwt;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.registry.ws.security.jwt.JwtConfiguration.TOKEN_HEADER_RESPONSE;

/**
 * Filter to add the JWT token to the responses.
 * <p>
 * This filter is needed to add a newly generated token to the response. If there isn't a new token set in the request
 * nothing is added to the response.
 */
public class JwtResponseFilter implements ContainerResponseFilter {

  private static final Logger LOG = LoggerFactory.getLogger(JwtResponseFilter.class);

  @Override
  public ContainerResponse filter(
    ContainerRequest request, ContainerResponse response
  ) {
    String token = request.getHeaderValue(TOKEN_HEADER_RESPONSE);
    if (token != null) {
      LOG.debug("Adding jwt token to the response");
      response.getHttpHeaders().putSingle(TOKEN_HEADER_RESPONSE, token);
    }

    return response;
  }
}
