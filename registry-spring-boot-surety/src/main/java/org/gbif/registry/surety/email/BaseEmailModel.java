package org.gbif.registry.surety.email;

import com.google.common.base.MoreObjects;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Very basic email model that holds the main components of an email to send.
 */
@Validated
public class BaseEmailModel {

  @NotNull
  private final String emailAddress;
  private final String subject;
  private final String body;

  private final List<String> ccAddress;

  public BaseEmailModel(String emailAddress, String subject, String body) {
    this(emailAddress, subject, body, null);
  }

  public BaseEmailModel(String emailAddress, String subject, String body, List<String> ccAddress) {
    this.emailAddress = emailAddress;
    this.subject = subject;
    this.body = body;
    this.ccAddress = ccAddress;
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

  public List<String> getCcAddress() {
    return ccAddress;
  }

  // TODO: 2019-06-26 use another (not guava?)
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
            .add("emailAddress", emailAddress)
            .add("subject", subject)
            .add("body", body)
            .add("ccAddress", ccAddress)
            .toString();
  }
}
