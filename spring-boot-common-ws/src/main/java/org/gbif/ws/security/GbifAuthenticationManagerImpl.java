package org.gbif.ws.security;

import com.google.common.base.Strings;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.service.common.IdentityAccessService;
import org.gbif.ws.WebApplicationException;
import org.gbif.ws.server.RequestObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Base64;
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

@Component
public class GbifAuthenticationManagerImpl implements GbifAuthenticationManager {

  private static final Logger LOG = LoggerFactory.getLogger(GbifAuthenticationManagerImpl.class);

  private static final Pattern COLON_PATTERN = Pattern.compile(":");

  private final IdentityAccessService identityAccessService;
  private final GbifAuthService authService;

  /**
   * In case {@link GbifAuthService} is not provided, this class will reject all authentications
   * on the GBIF scheme prefix.
   */
  public GbifAuthenticationManagerImpl(@NotNull IdentityAccessService identityAccessService, @Nullable GbifAuthService authService) {
    Objects.requireNonNull(identityAccessService, "identityAccessService shall be provided");
    this.identityAccessService = identityAccessService;
    this.authService = authService;
  }

  @Override
  public Authentication authenticate(final Authentication authentication) {
    throw new UnsupportedOperationException("Use another authenticate method");
  }

  /**
   * Authenticate a provided request.
   * There are two authentication types here: GBIF and Basic.
   */
  @Override
  public GbifAuthentication authenticate(final HttpServletRequest request) {
    // Extract authentication credentials
    final String authentication = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (authentication != null) {
      if (authentication.startsWith(BASIC_SCHEME_PREFIX)) {
        return basicAuthentication(authentication.substring(BASIC_SCHEME_PREFIX.length()));
      } else if (authentication.startsWith(GBIF_SCHEME_PREFIX)) {
        return gbifAuthentication(request);
      }
    }
    return getAnonymous();
  }

  /**
   * Basic authentication (when the Authorization header scheme is 'BASIC').
   */
  private GbifAuthentication basicAuthentication(final String authentication) {
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

    // it's not a good approach to check UUID
    // ignore usernames which are UUIDs - these are registry legacy IPT calls and handled by a special security filter
    try {
      UUID.fromString(username);
      return getAnonymous();
    } catch (IllegalArgumentException e) {
      // no UUID, continue with regular drupal authentication
    }

    GbifUser user = identityAccessService.authenticate(username, password);
    if (user == null) {
      throw new WebApplicationException(HttpStatus.UNAUTHORIZED);
    }

    LOG.debug("Authenticating user {} via scheme {}", username, BASIC_AUTH);
    return getAuthenticated(user, BASIC_AUTH);
  }

  /**
   * GBIF authentication (when the Authorization header scheme is 'GBIF').
   */
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
    if (!authService.isValidRequest(new RequestObject(request))) {
      LOG.warn("Invalid GBIF authenticated request");
      throw new WebApplicationException(HttpStatus.UNAUTHORIZED);
    }

    LOG.debug("Authenticating user {} via scheme {}", username, GBIF_SCHEME);

    //check if we have a request that impersonates a user
    GbifUser user = identityAccessService.get(username);
    //Note: using an Anonymous Authorizer is probably not the best thing to do here
    //we should consider simply return null to let another filter handle it
    return user == null ? getAnonymous() : getAuthenticated(user, GBIF_SCHEME);
  }

  /**
   * Get an anonymous.
   * Anonymous users do not have {@link Principal}.
   *
   * @return authentication object for the anonymous user
   */
  private GbifAuthentication getAnonymous() {
    return GbifAuthentication.anonymous();
  }

  /**
   * Construct GbifAuthentication by parameter.
   *
   * @param user                 user which has to be authenticated
   * @param authenticationScheme authentication scheme (BASIC, GBIF etc.)
   * @return authentication object for this user
   */
  private GbifAuthentication getAuthenticated(final GbifUser user, final String authenticationScheme) {
    final List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
      .map(Enum::name)
      .map(SimpleGrantedAuthority::new)
      .collect(Collectors.toList());

    return new GbifAuthentication(new GbifUserPrincipal(user), null, authorities, authenticationScheme);
  }
}
