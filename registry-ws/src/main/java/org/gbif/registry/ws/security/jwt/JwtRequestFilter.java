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

import static org.gbif.registry.ws.security.jwt.JwtConfiguration.TOKEN_HEADER_RESPONSE;
import static org.gbif.registry.ws.security.jwt.JwtUtils.generateJwt;

/**
 * Filter to validate the JWT tokens.
 * <p>
 * If the token is not present this validation is skipped.
 */
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

    Optional<String> token = JwtUtils.findTokenInRequest(containerRequest);

    if (!token.isPresent()) {
      // if there is no token in the request we ignore this authentication
      LOG.debug("Skipping JWT validation");
      return containerRequest;
    }

    try {
      GbifUser gbifUser = jwtAuthenticator.authenticate(token.get());
      LOG.debug("JWT successfully validated for user {}", gbifUser.getUserName());

      // set the user to the security context
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

      // refresh the token and add it to the headers
      containerRequest.getRequestHeaders()
        .putSingle(TOKEN_HEADER_RESPONSE, generateJwt(gbifUser.getUserName(), jwtConfiguration));

    } catch (GbifJwtException e) {
      LOG.warn("JWT validation failed: {}", e.getErrorCode());
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }

    return containerRequest;
  }
}
