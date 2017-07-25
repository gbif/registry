package org.gbif.registry.surety.email;

/**
 * Email manager that does nothing.
 * Useful to use in an environment where emails should not and cannot be sent.
 */
public class EmptyEmailManager implements EmailManager {

  @Override
  public void send(BaseEmailModel baseEmailModel) {
    //empty method
  }
}
