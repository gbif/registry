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
package org.gbif.registry.ws.surety;

import org.gbif.api.model.ChallengeCode;
import org.gbif.api.model.directory.NodePerson;
import org.gbif.api.model.directory.Participant;
import org.gbif.api.model.directory.Person;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.directory.NodeService;
import org.gbif.api.service.directory.ParticipantService;
import org.gbif.api.service.directory.PersonService;
import org.gbif.api.vocabulary.directory.NodePersonRole;
import org.gbif.registry.directory.DirectoryRegistryMapping;
import org.gbif.registry.domain.mail.BaseEmailModel;
import org.gbif.registry.mail.EmailSender;
import org.gbif.registry.mail.organization.OrganizationEmailManager;
import org.gbif.registry.mail.util.RegistryMailUtils;
import org.gbif.registry.persistence.mapper.NodeMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.persistence.service.MapperServiceLocator;
import org.gbif.registry.surety.ChallengeCodeManager;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * {@link OrganizationEndorsementService} implementation responsible to handle the business logic of
 * creating and confirming the endorsement of an {@link Organization}.
 */
@Qualifier("organizationEmailEndorsementService")
@Service
public class OrganizationEmailEndorsementService implements OrganizationEndorsementService<UUID> {

  private static final Logger LOG =
      LoggerFactory.getLogger(OrganizationEmailEndorsementService.class);

  private final OrganizationMapper organizationMapper;
  private final NodeMapper nodeMapper;
  private final ParticipantService participantService;
  private final PersonService personService;
  private final NodeService directoryNodeService;
  private final ChallengeCodeManager<UUID> challengeCodeManager;
  private final OrganizationEmailManager emailTemplateManager;
  private final EmailSender emailSender;

  public OrganizationEmailEndorsementService(
      MapperServiceLocator mapperServiceLocator,
      ParticipantService participantService,
      NodeService directoryNodeService,
      PersonService personService,
      ChallengeCodeManager<UUID> challengeCodeManager,
      OrganizationEmailManager emailTemplateManager,
      EmailSender emailSender) {
    this.organizationMapper = mapperServiceLocator.getOrganizationMapper();
    this.nodeMapper = mapperServiceLocator.getNodeMapper();
    this.participantService = participantService;
    this.directoryNodeService = directoryNodeService;
    this.personService = personService;
    this.challengeCodeManager = challengeCodeManager;
    this.emailTemplateManager = emailTemplateManager;
    this.emailSender = emailSender;
  }

  /** Handles the logic to generate a new challenge code and send an email to the right person. */
  @Transactional
  @Override
  public void onNewOrganization(Organization newOrganization) {
    ChallengeCode challengeCode = challengeCodeManager.create(newOrganization.getKey());

    // try to get the node manager
    Node endorsingNode = nodeMapper.get(newOrganization.getEndorsingNodeKey());
    Optional<Person> nodeManager = getNodeContact(endorsingNode);
    try {
      BaseEmailModel emailModel =
          emailTemplateManager.generateOrganizationEndorsementEmailModel(
              newOrganization, nodeManager.orElse(null), challengeCode.getCode(), endorsingNode);
      emailSender.send(emailModel);
    } catch (IOException ex) {
      LOG.error(
          RegistryMailUtils.NOTIFY_ADMIN,
          "Error while trying to generate email on new organization created:"
              + newOrganization.getKey(),
          ex);
    }
  }

  /**
   * Confirm the endorsement of an organization using, optionally, a challengeCode. If a
   * challengeCode is provided, it shall be the expected one. If no challengeCode is provided, the
   * organization endorsement will be approved without verification. The caller is responsible to
   * determine if a challengeCode should be used or not.
   *
   * @return the organization endorsement was approved or not
   */
  @Transactional
  @Override
  public boolean confirmEndorsement(UUID organizationKey, @Nullable UUID challengeCode) {
    Organization organization = organizationMapper.get(organizationKey);

    if (organization != null
        && !organization.isEndorsementApproved()
        && (challengeCode == null || challengeCodeManager.isValidChallengeCode(organizationKey, challengeCode))) {
      checkArgument(
          organization.getDeleted() == null,
          "Unable to endorse a previously deleted organization");
      challengeCodeManager.remove(organizationKey);
      organizationMapper.endorse(organizationKey);

      Node endorsingNode = nodeMapper.get(organization.getEndorsingNodeKey());
      try {
        List<BaseEmailModel> emailModel =
            emailTemplateManager.generateOrganizationEndorsedEmailModel(
                organization, endorsingNode);
        emailModel.forEach(emailSender::send);
      } catch (IOException ex) {
        LOG.error(
            RegistryMailUtils.NOTIFY_ADMIN,
            "Error while trying to generate email on organization confirmed: " + organizationKey,
            ex);
      }
      return true;
    }

    return false;
  }

  /**
   * Confirm the endorsement of an organization without a challengeCode.
   *
   * @return the organization endorsement was approved or not
   */
  @Transactional
  @Override
  public boolean confirmEndorsement(UUID organizationKey) {
    return confirmEndorsement(organizationKey, null);
  }

  /**
   * Revoke the endorsement from an organization.
   *
   * @return the organization endorsement was approved or not
   */
  @Transactional
  @Override
  public boolean revokeEndorsement(UUID organizationKey) {
    Organization organization = organizationMapper.get(organizationKey);

    if (organization != null && organization.isEndorsementApproved()) {
      checkArgument(
          organization.getDeleted() == null,
          "Unable to revoke endorsement from a previously deleted organization");
      organizationMapper.revokeEndorsement(organizationKey);
      return true;
    }

    return false;
  }

  /**
   * Handles the logic to generate an organization password reminder and send an email to the right
   * person.
   *
   * @return organization reminder was sent or not
   */
  public boolean passwordReminder(
      Organization organization, Contact contact, @NotNull String emailAddress) {
    try {
      BaseEmailModel emailModel =
          emailTemplateManager.generateOrganizationPasswordReminderEmailModel(
              organization, contact, emailAddress);
      emailSender.send(emailModel);
    } catch (IOException e) {
      LOG.error(
          RegistryMailUtils.NOTIFY_ADMIN,
          "Error while trying to generate email on organization password reminder: "
              + organization.getKey(),
          e);
      return false;
    }

    return true;
  }

  /** Get the Node Manager based on a registry {@link Node}. */
  private Optional<Person> getNodeContact(Node endorsingNode) {
    if (endorsingNode == null) {
      return Optional.empty();
    }
    Optional<Person> nodeManager = Optional.empty();
    Integer participantId = DirectoryRegistryMapping.findParticipantID(endorsingNode);
    if (participantId != null) {
      Participant participant = participantService.get(participantId);
      List<org.gbif.api.model.directory.Node> participantNodes = participant.getNodes();
      Optional<NodePerson> nodePeople =
          participantNodes.stream()
              .map(pn -> directoryNodeService.get(pn.getId()).getPeople())
              .flatMap(List::stream)
              .filter(p -> NodePersonRole.NODE_MANAGER == p.getRole())
              .findFirst();
      nodeManager = nodePeople.map(np -> personService.get(np.getPersonId()));
    }
    return nodeManager;
  }
}
