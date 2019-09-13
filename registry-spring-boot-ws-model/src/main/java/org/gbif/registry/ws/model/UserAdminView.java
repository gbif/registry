package org.gbif.registry.ws.model;

import org.codehaus.jackson.annotate.JsonUnwrapped;
import org.gbif.api.model.common.GbifUser;

/**
 * Administrative view of {@link org.gbif.api.model.common.User} .
 */
public class UserAdminView {

  private GbifUser user;
  private boolean challengeCodePresent;

  public UserAdminView(){}

  public UserAdminView(GbifUser user, boolean challengeCodePresent) {
    this.user = user;
    this.challengeCodePresent = challengeCodePresent;
  }

  @JsonUnwrapped
  public GbifUser getUser() {
    return user;
  }

  public boolean isChallengeCodePresent() {
    return challengeCodePresent;
  }

}
