package org.gbif.registry.ws.surety;

import org.gbif.api.model.ChallengeCode;
import org.gbif.api.model.directory.NodePerson;
import org.gbif.api.model.directory.Participant;
import org.gbif.api.model.directory.Person;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.directory.NodeService;
import org.gbif.api.service.directory.ParticipantService;
import org.gbif.api.service.directory.PersonService;
import org.gbif.api.vocabulary.directory.NodePersonRole;
import org.gbif.registry.directory.DirectoryRegistryMapping;
import org.gbif.registry.persistence.ChallengeCodeManager;
import org.gbif.registry.persistence.WithMyBatis;
import org.gbif.registry.persistence.mapper.NodeMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.surety.SuretyConstants;
import org.gbif.registry.surety.email.BaseEmailModel;
import org.gbif.registry.surety.email.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * {@link OrganizationEndorsementService} implementation responsible to handle the business logic of creating and
 * confirming the endorsement of an {@link Organization}.
 */
@Qualifier("organizationEmailEndorsementService")
@Service
class OrganizationEmailEndorsementService implements OrganizationEndorsementService<UUID> {

  private static final Logger LOG = LoggerFactory.getLogger(OrganizationEmailEndorsementService.class);

  private final OrganizationMapper organizationMapper;
  private final NodeMapper nodeMapper;
  private final ParticipantService participantService;
  private final PersonService personService;
  private final NodeService directoryNodeService;
  private final ChallengeCodeManager<UUID> challengeCodeManager;
  private final OrganizationEmailManager emailTemplateManager;
  private final EmailSender emailManager;
  private final WithMyBatis withMyBatis;

  // TODO: 2019-08-22 it should be carefully analyzed: projects directory-ws-client (directory) and gbif-common-ws
  // TODO: 2019-08-22 implement test mocks for now
  public OrganizationEmailEndorsementService(OrganizationMapper organizationMapper,
                                             NodeMapper nodeMapper,
                                             ParticipantService participantService,
                                             NodeService directoryNodeService,
                                             PersonService personService,
                                             ChallengeCodeManager<UUID> challengeCodeManager,
                                             OrganizationEmailManager emailTemplateManager,
                                             @Qualifier("emailSender") EmailSender emailManager,
                                             WithMyBatis withMyBatis) {
    this.organizationMapper = organizationMapper;
    this.nodeMapper = nodeMapper;
    this.participantService = participantService;
    this.directoryNodeService = directoryNodeService;
    this.personService = personService;
    this.challengeCodeManager = challengeCodeManager;
    this.emailTemplateManager = emailTemplateManager;
    this.emailManager = emailManager;
    this.withMyBatis = withMyBatis;
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
    Optional<Person> nodeManager = getNodeContact(endorsingNode);
    try {
      BaseEmailModel emailModel = emailTemplateManager.generateOrganizationEndorsementEmailModel(
          newOrganization, nodeManager.orElse(null), challengeCode.getCode(), endorsingNode);
      emailManager.send(emailModel);
    } catch (IOException ex) {
      LOG.error(SuretyConstants.NOTIFY_ADMIN,
          "Error while trying to generate email on new organization created:" + newOrganization.getKey(), ex);
    }

  }

  /**
   * Confirm the endorsement of an organization using, optionally, a challengeCode.
   * If a challengeCode is provided, it shall be the expected one.
   * If no challengeCode is provided, the organization endorsement will be approved without verification.
   * The caller is responsible to determine if a challengeCode should be used or not.
   *
   * @param organizationKey
   * @param challengeCode
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
      withMyBatis.update(organizationMapper, organization);

      Node endorsingNode = nodeMapper.get(organization.getEndorsingNodeKey());
      try {
        List<BaseEmailModel> emailModel = emailTemplateManager.generateOrganizationEndorsedEmailModel(organization, endorsingNode);
        emailModel.forEach(emailManager::send);
      } catch (IOException ex) {
        LOG.error(SuretyConstants.NOTIFY_ADMIN,
            "Error while trying to generate email on organization confirmed: " + organizationKey, ex);
      }
      return true;
    }
    return false;
  }

  /**
   * Get the Node Manager based on a registry {@link Node}.
   *
   * @param endorsingNode
   * @return
   */
  private Optional<Person> getNodeContact(Node endorsingNode) {
    if (endorsingNode == null) {
      return Optional.empty();
    }
    Optional<Person> nodeManager = Optional.empty();
    Integer participantId = DirectoryRegistryMapping.findParticipantID(endorsingNode);
    if (participantId != null) {
      Participant participant = participantService.get(participantId);
      List<org.gbif.api.model.directory.Node> participantNodes = participant.getNodes();
      Optional<NodePerson> nodePeople = participantNodes.stream()
          .map(pn -> directoryNodeService.get(pn.getId()).getPeople())
          .flatMap(List::stream)
          .filter(p -> NodePersonRole.NODE_MANAGER == p.getRole())
          .findFirst();
      nodeManager = nodePeople.map(np -> personService.get(np.getPersonId()));
    }
    return nodeManager;
  }
}
