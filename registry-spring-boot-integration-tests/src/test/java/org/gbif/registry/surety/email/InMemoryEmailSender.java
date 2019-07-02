package org.gbif.registry.surety.email;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple {@link EmailSender} implementation that keep the {@link BaseEmailModel} into memory.
 * - For testing only
 * - 1 {@link BaseEmailModel} is stored per email address
 * - no automatic cleanup
 */
@Service
public class InMemoryEmailSender implements EmailSender {

  private final Map<String, BaseEmailModel> emails = new HashMap<>();

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
