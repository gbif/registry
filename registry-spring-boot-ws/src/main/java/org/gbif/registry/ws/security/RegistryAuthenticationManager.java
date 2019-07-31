package org.gbif.registry.ws.security;

import com.google.common.base.Strings;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.service.common.IdentityAccessService;
import org.gbif.ws.WebApplicationException;
import org.gbif.ws.security.GbifAuthService;
import org.gbif.ws.security.GbifAuthentication;
import org.gbif.ws.security.GbifUserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.gbif.ws.util.SecurityConstants.BASIC_AUTH;
import static org.gbif.ws.util.SecurityConstants.BASIC_SCHEME_PREFIX;
import static org.gbif.ws.util.SecurityConstants.GBIF_SCHEME;
import static org.gbif.ws.util.SecurityConstants.GBIF_SCHEME_PREFIX;
import static org.gbif.ws.util.SecurityConstants.HEADER_GBIF_USER;

// TODO: 2019-07-26 comment, revise existing comments
// TODO: 2019-07-26 test
@Component
public class RegistryAuthenticationManager implements AuthenticationManager {

  private static final Logger LOG = LoggerFactory.getLogger(RegistryAuthenticationManager.class);

  private static final Pattern COLON_PATTERN = Pattern.compile(":");



  private final IdentityAccessService identityAccessService;
  private final GbifAuthService authService;

  /**
   * In case {@link GbifAuthService} is not provided, this class will reject all authentications
   * on the GBIF scheme prefix.
   */
  public RegistryAuthenticationManager(@NotNull IdentityAccessService identityAccessService, @Nullable GbifAuthService authService) {
    Objects.requireNonNull(identityAccessService, "identityAccessService shall be provided");
    this.identityAccessService = identityAccessService;
    this.authService = authService;
  }

  @Override
  public Authentication authenticate(final Authentication authentication) {
    return authenticate(((GbifAuthentication) authentication).getRequest());
  }

  private GbifAuthentication authenticate(final HttpServletRequest request) {
    // Extract authentication credentials
    final String authentication = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (authentication != null) {
      if (authentication.startsWith(BASIC_SCHEME_PREFIX)) {
        return basicAuthentication(authentication.substring(BASIC_SCHEME_PREFIX.length()), request);
      } else if (authentication.startsWith(GBIF_SCHEME_PREFIX)) {
        return gbifAuthentication(request);
      }
    }
    return getAnonymous(request);
  }

  private GbifAuthentication basicAuthentication(final String authentication, final HttpServletRequest request) {
    // As specified in RFC 7617, the auth header (if not ASCII) is in UTF-8.
    byte[] decodedAuthentication = Base64.getDecoder().decode(authentication);
    String[] values = COLON_PATTERN.split(new String(decodedAuthentication, StandardCharsets.UTF_8), 2);
    if (values.length < 2) {
      LOG.warn("Invalid syntax for username and password: {}", authentication);
      throw new WebApplicationException(HttpStatus.BAD_REQUEST);
    }

    String username = values[0];
    String password = values[1];
    if (username == null || password == null) {
      LOG.warn("Missing basic authentication username or password: {}", authentication);
      throw new WebApplicationException(HttpStatus.BAD_REQUEST);
    }

    // TODO: 2019-07-26 it's not a good approach to check UUID
    // ignore usernames which are UUIDs - these are registry legacy IPT calls and handled by a special security filter
    try {
      UUID.fromString(username);
      return getAnonymous(request);
    } catch (IllegalArgumentException e) {
      // no UUID, continue with regular drupal authentication
    }

    GbifUser user = identityAccessService.authenticate(username, password);
    if (user == null) {
      throw new WebApplicationException(HttpStatus.UNAUTHORIZED);
    }

    LOG.debug("Authenticating user {} via scheme {}", username, BASIC_AUTH);
    return getAuthenticated(user, BASIC_AUTH, request);
  }

  private GbifAuthentication gbifAuthentication(final HttpServletRequest request) {
    String username = request.getHeader(HEADER_GBIF_USER);
    if (Strings.isNullOrEmpty(username)) {
      LOG.warn("Missing gbif username header {}", HEADER_GBIF_USER);
      throw new WebApplicationException(HttpStatus.BAD_REQUEST);
    }
    if (authService == null) {
      LOG.warn("No GbifAuthService defined.");
      throw new WebApplicationException(HttpStatus.UNAUTHORIZED);
    }
    if (!authService.isValidRequest(request)) {
      LOG.warn("Invalid GBIF authenticated request");
      throw new WebApplicationException(HttpStatus.UNAUTHORIZED);
    }

    LOG.debug("Authenticating user {} via scheme {}", username, GBIF_SCHEME);
    if (identityAccessService == null) {
      LOG.debug("No identityService configured! No roles assigned, using anonymous user instead.");
      return getAnonymous(request);
    }

    //check if we have a request that impersonates a user
    GbifUser user = identityAccessService.get(username);
    //Note: using an Anonymous Authorizer is probably not the best thing to do here
    //we should consider simply return null to let another filter handle it
    return user == null ? getAnonymous(request)
        : getAuthenticated(user, GBIF_SCHEME, request);
  }

  /**
   * Get an anonymous.
   * Anonymous users do not have {@link Principal}.
   */
  private GbifAuthentication getAnonymous(final HttpServletRequest request) {
    return new GbifAuthentication(null, null, Collections.emptyList(), "", request);
  }

  private GbifAuthentication getAuthenticated(final GbifUser user, final String authenticationScheme, final HttpServletRequest request) {
    final List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
        .map(Enum::name)
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());

    // TODO: 2019-07-26 set credentials?
    return new GbifAuthentication(new GbifUserPrincipal(user), null, authorities, authenticationScheme, request);
  }
}
