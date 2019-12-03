package org.gbif.registry.domain.mail;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Organization;

import java.net.URL;
import java.util.List;

public class OrganizationPasswordReminderTemplateDataModel extends BaseTemplateDataModel {

  private final Organization organization;
  private final Contact contact;
  private final String email;
  private final List<String> ccEmail;

  public OrganizationPasswordReminderTemplateDataModel(String name,
                                                       URL url,
                                                       Organization organization,
                                                       Contact contact,
                                                       String emailAddress,
                                                       List<String> ccEmail) {
    super(name, url);
    this.organization = organization;
    this.contact = contact;
    this.email = emailAddress;
    this.ccEmail = ccEmail;
  }

  public Organization getOrganization() {
    return organization;
  }

  public Contact getContact() {
    return contact;
  }

  public String getEmail() {
    return email;
  }

  public List<String> getCcEmail() {
    return ccEmail;
  }
}
