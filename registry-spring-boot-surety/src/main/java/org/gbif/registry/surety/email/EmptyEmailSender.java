package org.gbif.registry.surety.email;

import org.springframework.stereotype.Service;

/**
 * Email manager that does nothing.
 * Useful to use in an environment where email sending is disabled.
 */
@Service
public class EmptyEmailSender implements EmailSender {

  @Override
  public void send(BaseEmailModel baseEmailModel) {
    //empty method
  }
}
