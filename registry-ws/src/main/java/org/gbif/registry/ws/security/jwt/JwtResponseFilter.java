package org.gbif.registry.ws.security.jwt;

import java.util.Optional;

import com.google.inject.Inject;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

public class JwtResponseFilter implements ContainerResponseFilter {

  private final JwtConfiguration jwtConfiguration;

  @Inject
  public JwtResponseFilter(JwtConfiguration jwtConfiguration) {
    this.jwtConfiguration = jwtConfiguration;
  }

  @Override
  public ContainerResponse filter(
    ContainerRequest request, ContainerResponse response
  ) {
    Optional<String> token = JwtUtils.findTokenInRequest(request, jwtConfiguration);

    if (token.isPresent()) {
      response.getHttpHeaders().putSingle(ContainerRequest.AUTHORIZATION, "Bearer " + token.get());
    }

    return response;
  }
}
