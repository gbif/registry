package org.gbif.registry.mail;

import org.gbif.registry.domain.mail.BaseEmailModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Email manager that does nothing.
 * Useful to use in an environment where email sending is disabled.
 */
@Service
@Qualifier("emptyEmailSender")
public class EmptyEmailSender implements EmailSender {

  @Override
  public void send(BaseEmailModel baseEmailModel) {
    // do nothing
  }
}
