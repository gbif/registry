package org.gbif.registry.ws.surety;

import freemarker.template.TemplateException;
import org.apache.commons.lang3.StringUtils;
import org.gbif.api.model.directory.Person;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.registry.domain.mail.BaseEmailModel;
import org.gbif.registry.surety.email.EmailTemplateProcessor;
import org.gbif.registry.surety.email.FreemarkerEmailTemplateProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Manager handling the different types of email related to organization endorsement.
 * Responsibilities (with the help of (via {@link FreemarkerEmailTemplateProcessor}):
 * - decide where to send the email (which address)
 * - generate the body of the email
 */
@Service
class OrganizationEmailManager {

  // TODO: 2019-08-22 move to ConfigurationProperties
  @Value("${organization.surety.helpdesk.email}")
  private String helpdeskEmail;

  // TODO: 2019-08-22 add disable\enable functionality
  @Value("${organization.surety.mail.enable}")
  private boolean isEmailEnabled;

  @Value("${organization.surety.mail.urlTemplate.confirmOrganization}")
  private String endorsementUrlTemplate;

  @Value("${organization.surety.mail.urlTemplate.organization}")
  private String organizationUrlTemplate;

  private final EmailTemplateProcessor emailTemplateProcessors;

  private static final String HELPDESK_NAME = "Helpdesk";

  /**
   * @param emailTemplateProcessors configured EmailTemplateProcessor
   */
  OrganizationEmailManager(@Qualifier("organizationEmailTemplateProcessor") EmailTemplateProcessor emailTemplateProcessors) {
    Objects.requireNonNull(emailTemplateProcessors, "emailTemplateProcessors shall be provided");
    this.emailTemplateProcessors = emailTemplateProcessors;
  }

  /**
   * If nodeManagerContact does not contain an email address, the model will be set to send the message to helpdesk.
   *
   * @param newOrganization
   * @param nodeManager     the {@link Person} representing the NodeManager or null if there is none
   * @param confirmationKey
   * @param endorsingNode
   * @return the {@link BaseEmailModel} or null if the model can not be generated
   */
  BaseEmailModel generateOrganizationEndorsementEmailModel(Organization newOrganization,
                                                           Person nodeManager,
                                                           UUID confirmationKey,
                                                           Node endorsingNode) throws IOException {
    Objects.requireNonNull(newOrganization, "newOrganization shall be provided");
    Objects.requireNonNull(confirmationKey, "confirmationKey shall be provided");
    Objects.requireNonNull(endorsingNode, "endorsingNode shall be provided");

    Optional<String> nodeManagerEmailAddress = Optional.ofNullable(nodeManager).map(Person::getEmail);

    String name = HELPDESK_NAME;
    String emailAddress = helpdeskEmail;
    // do we have an email to contact the node manager ?
    if (nodeManagerEmailAddress.isPresent()) {
      name = Optional.ofNullable(StringUtils.trimToNull(
        org.gbif.utils.text.StringUtils.thenJoin(StringUtils::trimToNull, nodeManager.getFirstName(),
          nodeManager.getSurname())))
        .orElse(endorsingNode.getTitle());
      emailAddress = nodeManagerEmailAddress.get();
    }

    BaseEmailModel baseEmailModel;
    try {
      URL endorsementUrl = generateEndorsementUrl(newOrganization.getKey(), confirmationKey);
      OrganizationTemplateDataModel templateDataModel = OrganizationTemplateDataModel
        .buildEndorsementModel(name, endorsementUrl, newOrganization, endorsingNode, nodeManagerEmailAddress.isPresent());

      baseEmailModel = emailTemplateProcessors.buildEmail(OrganizationEmailType.NEW_ORGANIZATION, emailAddress, templateDataModel, Locale.ENGLISH,
        //CC helpdesk unless we are sending the email to helpdesk
        Optional.ofNullable(emailAddress)
          .filter(e -> !e.equals(helpdeskEmail))
          .map(e -> Collections.singletonList(helpdeskEmail)).orElse(null));
    } catch (TemplateException tEx) {
      throw new IOException(tEx);
    }
    return baseEmailModel;
  }

  /**
   * Generate an email to inform a new organization was endorsed.
   * The returning list includes an email to helpdesk and one to the contact who submitted the request.
   *
   * @param newOrganization
   * @param endorsingNode
   * @return the list of {@link BaseEmailModel} to send.
   */
  List<BaseEmailModel> generateOrganizationEndorsedEmailModel(Organization newOrganization, Node endorsingNode) throws IOException {
    List<BaseEmailModel> baseEmailModelList = new ArrayList<>();
    URL organizationUrl = generateOrganizationUrl(newOrganization.getKey());

    OrganizationTemplateDataModel templateDataModel = OrganizationTemplateDataModel.
      buildEndorsedModel(HELPDESK_NAME, newOrganization, organizationUrl, endorsingNode);

    try {
      baseEmailModelList.add(emailTemplateProcessors.buildEmail(OrganizationEmailType.ENDORSEMENT_CONFIRMATION, helpdeskEmail, templateDataModel, Locale.ENGLISH));

      Optional<Contact> pointOfContact = newOrganization.getContacts()
        .stream()
        .filter(c -> ContactType.POINT_OF_CONTACT == c.getType())
        .findFirst();
      Optional<String> pointOfContactEmail = pointOfContact
        .map(Contact::getEmail)
        .orElse(Collections.emptyList())
        .stream().findFirst();

      if (pointOfContactEmail.isPresent()) {
        templateDataModel = OrganizationTemplateDataModel.
          buildEndorsedModel(pointOfContact.isPresent() ? pointOfContact.get().computeCompleteName() : "",
            newOrganization, organizationUrl, endorsingNode);
        baseEmailModelList.add(emailTemplateProcessors.buildEmail(OrganizationEmailType.ENDORSEMENT_CONFIRMATION, pointOfContactEmail.get(), templateDataModel, Locale.ENGLISH));
      }
    } catch (TemplateException tEx) {
      throw new IOException(tEx);
    }
    return baseEmailModelList;
  }

  /**
   * Generates (from a url template) the URL to visit an organization page.
   */
  private URL generateOrganizationUrl(UUID organizationKey) throws MalformedURLException {
    return new URL(MessageFormat.format(organizationUrlTemplate, organizationKey.toString()));
  }

  /**
   * Generates (from a url template) the URL to visit in order to endorse an organization.
   */
  private URL generateEndorsementUrl(UUID organizationKey, UUID confirmationKey) throws MalformedURLException {
    return new URL(MessageFormat.format(endorsementUrlTemplate, organizationKey.toString(), confirmationKey.toString()));
  }
}
