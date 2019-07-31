package org.gbif.registry.ws.security;

import org.apache.commons.lang3.StringUtils;
import org.gbif.api.vocabulary.AppRole;
import org.gbif.registry.ws.config.AppPrincipal;
import org.gbif.registry.ws.config.RegistryAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import javax.annotation.Nullable;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.gbif.registry.ws.security.SecurityConstants.GBIF_SCHEME;
import static org.gbif.registry.ws.security.SecurityConstants.GBIF_SCHEME_PREFIX;
import static org.gbif.registry.ws.security.SecurityConstants.HEADER_GBIF_USER;

// TODO: 2019-07-29 test
// TODO: 2019-07-29 it's not working because of WebApplicationException at IdentityFilter
/**
 * A filter that allows an application to identify itself as an application (as opposed to an application
 * impersonating a user).
 * In order to identify itself an application shall provide its appKey in the header x-gbif-user and sign the request
 * accordingly.
 * If the application can be authenticated AND its appKey is in the whitelist, the {@link Principal#getName()} will return the appKey and the
 * role {@link AppRole#APP} will be assigned to it.
 * <p>
 * We use an appKey whitelist to control which app should be allowed to have the {@link AppRole#APP} while letting
 * the user impersonation available in {@link IdentityFilter}. If at some point multiple {@link AppRole} should be
 * supported the whitelist should simply be changed for something more structured.
 * <p>
 * This filter must run AFTER {@link IdentityFilter} if user impersonation using appKey is required.
 * This filter will be skipped if the request already has a {@link Principal} attached.
 * This filter operates on {@link SecurityConstants#GBIF_SCHEME} only.
 * If the appKeyWhitelist list is not provided no apps will be authenticated by this filter.
 */
@Component
public class AppIdentityFilter extends GenericFilterBean {

  private static final Logger LOG = LoggerFactory.getLogger(AppIdentityFilter.class);

  private final GbifAuthService authService;
  private final List<String> appKeyWhitelist;

  public AppIdentityFilter(
      @NotNull GbifAuthService authService,
      @Nullable @Value("${identity.appkeys.whitelist}") List<String> appKeyWhitelist) {
    this.authService = authService;
    //defensive copy or creation
    this.appKeyWhitelist = appKeyWhitelist != null ? new ArrayList<>(appKeyWhitelist) : new ArrayList<>();
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
    final HttpServletRequest httpRequest = (HttpServletRequest) request;
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    // Only try if no user principal is already there
    if (authentication == null || authentication.getPrincipal() == null) {
      String authorization = httpRequest.getHeader(HttpHeaders.AUTHORIZATION);
      if (StringUtils.startsWith(authorization, GBIF_SCHEME_PREFIX)) {
        if (!authService.isValidRequest(httpRequest)) {
          LOG.warn("Invalid GBIF authenticated request");
          throw new WebApplicationException(HttpStatus.UNAUTHORIZED);
        }

        String username = httpRequest.getHeader(HEADER_GBIF_USER);
        String appKey = GbifAuthUtils.getAppKeyFromRequest(authorization);

        // check if it's an app by ensuring the appkey used to sign the request is the one used as x-gbif-user
        if (StringUtils.equals(appKey, username) && appKeyWhitelist.contains(appKey)) {
          final AppPrincipal principal = new AppPrincipal(appKey, AppRole.APP.name());
          final List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(AppRole.APP.name()));
          final Authentication updatedAuth = new RegistryAuthentication(principal, null, authorities, GBIF_SCHEME, httpRequest);

          SecurityContextHolder.getContext().setAuthentication(updatedAuth);
        }
      }
    }

    filterChain.doFilter(request, response);
  }
}
