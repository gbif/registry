/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.ws.resources;

import org.gbif.api.model.common.GbifUser;
import org.gbif.registry.domain.ws.AuthenticationDataParameters;
import org.gbif.registry.identity.model.ExtendedLoggedUser;
import org.gbif.registry.identity.model.LoggedUser;
import org.gbif.registry.identity.model.UserModelMutationResult;
import org.gbif.registry.identity.service.IdentityService;
import org.gbif.registry.security.jwt.GbifJwtException;
import org.gbif.registry.security.jwt.JwtAuthenticateService;
import org.gbif.registry.security.jwt.JwtIssuanceService;
import org.gbif.registry.security.jwt.JwtUtils;

import java.util.Collections;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.gbif.registry.security.SecurityContextCheck.ensureGbifScheme;
import static org.gbif.registry.security.SecurityContextCheck.ensureNotGbifScheme;
import static org.gbif.registry.security.SecurityContextCheck.ensureUserSetInSecurityContext;
import static org.gbif.registry.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.security.UserRoles.APP_ROLE;
import static org.gbif.registry.security.UserRoles.USER_ROLE;

@Validated
@RequestMapping(path = "user", produces = MediaType.APPLICATION_JSON_VALUE)
@RestController
public class UserResource {

  private final IdentityService identityService;
  private final JwtIssuanceService jwtIssuanceService;
  private final JwtAuthenticateService jwtAuthenticateService;

  public UserResource(
      IdentityService identityService,
      JwtIssuanceService jwtIssuanceService,
      JwtAuthenticateService jwtAuthenticateService) {
    this.identityService = identityService;
    this.jwtIssuanceService = jwtIssuanceService;
    this.jwtAuthenticateService = jwtAuthenticateService;
  }

  /**
   * Check the credentials of a user using Basic Authentication and return a {@link LoggedUser} if
   * successful.
   *
   * @return the user as {@link LoggedUser}
   */
  @RequestMapping(
      path = "login",
      method = {RequestMethod.GET, RequestMethod.POST})
  public ResponseEntity<ExtendedLoggedUser> login(Authentication authentication) {
    // the user shall be authenticated using basic auth. scheme only.
    ensureNotGbifScheme(authentication);
    ensureUserSetInSecurityContext(authentication);

    String username = authentication.getName();
    // get the user
    final GbifUser user = identityService.get(username);

    if (user == null) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    identityService.updateLastLogin(user.getKey());

    final String token = jwtIssuanceService.generateJwt(user.getUserName());

    // build response
    ExtendedLoggedUser response =
        ExtendedLoggedUser.from(user, token, identityService.listEditorRights(user.getUserName()));

    return ResponseEntity.ok().cacheControl(CacheControl.noCache().cachePrivate()).body(response);
  }

  @RequestMapping(
      path = "auth/basic",
      method = {RequestMethod.GET, RequestMethod.POST})
  public ResponseEntity<?> basicRemoteAuth(Authentication authentication) {
    // the user shall be authenticated using basic auth. scheme only.
    ensureNotGbifScheme(authentication);
    ensureUserSetInSecurityContext(authentication);

    String username = authentication.getName();
    // get the user
    GbifUser user = identityService.get(username);

    if (user == null) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    return ResponseEntity.ok()
        .cacheControl(CacheControl.noCache().cachePrivate())
        .body(
            ExtendedLoggedUser.from(
                user, null, identityService.listEditorRights(user.getUserName())));
  }

  @RequestMapping(
      path = "auth/app",
      method = {RequestMethod.GET, RequestMethod.POST})
  public ResponseEntity<?> appRemoteAuth(Authentication authentication) {
    // the user shall be authenticated using basic auth. scheme only.
    ensureGbifScheme(authentication);
    ensureUserSetInSecurityContext(authentication);

    String username = authentication.getName();
    // get the user. It can be null
    GbifUser user = identityService.get(username);

    return ResponseEntity.ok()
        .cacheControl(CacheControl.noCache().cachePrivate())
        .body(
            ExtendedLoggedUser.from(
                user,
                null,
                user != null
                    ? identityService.listEditorRights(user.getUserName())
                    : Collections.emptyList()));
  }

  @RequestMapping(
      path = "auth/jwt",
      method = {RequestMethod.GET, RequestMethod.POST})
  public ResponseEntity<?> jwtRemoteAuth(
      HttpServletRequest httpServletRequest, Authentication authentication) {
    ensureUserSetInSecurityContext(authentication);

    // Gets the JWT
    Optional<String> jwtToken = JwtUtils.findTokenInRequest(httpServletRequest);

    if (jwtToken.isPresent()) { // Performs the authentication
      try {
        GbifUser user = jwtAuthenticateService.authenticate(jwtToken.get());
        ExtendedLoggedUser extendedLoggedUser =
            ExtendedLoggedUser.from(
                user, jwtToken.get(), identityService.listEditorRights(user.getUserName()));
        // Success authentication
        return ResponseEntity.ok(extendedLoggedUser);
      } catch (GbifJwtException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body("JWT Error " + ex.getErrorCode());
      }
    }
    // Token not found
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("JWT not found");
  }

  @PostMapping("whoami")
  public ResponseEntity<ExtendedLoggedUser> whoAmI(Authentication authentication) {
    // the user shall be authenticated using basic auth scheme
    ensureNotGbifScheme(authentication);
    ensureUserSetInSecurityContext(authentication);

    // get the user
    return getUserData(authentication.getName());
  }

  @Secured({ADMIN_ROLE, APP_ROLE})
  @RequestMapping(value = "{userName}",
                  method = {RequestMethod.GET, RequestMethod.POST})
  public ResponseEntity<ExtendedLoggedUser> getUserData(@PathVariable("userName") String userName) {
    // the user shall be authenticated using basic auth scheme

    // get the user
    GbifUser user = identityService.get(userName);

    if (user == null) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    return ResponseEntity.ok()
      .cacheControl(CacheControl.noCache().cachePrivate())
      .body(
        ExtendedLoggedUser.from(
          user, null, identityService.listEditorRights(user.getUserName())));
  }

  /** Allows a user to change its own password. */
  @Secured({USER_ROLE})
  @PutMapping(path = "changePassword", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<UserModelMutationResult> changePassword(
      @RequestBody @NotNull AuthenticationDataParameters authenticationDataParameters,
      Authentication authentication) {
    // the user shall be authenticated using basic auth scheme
    ensureNotGbifScheme(authentication);
    ensureUserSetInSecurityContext(authentication);

    final String identifier = authentication.getName();
    final GbifUser user = identityService.get(identifier);
    if (user != null) {
      UserModelMutationResult updatePasswordMutationResult =
          identityService.updatePassword(user.getKey(), authenticationDataParameters.getPassword());
      if (updatePasswordMutationResult.containsError()) {
        return ResponseEntity.unprocessableEntity().body(updatePasswordMutationResult);
      }
    }
    return ResponseEntity.noContent().build();
  }
}
