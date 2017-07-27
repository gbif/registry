package org.gbif.registry.ws.surety;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.registry.surety.email.BaseEmailModel;
import org.gbif.registry.surety.email.EmailTemplateProcessor;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import freemarker.template.TemplateException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager handling the different types of email related to organization endorsement.
 * 
 * Responsibilities (with the help of (via {@link EmailTemplateProcessor}):
 *  - decide where to send the email (which address)
 *  - generate the body of the email
 *
 */
class OrganizationEmailTemplateManager {

  private static final Logger LOG = LoggerFactory.getLogger(OrganizationEmailTemplateManager.class);

  private final EmailTemplateProcessor endorsementEmailTemplateProcessors;
  private final EmailTemplateProcessor endorsedEmailTemplateProcessors;
  private final OrganizationEmailTemplateConfiguration config;

  private static final String HELPDESK_NAME = "Helpdesk";

  /**
   *
   * @param endorsementEmailTemplateProcessors configured EmailTemplateProcessor
   * @param endorsedEmailTemplateProcessors configured EmailTemplateProcessor
   * @param config
   */
  OrganizationEmailTemplateManager(EmailTemplateProcessor endorsementEmailTemplateProcessors,
                                   EmailTemplateProcessor endorsedEmailTemplateProcessors,
                                   OrganizationEmailTemplateConfiguration config) {
    Objects.requireNonNull(endorsementEmailTemplateProcessors, "endorsementEmailTemplateProcessors shall be provided");
    Objects.requireNonNull(endorsedEmailTemplateProcessors, "endorsedEmailTemplateProcessors shall be provided");
    Objects.requireNonNull(config, "configuration email shall be provided");

    this.endorsementEmailTemplateProcessors = endorsementEmailTemplateProcessors;
    this.endorsedEmailTemplateProcessors = endorsedEmailTemplateProcessors;
    this.config = config;
  }

  /**
   * If nodeManagerContact does not contain an email address, the model will be set to send the message to helpdesk.
   *
   * @param newOrganization
   * @param nodeManagerContact
   * @param confirmationKey
   * @param endorsingNode
   *
   * @return new {@link BaseEmailModel} or null if an error occurred
   */
  BaseEmailModel generateNewOrganizationEmailModel(Organization newOrganization,
                                                          Contact nodeManagerContact,
                                                          UUID confirmationKey,
                                                          Node endorsingNode) {
    Objects.requireNonNull(newOrganization, "newOrganization shall be provided");
    Objects.requireNonNull(endorsingNode, "endorsingNode shall be provided");

    BaseEmailModel baseEmailModel = null;
    Optional<String> contactEmailAddress =
      Optional.ofNullable(nodeManagerContact)
              .map(Contact::getEmail)
              .flatMap( emails -> emails.stream().findFirst());

    String name = HELPDESK_NAME;
    String emailAddress = config.getHelpdeskEmail();
    // do we have an email to contact the node manager ?
    if (contactEmailAddress.isPresent()) {
      name = Optional.ofNullable(StringUtils.trimToNull(nodeManagerContact.computeCompleteName()))
              .orElse(endorsingNode.getTitle());
      emailAddress = contactEmailAddress.get();
    }

    try {
      URL endorsementUrl = config.generateEndorsementUrl(newOrganization.getKey(), confirmationKey);
      OrganizationTemplateDataModel templateDataModel = new OrganizationTemplateDataModel(name, endorsementUrl,
              newOrganization, endorsingNode);
      baseEmailModel = endorsementEmailTemplateProcessors.buildEmail(emailAddress, templateDataModel, Locale.ENGLISH);
    } catch (TemplateException | IOException ex) {
      LOG.error("Error while trying to generate email to confirm organization " + newOrganization.getKey(), ex);
    }
    return baseEmailModel;
  }

  /**
   * Generate an email to helpdesk to inform a new organization was confirmed.
   * @param newOrganization
   * @param endorsingNode
   * @return
   */
  BaseEmailModel generateOrganizationConfirmedEmailModel(Organization newOrganization, Node endorsingNode) {
    BaseEmailModel baseEmailModel = null;
    OrganizationTemplateDataModel templateDataModel = new OrganizationTemplateDataModel(HELPDESK_NAME, null,
            newOrganization, endorsingNode);
    try {
      baseEmailModel = endorsedEmailTemplateProcessors.buildEmail(config.getHelpdeskEmail(), templateDataModel, Locale.ENGLISH);
    } catch (TemplateException | IOException ex) {
      LOG.error("Error while trying to generate email on organization confirmed" + newOrganization.getKey(), ex);
    }
    return baseEmailModel;
  }

}
