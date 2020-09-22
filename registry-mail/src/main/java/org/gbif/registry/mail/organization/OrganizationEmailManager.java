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
package org.gbif.registry.mail.organization;

import org.gbif.api.model.directory.Person;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.registry.domain.mail.OrganizationPasswordReminderTemplateDataModel;
import org.gbif.registry.domain.mail.OrganizationTemplateDataModel;
import org.gbif.registry.mail.BaseEmailModel;
import org.gbif.registry.mail.EmailTemplateProcessor;
import org.gbif.registry.mail.FreemarkerEmailTemplateProcessor;
import org.gbif.registry.mail.config.MailConfigurationProperties;
import org.gbif.registry.mail.config.OrganizationSuretyMailConfigurationProperties;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import freemarker.template.TemplateException;

/**
 * Manager handling the different types of email related to organization endorsement.
 * Responsibilities (with the help of (via {@link FreemarkerEmailTemplateProcessor}): - decide where
 * to send the email (which address) - generate the body of the email
 */
@Service
public class OrganizationEmailManager {

  private static final Logger LOG = LoggerFactory.getLogger(OrganizationEmailManager.class);

  private final EmailTemplateProcessor emailTemplateProcessors;
  private final MailConfigurationProperties mailConfigProperties;
  private final OrganizationSuretyMailConfigurationProperties organizationMailConfigProperties;

  private static final String HELPDESK_NAME = "Helpdesk";

  /** @param emailTemplateProcessors configured EmailTemplateProcessor */
  public OrganizationEmailManager(
      @Qualifier("organizationEmailTemplateProcessor")
          EmailTemplateProcessor emailTemplateProcessors,
      MailConfigurationProperties mailConfigProperties,
      OrganizationSuretyMailConfigurationProperties organizationMailConfigProperties) {
    Objects.requireNonNull(emailTemplateProcessors, "emailTemplateProcessors shall be provided");
    this.emailTemplateProcessors = emailTemplateProcessors;
    this.mailConfigProperties = mailConfigProperties;
    this.organizationMailConfigProperties = organizationMailConfigProperties;
  }

  /**
   * Generate an email to inform a new organization has to be endorsed.
   * If nodeManagerContact does not contain an email address, the model will be set to send the message to helpdesk.
   *
   * @param newOrganization new organization data
   * @param nodeManager the {@link Person} representing the NodeManager or null if there is none
   * @param confirmationKey organization confirmation key
   * @param endorsingNode organization endorsing node
   * @return the {@link BaseEmailModel} or null if the model can not be generated
   */
  public BaseEmailModel generateOrganizationEndorsementEmailModel(
      Organization newOrganization,
      @Nullable Person nodeManager,
      UUID confirmationKey,
      Node endorsingNode)
      throws IOException {
    Objects.requireNonNull(newOrganization, "newOrganization shall be provided");
    Objects.requireNonNull(newOrganization.getKey(), "newOrganization key shall be present");
    Objects.requireNonNull(confirmationKey, "confirmationKey shall be provided");
    Objects.requireNonNull(endorsingNode, "endorsingNode shall be provided");
    LOG.debug(
        "Organization key: {}; Confirmation key: {}; Endorsing node key: {}",
        newOrganization.getKey(),
        confirmationKey,
        endorsingNode.getKey());

    Optional<String> nodeManagerEmailAddress =
        Optional.ofNullable(nodeManager).map(Person::getEmail);
    LOG.debug("Node manager email address: {}", nodeManagerEmailAddress);

    String name;
    String emailAddress;
    // do we have an email to contact the node manager ?
    if (nodeManagerEmailAddress.isPresent()) {
      name =
          Optional.ofNullable(
                  StringUtils.trimToNull(
                      org.gbif.utils.text.StringUtils.thenJoin(
                          StringUtils::trimToNull,
                          nodeManager.getFirstName(),
                          nodeManager.getSurname())))
              .orElse(endorsingNode.getTitle());
      emailAddress = nodeManagerEmailAddress.get();
    } else {
      name = HELPDESK_NAME;
      emailAddress = organizationMailConfigProperties.getHelpdesk();
    }
    LOG.debug("Name: {}; Email address: {}", name, emailAddress);

    BaseEmailModel baseEmailModel;
    try {
      URL endorsementUrl = generateEndorsementUrl(newOrganization.getKey(), confirmationKey);
      OrganizationTemplateDataModel templateDataModel =
          OrganizationTemplateDataModel.buildEndorsementModel(
              name,
              endorsementUrl,
              newOrganization,
              endorsingNode,
              nodeManagerEmailAddress.isPresent());

      // CC helpdesk unless we are sending the email to helpdesk
      Set<String> ccAddresses =
          emailAddress.equals(organizationMailConfigProperties.getHelpdesk())
              ? Collections.emptySet()
              : Collections.singleton(organizationMailConfigProperties.getHelpdesk());
      LOG.debug("Cc addresses: {}", ccAddresses);

      baseEmailModel =
          emailTemplateProcessors.buildEmail(
              OrganizationEmailType.NEW_ORGANIZATION,
              emailAddress,
              templateDataModel,
              Locale.ENGLISH,
              ccAddresses);
    } catch (TemplateException tEx) {
      throw new IOException(tEx);
    }
    return baseEmailModel;
  }

