package org.gbif.registry.mail;

import org.gbif.registry.domain.mail.BaseEmailModel;

/**
 * Manager of emails related tasks.
 */
public interface EmailSender {

  /**
   * Send an email using the provided {@link BaseEmailModel}.
   */
  void send(BaseEmailModel baseEmailModel);
}
