package org.gbif.registry.ws.surety;

import org.gbif.api.model.registry.Contact;
import org.gbif.registry.surety.email.BaseEmailModel;
import org.gbif.registry.surety.email.BaseTemplateDataModel;
import org.gbif.registry.surety.email.EmailTemplateProcessor;
import org.gbif.registry.surety.model.ChallengeCode;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.UUID;

import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Specialized EmailTemplate processor for Organization.
 * Responsibilities (with the help of (via {@link EmailTemplateProcessor}):
 *  - decide where to send the email (which address)
 *  - generate the subject of the email
 *  - generate the body of the email
 */
public class OrganizationEmailTemplateProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(OrganizationEmailTemplateProcessor.class);

  private final EmailTemplateProcessor emailTemplateProcessor;
  private final String urlTemplate;
  private final String helpdeskEmail;
  private static final String HELPDESK_NAME = "Helpdesk";

  public OrganizationEmailTemplateProcessor(EmailTemplateProcessor emailTemplateProcessor, String urlTemplate,
                                             String helpdeskEmail) {
    Objects.requireNonNull(emailTemplateProcessor, "emailTemplateProcessor shall be provided");
    Objects.requireNonNull(urlTemplate, "urlTemplate shall be provided");
    Objects.requireNonNull(helpdeskEmail, "helpdesk email shall be provided");

    this.emailTemplateProcessor = emailTemplateProcessor;
    this.urlTemplate = urlTemplate;
    this.helpdeskEmail = helpdeskEmail;
  }

  /**
   * If nodeManagerContact does not contain an email address, the model will be set to send the message to helpdesk.
   * @param organizationKey
   * @param nodeManagerContact
   * @param challengeCode
   * @return new {@link BaseEmailModel} or null if an error occurred
   */
  public BaseEmailModel generateNewOrganizationEmailModel(UUID organizationKey, Contact nodeManagerContact, ChallengeCode challengeCode) {
    BaseEmailModel baseEmailModel = null;

    String emailAddress;
    String name;
    if(nodeManagerContact == null || nodeManagerContact.getEmail() == null || nodeManagerContact.getEmail().isEmpty()) {
      name = HELPDESK_NAME;
      emailAddress = helpdeskEmail;
    }
    else{
      name = nodeManagerContact.getFirstName() + " " + nodeManagerContact.getLastName();
      emailAddress = nodeManagerContact.getEmail().get(0).trim();
    }

    try {
      URL url = new URL(MessageFormat.format(urlTemplate, organizationKey.toString(), challengeCode.getCode().toString()));
      BaseTemplateDataModel templateDataModel = new BaseTemplateDataModel(name, url);
      baseEmailModel = emailTemplateProcessor.buildEmail(emailAddress, templateDataModel);
    } catch (TemplateException | IOException ex) {
      LOG.error("Error while trying to send email to confirm organization " + organizationKey, ex);
    }
    return baseEmailModel;
  }

//  public BaseEmailModel generateNewOrganizationConfirmationEmailModel() {
//
//  }

}
