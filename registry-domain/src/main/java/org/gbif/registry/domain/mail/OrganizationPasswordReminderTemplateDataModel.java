/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.domain.mail;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Organization;

import java.util.Set;

public class OrganizationPasswordReminderTemplateDataModel extends BaseTemplateDataModel {

  private final Organization organization;
  private final Contact contact;
  private final String email;
  private final Set<String> ccEmail;

  public OrganizationPasswordReminderTemplateDataModel(
      String name,
      Organization organization,
      Contact contact,
      String emailAddress,
      Set<String> ccEmail) {
    super(name);
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

  public Set<String> getCcEmail() {
    return ccEmail;
  }
}
