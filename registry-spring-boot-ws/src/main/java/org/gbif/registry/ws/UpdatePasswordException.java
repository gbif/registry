package org.gbif.registry.ws;

import org.gbif.registry.identity.model.UserModelMutationResult;

public class UpdatePasswordException extends RuntimeException {

  private final UserModelMutationResult updatePasswordMutationResult;

  public UpdatePasswordException(UserModelMutationResult updatePasswordMutationResult) {
    this.updatePasswordMutationResult = updatePasswordMutationResult;
  }

  public UserModelMutationResult getUpdatePasswordMutationResult() {
    return updatePasswordMutationResult;
  }
}
