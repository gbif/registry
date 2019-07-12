package org.gbif.registry.surety.email;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

// TODO: 2019-07-04 remove?
/**
 * Email manager that does nothing.
 * Useful to use in an environment where email sending is disabled.
 */
@Service
@Qualifier("emptyEmailSender")
public class EmptyEmailSender implements EmailSender {

  @Override
  public void send(BaseEmailModel baseEmailModel) {
    //empty method
  }
}
