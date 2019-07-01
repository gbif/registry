package org.gbif.registry.identity.model;

import com.google.common.base.MoreObjects;
import org.gbif.api.model.common.AbstractGbifUser;

import javax.validation.ConstraintViolation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.gbif.registry.identity.model.ModelMutationError.CONSTRAINT_VIOLATION;

/**
 * Model containing result of mutations to user data.
 * Mostly used to return significant modelError if creation/update fails.
 */
public class UserModelMutationResult {

  private String username;
  private String email;
  private ModelMutationError modelError;
  private Map<String, String> constraintViolation;

  public static UserModelMutationResult onSuccess() {
    return new UserModelMutationResult(null, null);
  }

  public static UserModelMutationResult onSuccess(String username, String email) {
    return new UserModelMutationResult(username, email);
  }

  public static UserModelMutationResult withError(ModelMutationError modelError) {
    return new UserModelMutationResult(modelError);
  }

  /**
   * Create a new {@link UserModelMutationResult} representing a custom {@link ModelMutationError#CONSTRAINT_VIOLATION}
   * (not coming from javax.validation.Validator).
   * @param key
   * @param value
   * @return
   */
  public static UserModelMutationResult withSingleConstraintViolation(String key, String value) {
    UserModelMutationResult userModelMutationResult = new UserModelMutationResult(CONSTRAINT_VIOLATION);
    Map<String, String> constraintViolation = new HashMap<>();
    constraintViolation.put(key, value);
    userModelMutationResult.setConstraintViolation(constraintViolation);
    return userModelMutationResult;
  }

  public static <T extends AbstractGbifUser> UserModelMutationResult withError(Set<ConstraintViolation<T>> constraintViolation) {
    Map<String, String> cvMap = new HashMap<>();
    constraintViolation.forEach(cv ->
        cvMap.put(cv.getPropertyPath().toString(), cv.getMessage())
    );
    return new UserModelMutationResult(cvMap);
  }

  private UserModelMutationResult(String username, String email) {
    this.username = username;
    this.email = email;
  }

  private UserModelMutationResult(ModelMutationError modelError) {
    this.modelError = modelError;
  }

  /**
   * Only for JSON serialisation
   */
  public UserModelMutationResult() {
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public void setError(ModelMutationError modelError) {
    this.modelError = modelError;
  }

  public void setConstraintViolation(Map<String, String> constraintViolation) {
    this.constraintViolation = constraintViolation;
  }


  public UserModelMutationResult(Map<String, String> constraintViolation) {
    this.modelError = CONSTRAINT_VIOLATION;
    this.constraintViolation = constraintViolation;
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

  public ModelMutationError getError() {
    return modelError;
  }

  public boolean containsError() {
    return modelError != null;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("username", username)
        .add("email", email)
        .add("modelError", modelError)
        .add("constraintViolation", constraintViolation)
        .toString();
  }
}
