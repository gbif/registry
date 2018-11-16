package org.gbif.registry.ws.security.jwt;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.common.GbifUserPrincipal;

import java.security.Principal;
import java.util.Optional;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.google.inject.Inject;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JwtRequestFilter implements ContainerRequestFilter {

  private static final Logger LOG = LoggerFactory.getLogger(JwtRequestFilter.class);

  private final JwtConfiguration jwtConfiguration;
  private final JwtAuthenticator jwtAuthenticator;

  @Inject
  public JwtRequestFilter(
    JwtConfiguration jwtConfiguration, JwtAuthenticator jwtAuthenticator
  ) {
    this.jwtConfiguration = jwtConfiguration;
    this.jwtAuthenticator = jwtAuthenticator;
  }

  @Override
  public ContainerRequest filter(ContainerRequest containerRequest) {

    Optional<String> token = JwtUtils.findTokenInRequest(containerRequest, jwtConfiguration);

    if (!token.isPresent()) {
      // if there is no token in the request we ignore this authentication
      return containerRequest;
    }

    try {
      GbifUser gbifUser = jwtAuthenticator.authenticate(token.get());

      containerRequest.setSecurityContext(new SecurityContext() {

        private final GbifUserPrincipal gbifUserPrincipal = new GbifUserPrincipal(gbifUser);

        @Override
        public Principal getUserPrincipal() {
          return gbifUserPrincipal;
        }

        @Override
        public boolean isUserInRole(String role) {
          return gbifUserPrincipal.hasRole(role);
        }

        @Override
        public boolean isSecure() {
          return containerRequest.getSecurityContext().isSecure();
        }

        @Override
        public String getAuthenticationScheme() {
          return jwtConfiguration.getSecurityContext();
        }
      });

      // generate a new token with a new expiration
      containerRequest.getRequestHeaders()
        .putSingle(ContainerRequest.AUTHORIZATION, "Bearer " + JwtUtils.generateJwt(gbifUser, jwtConfiguration));

    } catch (GbifJwtException e) {
      // TODO: ask Morten if these error codes are ok for him??
//      if (GbifJwtException.JwtErrorCode.EXPIRED_TOKEN == e.getErrorCode()
//          || GbifJwtException.JwtErrorCode.INVALID_TOKEN == e.getErrorCode()) {
//        throw new WebApplicationException(Response.Status.UNAUTHORIZED);
//      }
//      if (GbifJwtException.JwtErrorCode.INVALID_USERNAME == e.getErrorCode()) {
//        throw new WebApplicationException(Response.Status.FORBIDDEN);
//      }
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }

    return containerRequest;
  }
}
