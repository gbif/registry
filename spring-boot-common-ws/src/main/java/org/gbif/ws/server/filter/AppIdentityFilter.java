package org.gbif.ws.server.filter;

import org.apache.commons.lang3.StringUtils;
import org.gbif.api.vocabulary.AppRole;
import org.gbif.ws.security.AppPrincipal;
import org.gbif.ws.security.AppkeysConfiguration;
import org.gbif.ws.security.GbifAuthService;
import org.gbif.ws.security.GbifAuthUtils;
import org.gbif.ws.security.GbifAuthentication;
import org.gbif.ws.security.SecurityContextProvider;
import org.gbif.ws.server.RequestObject;
import org.gbif.ws.util.SecurityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
  private final SecurityContextProvider securityContextProvider;

  public AppIdentityFilter(
      @NotNull GbifAuthService authService,
      AppkeysConfiguration appkeysConfiguration,
      SecurityContextProvider securityContextProvider) {
    this.authService = authService;
    //defensive copy or creation
    this.appKeyWhitelist = appkeysConfiguration.getWhitelist() != null
        ? new ArrayList<>(appkeysConfiguration.getWhitelist()) : new ArrayList<>();
    this.securityContextProvider = securityContextProvider;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
    final HttpServletRequest httpRequest = (HttpServletRequest) request;
    final HttpServletResponse httpResponse = (HttpServletResponse) response;

    final SecurityContext context = securityContextProvider.getContext();
    final Authentication authentication = context.getAuthentication();

    // Only try if no user principal is already there
    if (authentication == null || authentication.getPrincipal() == null) {
      String authorization = httpRequest.getHeader(HttpHeaders.AUTHORIZATION);
      if (StringUtils.startsWith(authorization, SecurityConstants.GBIF_SCHEME_PREFIX)) {
        if (authService.isValidRequest(new RequestObject(httpRequest))) {
          String username = httpRequest.getHeader(SecurityConstants.HEADER_GBIF_USER);
          String appKey = GbifAuthUtils.getAppKeyFromRequest(authorization);

          // check if it's an app by ensuring the appkey used to sign the request is the one used as x-gbif-user
          if (StringUtils.equals(appKey, username) && appKeyWhitelist.contains(appKey)) {
            final List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(AppRole.APP.name()));
            final AppPrincipal principal = new AppPrincipal(appKey, authorities);
            final Authentication updatedAuth = new GbifAuthentication(principal, null, authorities, SecurityConstants.GBIF_SCHEME, httpRequest);

            context.setAuthentication(updatedAuth);
          }
        } else {
          LOG.warn("Invalid GBIF authenticated request");
          httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
        }
      }
    }

    filterChain.doFilter(request, httpResponse);
  }
}