  /**
   * Generate an email to inform a new organization was endorsed. The returning list includes an
   * email to helpdesk and one to the contact who submitted the request.
   *
   * @param newOrganization new organization data
   * @param endorsingNode organization endorsing node
   * @return the list of {@link BaseEmailModel} to send.
   */
  public List<BaseEmailModel> generateOrganizationEndorsedEmailModel(
      Organization newOrganization, Node endorsingNode) throws IOException {
    Objects.requireNonNull(newOrganization, "newOrganization shall be provided");
    Objects.requireNonNull(newOrganization.getKey(), "newOrganization key shall be present");
    LOG.debug(
        "Organization key: {}; Endorsing node key: {}",
        newOrganization.getKey(),
        endorsingNode.getKey());

    List<BaseEmailModel> baseEmailModelList = new ArrayList<>();
    URL organizationUrl = generateOrganizationUrl(newOrganization.getKey());

    OrganizationTemplateDataModel templateDataModel =
        OrganizationTemplateDataModel.buildEndorsedModel(
            HELPDESK_NAME, newOrganization, organizationUrl, endorsingNode);
    LOG.debug("Name: {}; Organization url: {}", HELPDESK_NAME, organizationUrl);

    try {
      baseEmailModelList.add(
          emailTemplateProcessors.buildEmail(
              OrganizationEmailType.ENDORSEMENT_CONFIRMATION,
              organizationMailConfigProperties.getHelpdesk(),
              templateDataModel,
              Locale.ENGLISH));

      Optional<Contact> pointOfContact =
          newOrganization.getContacts().stream()
              .filter(c -> ContactType.POINT_OF_CONTACT == c.getType())
              .findFirst();
      Optional<String> pointOfContactEmail =
          pointOfContact.map(Contact::getEmail).orElse(Collections.emptyList()).stream()
              .findFirst();

      if (pointOfContactEmail.isPresent()) {
        String completeName = pointOfContact.get().computeCompleteName();
        String email = pointOfContactEmail.get();
        LOG.debug("Point of contact complete name: {}, email: {}", completeName, email);

        templateDataModel =
            OrganizationTemplateDataModel.buildEndorsedModel(
                completeName, newOrganization, organizationUrl, endorsingNode);
        baseEmailModelList.add(
            emailTemplateProcessors.buildEmail(
                OrganizationEmailType.ENDORSEMENT_CONFIRMATION,
                email,
                templateDataModel,
                Locale.ENGLISH));
      } else {
        LOG.debug("Point of contact email is not present!");
      }
    } catch (TemplateException tEx) {
      throw new IOException(tEx);
    }
    return baseEmailModelList;
  }

  public BaseEmailModel generateOrganizationPasswordReminderEmailModel(
      Organization organization, Contact contact, String emailAddress) throws IOException {
    BaseEmailModel baseEmailModel;
    OrganizationPasswordReminderTemplateDataModel templateDataModel =
        new OrganizationPasswordReminderTemplateDataModel(
            contact.getFirstName(),
            URI.create("https://gbif.org").toURL(),
            organization,
            contact,
            emailAddress,
            mailConfigProperties.getCc());

    try {
      // if true, send mails to disposable email address service
      if (mailConfigProperties.getDevemail().getEnabled()) {
        baseEmailModel =
            emailTemplateProcessors.buildEmail(
                OrganizationEmailType.PASSWORD_REMINDER,
                mailConfigProperties.getDevemail().getAddress(),
                templateDataModel,
                Locale.ENGLISH,
                organization.getTitle());
      } else {
        baseEmailModel =
            emailTemplateProcessors.buildEmail(
                OrganizationEmailType.PASSWORD_REMINDER,
                emailAddress,
                templateDataModel,
                Locale.ENGLISH,
                mailConfigProperties.getCc(),
                organization.getTitle());
      }
    } catch (TemplateException tEx) {
      throw new IOException(tEx);
    }

    return baseEmailModel;
  }

  /** Generates (from a url template) the URL to visit an organization page. */
  private URL generateOrganizationUrl(UUID organizationKey) throws MalformedURLException {
    return new URL(
        MessageFormat.format(
            organizationMailConfigProperties.getUrlTemplate().getOrganization(),
            organizationKey.toString()));
  }

  /** Generates (from a url template) the URL to visit in order to endorse an organization. */
  private URL generateEndorsementUrl(UUID organizationKey, UUID confirmationKey)
      throws MalformedURLException {
    return new URL(
        MessageFormat.format(
            organizationMailConfigProperties.getUrlTemplate().getConfirmOrganization(),
            organizationKey.toString(),
            confirmationKey.toString()));
  }
}
