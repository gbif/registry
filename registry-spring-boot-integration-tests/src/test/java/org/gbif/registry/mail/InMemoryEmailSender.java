package org.gbif.registry.mail;

import org.gbif.registry.domain.mail.BaseEmailModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple {@link EmailSender} implementation that keep the {@link BaseEmailModel} into memory.
 * - For testing only
 * - 1 {@link BaseEmailModel} is stored per email address
 * - no automatic cleanup
 */
public class InMemoryEmailSender implements EmailSender {

  private static final Logger LOG = LoggerFactory.getLogger(InMemoryEmailSender.class);

  private final Map<String, BaseEmailModel> emails = new HashMap<>();

  public InMemoryEmailSender() {
    LOG.debug("Use InMemoryEmailSender");
  }

  @Override
  public void send(BaseEmailModel baseEmailModel) {
    emails.put(baseEmailModel.getEmailAddress(), baseEmailModel);
  }

  public BaseEmailModel getEmail(String emailAddress) {
    return emails.get(emailAddress);
  }

  /**
   * Clear all emails in memory
   */
  public void clear() {
    emails.clear();
  }

  @Override
  public String toString() {
    return emails.toString();
  }
}
