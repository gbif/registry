package org.gbif.identity.model;

import org.gbif.api.model.common.UserCreation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolation;

import static org.gbif.identity.model.ModelError.CONSTRAINT_VIOLATION;

/**
 * Used to return significant modelError if creation fails.
 */
public class UserCreationResult {

  private String username;
  private String email;
  private ModelError modelError;
  private Map<String, String> constraintViolation;

  public static UserCreationResult onSuccess(String username, String email) {
    return new UserCreationResult(username, email);
  }

  public static UserCreationResult withError(ModelError modelError) {
    return new UserCreationResult(modelError);
  }

  public static UserCreationResult withError(Set<ConstraintViolation<UserCreation>> constraintViolation) {
    return new UserCreationResult(constraintViolation);
  }

  private UserCreationResult(String username, String email) {
    this.username = username;
    this.email = email;
  }

  private UserCreationResult(ModelError modelError) {
    this.modelError = modelError;
  }

  public UserCreationResult(Set<ConstraintViolation<UserCreation>> constraintViolationSet) {
    modelError = CONSTRAINT_VIOLATION;
    constraintViolation = new HashMap<>();
    constraintViolationSet.forEach(cv ->
            constraintViolation.put(cv.getPropertyPath().toString(), cv.getMessage())
    );
  }

  public String getUsername() {
    return username;
  }

  public String getEmail() {
    return email;
  }

  public Map<String, String> getConstraintViolation() {
    return constraintViolation;
  }

  public ModelError getError() {
    return modelError;
  }

  public boolean containsError() {
    return modelError != null;
  }

}
