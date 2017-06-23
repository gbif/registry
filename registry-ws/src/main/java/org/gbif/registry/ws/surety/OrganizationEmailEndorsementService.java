package org.gbif.registry.ws.surety;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.registry.persistence.mapper.NodeMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.surety.email.BaseEmailModel;
import org.gbif.registry.surety.email.EmailManager;
import org.gbif.registry.surety.model.ChallengeCode;
import org.gbif.registry.surety.persistence.ChallengeCodeManager;

import java.util.Optional;
import java.util.UUID;

import com.google.inject.Inject;
import org.mybatis.guice.transactional.Transactional;

/**
 * {@link OrganizationEndorsementService} implementation responsible to handle the business logic of creating and
 * confirming {@link Organization}
 */
public class OrganizationEmailEndorsementService implements OrganizationEndorsementService<UUID> {

  private final OrganizationMapper organizationMapper;
  private final NodeMapper nodeMapper;
  private final ChallengeCodeManager<UUID> challengeCodeManager;

  private final OrganizationEmailTemplateProcessor emailTemplateProcessor;
  private final EmailManager emailManager;

  @Inject
  public OrganizationEmailEndorsementService(OrganizationMapper organizationMapper,
                                             NodeMapper nodeMapper,
                                             ChallengeCodeManager<UUID> challengeCodeManager,
                                             OrganizationEmailTemplateProcessor emailTemplateProcessor,
                                             EmailManager emailManager) {
    this.organizationMapper = organizationMapper;
    this.nodeMapper = nodeMapper;
    this.challengeCodeManager = challengeCodeManager;
    this.emailTemplateProcessor = emailTemplateProcessor;
    this.emailManager = emailManager;
  }

  /**
   * Handle to logic to generate a new challenge code and send an email to the right person.
   *
   * @param newOrganization
   */
  @Transactional
  @Override
  public void onNewOrganization(Organization newOrganization) {
    ChallengeCode challengeCode = challengeCodeManager.create(newOrganization.getKey());

    //try to get the node manager
    Node endorsingNode = nodeMapper.get(newOrganization.getEndorsingNodeKey());
    Optional<Contact> nodeManager =
            endorsingNode.getContacts().stream().filter(c -> c.getType() == ContactType.NODE_MANAGER).findFirst();


    BaseEmailModel emailModel = emailTemplateProcessor.generateNewOrganizationEmailModel(newOrganization.getKey(),
            nodeManager.orElse(null), challengeCode);
    emailManager.send(emailModel);
  }

  @Transactional
  @Override
  public boolean confirmOrganization(UUID organizationKey, UUID challengeCode) {
    Organization organization = organizationMapper.get(organizationKey);
    if (organization != null && !organization.isEndorsementApproved() &&
            challengeCodeManager.isValidChallengeCode(organizationKey, challengeCode) &&
            challengeCodeManager.remove(organizationKey)) {
      organization.setEndorsementApproved(true);
      return true;
    }
    return false;
  }

}
