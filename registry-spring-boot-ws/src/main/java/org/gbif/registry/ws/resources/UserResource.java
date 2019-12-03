package org.gbif.registry.ws.resources;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.service.common.IdentityService;
import org.gbif.api.service.common.LoggedUser;
import org.gbif.api.service.common.LoggedUserWithToken;
import org.gbif.registry.domain.ws.AuthenticationDataParameters;
import org.gbif.registry.identity.model.UserModelMutationResult;
import org.gbif.registry.ws.security.jwt.JwtIssuanceService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.gbif.registry.ws.security.SecurityContextCheck.ensureNotGbifScheme;
import static org.gbif.registry.ws.security.SecurityContextCheck.ensureUserSetInSecurityContext;
import static org.gbif.registry.ws.security.UserRoles.USER_ROLE;

@RequestMapping("user")
@RestController
public class UserResource {

  private final IdentityService identityService;
  private final JwtIssuanceService jwtIssuanceService;

  public UserResource(IdentityService identityService, JwtIssuanceService jwtIssuanceService) {
    this.identityService = identityService;
    this.jwtIssuanceService = jwtIssuanceService;
  }

  /**
   * Check the credentials of a user using Basic Authentication and return a {@link LoggedUser} if successful.
   *
   * @return the user as {@link LoggedUser}
   */
  @RequestMapping(value = "login",
    method = {RequestMethod.GET, RequestMethod.POST})
  public ResponseEntity<LoggedUserWithToken> login(Authentication authentication) {
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
    LoggedUserWithToken response = LoggedUserWithToken.from(user, token,
      identityService.listEditorRights(user.getUserName()));

    return ResponseEntity.ok()
      .cacheControl(CacheControl.noCache().cachePrivate())
      .body(response);
  }

  @PostMapping("whoami")
  public ResponseEntity<LoggedUserWithToken> whoAmI(Authentication authentication) {
    // the user shall be authenticated using basic auth scheme
    ensureNotGbifScheme(authentication);
    ensureUserSetInSecurityContext(authentication);

    // get the user
    final GbifUser user = identityService.get(authentication.getName());

    if (user == null) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    return ResponseEntity.ok()
      .cacheControl(CacheControl.noCache().cachePrivate())
      .body(LoggedUserWithToken.from(user, null, identityService.listEditorRights(user.getUserName())));
  }

  /**
   * Allows a user to change its own password.
   */
  @Secured({USER_ROLE})
  @PutMapping("changePassword")
  public ResponseEntity changePassword(@RequestBody AuthenticationDataParameters authenticationDataParameters,
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
