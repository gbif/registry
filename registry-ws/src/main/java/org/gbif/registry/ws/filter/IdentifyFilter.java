package org.gbif.registry.ws.filter;

import org.gbif.api.model.common.AppPrincipal;
import org.gbif.api.model.common.ExtendedPrincipal;
import org.gbif.api.model.common.User;
import org.gbif.api.model.common.UserPrincipal;
import org.gbif.api.service.common.IdentityService;
import org.gbif.ws.security.GbifAuthService;

import java.security.Principal;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.sun.jersey.core.util.Base64;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Future replacement for common-ws AuthFilter
 */
public class IdentifyFilter implements ContainerRequestFilter {

  public static final String GBIF_SCHEME_APP_ROLE = "GBIF_SCHEME_APP_ROLE";

  private static class Authorizer implements SecurityContext {

    private final ExtendedPrincipal principal;
    private final String authenticationScheme;
    private final boolean isSecure;

    private Authorizer(ExtendedPrincipal principal, String authenticationScheme, boolean isSecure) {
      this.principal = principal;
      this.authenticationScheme = authenticationScheme;
      this.isSecure = isSecure;
    }

    /**
     * Get an {@link Authorizer} for a specific {@link User}.
     * @param user
     * @param authenticationScheme
     * @param isSecure
     * @return
     */
    static Authorizer getAuthorizer(User user, String authenticationScheme, boolean isSecure) {
      return new Authorizer(new UserPrincipal(user), authenticationScheme, isSecure);
    }

    /**
     * Get an anonymous {@link Authorizer}.
     * Anonymous users do not have {@link Principal}.
     * @param isSecure
     * @return
     */
    static Authorizer getAnonymous(boolean isSecure) {
      return new Authorizer(null, "", isSecure);
    }

    /**
     * Get an {@link Authorizer} for an application represented by an appKey.
     * @param appKey
     * @param authenticationScheme
     * @param isSecure
     * @return
     */
    static Authorizer getAuthorizer(String appKey, String authenticationScheme, boolean isSecure) {
      return new Authorizer(new AppPrincipal(appKey, GBIF_SCHEME_APP_ROLE), authenticationScheme, isSecure);
    }

    @Override
    public String getAuthenticationScheme() {
      return authenticationScheme;
    }

    @Override
    public Principal getUserPrincipal() {
      return principal;
    }

    @Override
    public boolean isSecure() {
      return isSecure;
    }

    @Override
    public boolean isUserInRole(String role) {
      return principal != null && principal.hasRole(role);
    }
  }

  private static final Logger LOG = LoggerFactory.getLogger(IdentifyFilter.class);

  private static final Pattern COLON_PATTERN = Pattern.compile(":");
  private final IdentityService identityService;
  private final GbifAuthService authService;

  @Context
  private UriInfo uriInfo;
  private static final String GBIF_SCHEME_PREFIX = GbifAuthService.GBIF_SCHEME + " ";
  private static final String BASIC_SCHEME_PREFIX = "Basic ";

  /**
   * IdentifyFilter constructor
   * In case {@link GbifAuthService} is not provided, this class will reject all authentications
   * on the GBIF scheme prefix.
   *
   * @param identityService
   * @param authService nullable GbifAuthService
   */
  @Inject
  public IdentifyFilter(@NotNull IdentityService identityService, @Nullable GbifAuthService authService) {
    this.identityService = identityService;
    this.authService = authService;
  }

  @Override
  public ContainerRequest filter(ContainerRequest request) {
    Authorizer authorizer = null;

    // authenticates the HTTP method, but ignores legacy UUID user names
    if (identityService != null) {
      authorizer = authenticate(request);
    }

    if (authorizer == null) {
      // default is the anonymous user
      authorizer = Authorizer.getAnonymous(isSecure());
    }

    request.setSecurityContext(authorizer);

    return request;
  }

  private boolean isSecure() {
    return "https".equals(uriInfo.getRequestUri().getScheme());
  }

  private Authorizer authenticate(ContainerRequest request) {
    // Extract authentication credentials
    String authentication = request.getHeaderValue(ContainerRequest.AUTHORIZATION);
    if (authentication != null) {
      if (authentication.startsWith(BASIC_SCHEME_PREFIX)) {
        return basicAuthentication(authentication.substring(BASIC_SCHEME_PREFIX.length()));
      } else if (authentication.startsWith(GBIF_SCHEME_PREFIX)) {
        return gbifAuthentication(request);
      }
    }
    return Authorizer.getAnonymous(isSecure());
  }

  private Authorizer basicAuthentication(String authentication) {
    String[] values = COLON_PATTERN.split(Base64.base64Decode(authentication));
    if (values.length < 2) {
      LOG.warn("Invalid syntax for username and password: {}", authentication);
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

    String username = values[0];
    String password = values[1];
    if (username == null || password == null) {
      LOG.warn("Missing basic authentication username or password: {}", authentication);
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

    // ignore usernames which are UUIDs - these are registry legacy IPT calls and handled by a special security filter
    try {
      UUID.fromString(username);
      return null;
    } catch (IllegalArgumentException e) {
      // no UUID, continue with regular drupal authentication
    }

    User user = identityService.authenticate(username, password);
    if (user == null) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }

    LOG.debug("Authenticating user {} via scheme {}", username, SecurityContext.BASIC_AUTH);
    return Authorizer.getAuthorizer(user, SecurityContext.BASIC_AUTH, isSecure());
  }

  private Authorizer gbifAuthentication(ContainerRequest request) {
    String username = request.getHeaderValue(GbifAuthService.HEADER_GBIF_USER);
    if (Strings.isNullOrEmpty(username)) {
      LOG.warn("Missing gbif username header {}", GbifAuthService.HEADER_GBIF_USER);
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }
    if (authService == null){
      LOG.warn("No GbifAuthService defined.");
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    if (!authService.isValidRequest(request)) {
      LOG.warn("Invalid GBIF authenticated request");
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }

    LOG.debug("Authenticating user {} via scheme {}", username, GbifAuthService.GBIF_SCHEME);
    if (identityService == null) {
      LOG.debug("No identityService configured! No roles assigned, using anonymous user instead.");
      return Authorizer.getAnonymous(isSecure());
    }

    //check if we have a request that impersonates a user or if it's an app
    User user = identityService.getByIdentifier(username);
    if (user == null) {
      //check if it's an app by ensuring the appkey used to sign the request is the one used as x-gbif-user
      String appKey = GbifAuthService.getAppKeyFromRequest(request::getHeaderValue);
      if(StringUtils.equals(appKey, username)) {
        return Authorizer.getAuthorizer(appKey, GbifAuthService.GBIF_SCHEME, isSecure());
      }
      else{
        LOG.debug(
                "Authorized user {} not found in user service! No roles could be assigned, using anonymous user instead.",
                username);
        return Authorizer.getAnonymous(isSecure());
      }
    }
    return Authorizer.getAuthorizer(user, GbifAuthService.GBIF_SCHEME, isSecure());
  }

}
