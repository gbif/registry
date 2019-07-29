package org.gbif.registry.ws.resources;

import org.apache.http.HttpStatus;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.service.common.IdentityService;
import org.gbif.api.service.common.LoggedUserWithToken;
import org.gbif.registry.ws.config.UserPrincipal;
import org.gbif.registry.ws.security.jwt.JwtIssuanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/user")
@RestController
public class UserResource {

  private final IdentityService identityService;
  private final JwtIssuanceService jwtIssuanceService;

  public UserResource(IdentityService identityService, JwtIssuanceService jwtIssuanceService) {
    this.identityService = identityService;
    this.jwtIssuanceService = jwtIssuanceService;
  }

  @PostMapping("/login")
  public ResponseEntity<LoggedUserWithToken> loginPost(Authentication authentication) {
    // TODO: 2019-07-12 uncomment
    // the user shall be authenticated using basic auth. scheme only.
//    ensureNotGbifScheme(securityContext);
//    ensureUserSetInSecurityContext(securityContext);
    return login(((UserPrincipal) authentication.getPrincipal()).getUsername());
  }

  // only to use in login since it updates the last login
  private ResponseEntity<LoggedUserWithToken> login(String username) {
    // get the user
    GbifUser user = identityService.get(username);

    if (user == null) {
      return ResponseEntity.status(HttpStatus.SC_BAD_REQUEST).build();
    }

    identityService.updateLastLogin(user.getKey());

    // build response
    LoggedUserWithToken response = LoggedUserWithToken.from(
        user,
        jwtIssuanceService.generateJwt(user.getUserName()),
        identityService.listEditorRights(user.getUserName()));
    // add jwt token


    // TODO: 2019-07-12 add cache headers
//    cacheControl.setPrivate(true);
//    cacheControl.setNoCache(true);
//    cacheControl.setNoStore(true);
    return ResponseEntity.ok(response);
//        .cacheControl(createNoCacheHeaders())
//        .build();
  }

}
