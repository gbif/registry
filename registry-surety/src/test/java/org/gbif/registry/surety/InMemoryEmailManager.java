package org.gbif.registry.surety;

import org.gbif.registry.surety.email.BaseEmailModel;
import org.gbif.registry.surety.email.EmailSender;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple {@link EmailSender} implementation that keep the {@link BaseEmailModel} into memory.
 * - For testing only
 * - 1 {@link BaseEmailModel} is stored per email address
 * - no automatic cleanup
 */
public class InMemoryEmailManager implements EmailSender {

  private Map<String, BaseEmailModel> emails = new HashMap<>();

  @Override
  public void send(BaseEmailModel baseEmailModel) {
    System.out.println("sending email to" + baseEmailModel);
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
