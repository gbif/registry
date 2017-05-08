package org.gbif.identity.model;

/**
 * Enumeration of possible errors in user model mutations (creates and updates).
 */
public enum ModelMutationError {
  USER_ALREADY_EXIST,
  PASSWORD_LENGTH_VIOLATION,
  /** a user can update its own email but it shall not be use by another user*/
  EMAIL_ALREADY_IN_USE,
  CONSTRAINT_VIOLATION
}
