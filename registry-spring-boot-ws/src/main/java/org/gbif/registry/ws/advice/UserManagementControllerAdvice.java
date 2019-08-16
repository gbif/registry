package org.gbif.registry.ws.advice;

import org.gbif.registry.identity.model.PropertyConstants;
import org.gbif.registry.identity.model.UserModelMutationResult;
import org.gbif.registry.ws.UpdatePasswordException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class UserManagementControllerAdvice {

  @ExceptionHandler(UpdatePasswordException.class)
  public ResponseEntity<UserModelMutationResult> updatePasswordException(UpdatePasswordException e) {
    // determine if it's a challengeCode error
    boolean challengeCodeError = e.getUpdatePasswordMutationResult().getConstraintViolation().keySet()
        .stream()
        .anyMatch(s -> s.equalsIgnoreCase(PropertyConstants.CHALLENGE_CODE_PROPERTY_NAME));

    return challengeCodeError ? ResponseEntity.status(HttpStatus.UNAUTHORIZED).build() :
        ResponseEntity.unprocessableEntity().body(e.getUpdatePasswordMutationResult());
  }
}
