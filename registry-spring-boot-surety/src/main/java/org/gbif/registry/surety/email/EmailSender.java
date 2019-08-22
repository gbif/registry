package org.gbif.registry.surety.email;

/**
 * Manager of emails related tasks.
 */
public interface EmailSender {

  /**
   * Send an email using the provided {@link BaseEmailModel}.
   */
  void send(BaseEmailModel baseEmailModel);
}
