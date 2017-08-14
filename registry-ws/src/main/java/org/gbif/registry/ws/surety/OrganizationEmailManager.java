package org.gbif.registry.ws.surety;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.registry.surety.email.BaseEmailModel;
import org.gbif.registry.surety.email.EmailTemplateProcessor;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import freemarker.template.TemplateException;
import org.apache.commons.lang3.StringUtils;

/**
 * Manager handling the different types of email related to organization endorsement.
 * Responsibilities (with the help of (via {@link EmailTemplateProcessor}):
 * - decide where to send the email (which address)
 * - generate the body of the email
 */
class OrganizationEmailManager {

  private final EmailTemplateProcessor endorsementEmailTemplateProcessors;
  private final EmailTemplateProcessor endorsedEmailTemplateProcessors;
  private final OrganizationEmailConfiguration config;

  private static final String HELPDESK_NAME = "Helpdesk";

  /**
   * @param endorsementEmailTemplateProcessors configured EmailTemplateProcessor
   * @param endorsedEmailTemplateProcessors    configured EmailTemplateProcessor
   * @param config
   */
  OrganizationEmailManager(EmailTemplateProcessor endorsementEmailTemplateProcessors,
                           EmailTemplateProcessor endorsedEmailTemplateProcessors,
                           OrganizationEmailConfiguration config) {
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
   * @param nodeManagerContact the {@link Contact} representing the NodeManager or null if there is none
   * @param confirmationKey
   * @param endorsingNode
   *
   * @return the {@link BaseEmailModel} or null if the model can not be generated
   */
  BaseEmailModel generateOrganizationEndorsementEmailModel(Organization newOrganization,
                                                           Contact nodeManagerContact,
                                                           UUID confirmationKey,
                                                           Node endorsingNode) throws IOException {
    Objects.requireNonNull(newOrganization, "newOrganization shall be provided");
    Objects.requireNonNull(confirmationKey, "confirmationKey shall be provided");
    Objects.requireNonNull(endorsingNode, "endorsingNode shall be provided");

    Optional<String> nodeManagerEmailAddress =
            Optional.ofNullable(nodeManagerContact)
                    .map(Contact::getEmail)
                    .flatMap(emails -> emails.stream().findFirst());

    String name = HELPDESK_NAME;
    String emailAddress = config.getHelpdeskEmail();
    // do we have an email to contact the node manager ?
    if (nodeManagerEmailAddress.isPresent()) {
      name = Optional.ofNullable(StringUtils.trimToNull(nodeManagerContact.computeCompleteName()))
              .orElse(endorsingNode.getTitle());
      emailAddress = nodeManagerEmailAddress.get();
    }

    BaseEmailModel baseEmailModel;
    try {
      URL endorsementUrl = config.generateEndorsementUrl(newOrganization.getKey(), confirmationKey);
      OrganizationTemplateDataModel templateDataModel = new OrganizationTemplateDataModel(name, endorsementUrl,
              newOrganization, endorsingNode, nodeManagerEmailAddress.isPresent());
      baseEmailModel = endorsementEmailTemplateProcessors.buildEmail(emailAddress, templateDataModel, Locale.ENGLISH,
              //CC helpdesk unless we are sending the email to helpdesk
              Optional.ofNullable(emailAddress)
                      .filter(e -> !e.equals(config.getHelpdeskEmail()))
                      .map(e -> Collections.singletonList(config.getHelpdeskEmail())).orElse(null));
    } catch (TemplateException tEx) {
      throw new IOException(tEx);
    }
    return baseEmailModel;
  }

  /**
   * Generate an email to inform a new organization was endorsed.
   * The returning list includes an email to helpdesk and one to the contact who submitted the request.
   *
   *
   * @param newOrganization
   * @param endorsingNode
   *
   * @return the list of {@link BaseEmailModel} to send.
   */
  List<BaseEmailModel> generateOrganizationEndorsedEmailModel(Organization newOrganization, Node endorsingNode) throws IOException {
    List<BaseEmailModel> baseEmailModelList = new ArrayList<>();
    OrganizationTemplateDataModel templateDataModel = new OrganizationTemplateDataModel(HELPDESK_NAME, null,
            newOrganization, endorsingNode);

    try {
      baseEmailModelList.add(endorsedEmailTemplateProcessors.buildEmail(config.getHelpdeskEmail(), templateDataModel, Locale.ENGLISH));
      Optional<String> pointOfContactEmail = newOrganization.getContacts()
              .stream()
              .filter(c -> ContactType.POINT_OF_CONTACT == c.getType())
              .findFirst()
              .map(Contact::getEmail)
              .orElse(Collections.emptyList())
              .stream().findFirst();

      if (pointOfContactEmail.isPresent()) {
        baseEmailModelList.add(endorsedEmailTemplateProcessors.buildEmail(pointOfContactEmail.get(), templateDataModel, Locale.ENGLISH));
      }
    }
    catch (TemplateException tEx){
      throw new IOException(tEx);
    }
    return baseEmailModelList;
  }

}
