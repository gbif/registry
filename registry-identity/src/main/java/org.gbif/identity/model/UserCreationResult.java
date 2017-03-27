package org.gbif.identity.model;

/**
 * Used to return significant modelError if creation fails.
 */
public class UserCreationResult {

  private Integer key;
  private ModelError modelError;

  public static UserCreationResult fromKey(Integer key){
    return new UserCreationResult(key);
  }

  public static UserCreationResult withError(ModelError modelError){
    return new UserCreationResult(modelError);
  }

  public UserCreationResult(ModelError modelError){
    this.modelError = modelError;
  }
  public UserCreationResult(Integer key){
    this.key = key;
  }

  public Integer getKey() {
    return key;
  }

  public ModelError getError() {
    return modelError;
  }
}
