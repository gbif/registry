package org.gbif.registry.ws.surety;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.registry.persistence.WithMyBatis;
import org.gbif.registry.persistence.mapper.NodeMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.surety.email.BaseEmailModel;
import org.gbif.registry.surety.email.EmailManager;
import org.gbif.registry.surety.model.ChallengeCode;
import org.gbif.registry.surety.persistence.ChallengeCodeManager;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.mybatis.guice.transactional.Transactional;

/**
 * {@link OrganizationEndorsementService} implementation responsible to handle the business logic of creating and
 * confirming the endorsement of an {@link Organization}.
 */
class OrganizationEmailEndorsementService implements OrganizationEndorsementService<UUID> {

  static final String ENDORSEMENT_EMAIL_MANAGER_KEY = "endorsementEmailManager";

  private final OrganizationMapper organizationMapper;
  private final NodeMapper nodeMapper;
  private final ChallengeCodeManager<UUID> challengeCodeManager;

  private final OrganizationEmailManager emailTemplateManager;
  private final EmailManager emailManager;

  @Inject
  public OrganizationEmailEndorsementService(OrganizationMapper organizationMapper,
                                             NodeMapper nodeMapper,
                                             ChallengeCodeManager<UUID> challengeCodeManager,
                                             OrganizationEmailManager emailTemplateManager,
                                             @Named(ENDORSEMENT_EMAIL_MANAGER_KEY) EmailManager emailManager) {
    this.organizationMapper = organizationMapper;
    this.nodeMapper = nodeMapper;
    this.challengeCodeManager = challengeCodeManager;
    this.emailTemplateManager = emailTemplateManager;
    this.emailManager = emailManager;
  }

  /**
   * Handles the logic to generate a new challenge code and send an email to the right person.
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

    BaseEmailModel emailModel = emailTemplateManager.generateOrganizationEndorsementEmailModel(
            newOrganization, nodeManager.orElse(null), challengeCode.getCode(), endorsingNode);
    emailManager.send(emailModel);
  }

  /**
   * Confirm the endorsement of an organization using, optionally, a challengeCode.
   * If a challengeCode is provided, it shall be the expected one.
   * If no challengeCode is provided, the organisation endorsement will be approved without verification.
   * The caller is responsible to determine if a challengeCode should be used or not.
   *
   * @param organizationKey
   * @param challengeCode
   *
   * @return the organization endorsement was approved or not
   */
  @Transactional
  @Override
  public boolean confirmEndorsement(UUID organizationKey, @Nullable UUID challengeCode) {
    Organization organization = organizationMapper.get(organizationKey);

    if (organization != null && !organization.isEndorsementApproved() &&
            (challengeCode == null || challengeCodeManager.isValidChallengeCode(organizationKey, challengeCode)) &&
            challengeCodeManager.remove(organizationKey)) {

      organization.setEndorsementApproved(true);
      WithMyBatis.update(organizationMapper, organization);

      Node endorsingNode = nodeMapper.get(organization.getEndorsingNodeKey());
      BaseEmailModel emailModel = emailTemplateManager.generateOrganizationEndorsedEmailModel(organization, endorsingNode);
      emailManager.send(emailModel);
      return true;
    }
    return false;
  }

}
