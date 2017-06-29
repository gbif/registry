package org.gbif.registry.surety.email;

import javax.mail.Session;

/**
 * Container object to simplify handling of the EMail manager configurations.
 */
public class EmailManagerConfiguration {

  static final String FREEMARKER_TEMPLATES_LOCATION = "/email";

  private Session session;
  private String bccAddresses;

  public Session getSession() {
    return session;
  }

  public void setSession(Session session) {
    this.session = session;
  }

  public String getBccAddresses() {
    return bccAddresses;
  }

  public void setBccAddresses(String bccAddresses) {
    this.bccAddresses = bccAddresses;
  }

}
