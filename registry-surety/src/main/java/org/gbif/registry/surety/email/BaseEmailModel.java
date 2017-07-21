package org.gbif.registry.surety.email;

import com.google.common.base.MoreObjects;

/**
 * Very basic email model that holds the main components of an email to send.
 */
public class BaseEmailModel {

  private final String emailAddress;
  private final String subject;
  private final String body;

  public BaseEmailModel(String emailAddress, String subject, String body) {
    this.emailAddress = emailAddress;
    this.subject = subject;
    this.body = body;

  }

  public String getEmailAddress() {
    return emailAddress;
  }

  public String getSubject() {
    return subject;
  }

  public String getBody() {
    return body;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
            .add("emailAddress", emailAddress)
            .add("subject", subject)
            .add("body", body)
            .toString();
  }
}
