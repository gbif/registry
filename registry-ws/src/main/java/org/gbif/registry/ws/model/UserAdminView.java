package org.gbif.registry.ws.model;

import org.gbif.api.model.common.User;

import org.codehaus.jackson.annotate.JsonUnwrapped;

/**
 * Administrative view of {@link org.gbif.api.model.common.User} .
 */
public class UserAdminView {

  private User user;
  private boolean challengeCodePresent;

  public UserAdminView(){}

  public UserAdminView(User user, boolean challengeCodePresent) {
    this.user = user;
    this.challengeCodePresent = challengeCodePresent;
  }

  @JsonUnwrapped
  public User getUser() {
    return user;
  }

  public boolean isChallengeCodePresent() {
    return challengeCodePresent;
  }

}
