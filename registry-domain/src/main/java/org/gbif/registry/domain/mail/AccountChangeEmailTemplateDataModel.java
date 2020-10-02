package org.gbif.registry.domain.mail;

import java.net.URL;

public class AccountChangeEmailTemplateDataModel extends ConfirmableTemplateDataModel {

  private final String currentEmail;
  private final String newEmail;

  public AccountChangeEmailTemplateDataModel(String name, URL url, String currentEmail, String newEmail) {
    super(name, url);
    this.currentEmail = currentEmail;
    this.newEmail = newEmail;
  }

  public String getCurrentEmail() {
    return currentEmail;
  }

  public String getNewEmail() {
    return newEmail;
  }
}
