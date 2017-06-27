package org.gbif.registry.ws.filter;

import org.gbif.api.model.common.AppPrincipal;
import org.gbif.api.model.common.ExtendedPrincipal;
import org.gbif.ws.security.GbifAuthService;

import java.security.Principal;
import javax.validation.constraints.NotNull;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.google.inject.Inject;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.registry.ws.filter.IdentifyFilter.GBIF_SCHEME_APP_ROLE;

/**
 * A filter that allows an application to identify itself as an application (as opposed to an application
 * impersonating a user).
 * This filter must run AFTER {@link IdentifyFilter} if user impersonation using appKey is required.
 * This filter will be skipped if the {@link ContainerRequest} already has a {@link Principal} attached.
 * This filter operates on {@link GbifAuthService#GBIF_SCHEME} only.
 */
public class AppIdentityFilter implements ContainerRequestFilter {

  //FIXME should have its own scheme
  private static final String GBIF_SCHEME_PREFIX = GbifAuthService.GBIF_SCHEME + " ";
  private static final Logger LOG = LoggerFactory.getLogger(AppIdentityFilter.class);

  private final GbifAuthService authService;

  @Inject
  public AppIdentityFilter(@NotNull GbifAuthService authService) {
    this.authService = authService;
  }

  @Override
  public ContainerRequest filter(final ContainerRequest containerRequest) {

    // Only try if no user principal is already there
    if (containerRequest.getUserPrincipal() != null) {
      return containerRequest;
    }

    String authorization = containerRequest.getHeaderValue(ContainerRequest.AUTHORIZATION);
    if (StringUtils.startsWith(authorization, GBIF_SCHEME_PREFIX)) {
      if (!authService.isValidRequest(containerRequest)) {
        LOG.warn("Invalid GBIF authenticated request");
        throw new WebApplicationException(Response.Status.UNAUTHORIZED);
      }

      String username = containerRequest.getHeaderValue(GbifAuthService.HEADER_GBIF_USER);
      //check if it's an app by ensuring the appkey used to sign the request is the one used as x-gbif-user
      String appKey = GbifAuthService.getAppKeyFromRequest(containerRequest::getHeaderValue);

      if (StringUtils.equals(appKey, username)) {
        containerRequest.setSecurityContext(new SecurityContext() {
          private final ExtendedPrincipal principal = new AppPrincipal(appKey, GBIF_SCHEME_APP_ROLE);

          @Override
          public Principal getUserPrincipal() {
            return principal;
          }

          @Override
          public boolean isUserInRole(String s) {
            return principal.hasRole(s);
          }

          @Override
          public boolean isSecure() {
            return containerRequest.isSecure();
          }

          @Override
          public String getAuthenticationScheme() {
            return GbifAuthService.GBIF_SCHEME;
          }
        });
      }
    }
    return containerRequest;
  }
}
