/*
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
package org.gbif.registry.cli.directoryupdate;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.directory.Node;
import org.gbif.api.model.directory.Participant;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.service.directory.NodeService;
import org.gbif.api.service.directory.ParticipantService;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.cli.common.DirectoryRegistryMapping;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.NodeMapper;
import org.gbif.registry.service.WithMyBatis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

/**
 * Main logic Only a limited set of fields are updated see {@link #shouldUpdateRegistryNode} and
 * {@link #fillRegistryNode}
 */
@Component
public class DirectoryUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(DirectoryUpdater.class);

  private static final int DEFAULT_MAX_LIMIT = 1000;
  private static final String DIRECTORY_UPDATE_USER = "directory-update-robot";

  private ParticipantService directoryParticipantService;
  private NodeService directoryNodeService;

  private NodeMapper nodeMapper;
  private IdentifierMapper identifierMapper;
  private WithMyBatis withMyBatis;

  public DirectoryUpdater(
      ParticipantService directoryParticipantService,
      NodeService directoryNodeService,
      NodeMapper nodeMapper,
      IdentifierMapper identifierMapper,
      WithMyBatis withMyBatis) {
    this.directoryParticipantService = directoryParticipantService;
    this.directoryNodeService = directoryNodeService;
    this.nodeMapper = nodeMapper;
    this.identifierMapper = identifierMapper;
    this.withMyBatis = withMyBatis;
  }

  /** Apply the updates from the Directory to the Registry (if required) */
  public void applyUpdates() {
    // get all participant from the Directory
    PagingResponse<Participant> participants =
        directoryParticipantService.list(null, new PagingRequest(0, DEFAULT_MAX_LIMIT));
    PagingResponse<Node> nodes =
        directoryNodeService.list(null, new PagingRequest(0, DEFAULT_MAX_LIMIT));

    Map<Integer, Participant> participantsById = new HashMap<>();
    Map<Integer, Node> nodeByParticipantsId = new HashMap<>();
    for (Participant participant : participants.getResults()) {
      participantsById.put(participant.getId(), participant);
    }

    for (Node node : nodes.getResults()) {
      if (node.getParticipantId() != null) {
        nodeByParticipantsId.put(node.getParticipantId(), node);
      } else {
        LOG.warn("Directory Node {} without link to a Participant skipped.", node.getId());
      }
    }

    List<org.gbif.api.model.registry.Node> registryNodes =
        nodeMapper.list(new PagingRequest(0, DEFAULT_MAX_LIMIT));
    Integer participantId;
    Participant participant;
    Node directoryNode;
    Set<Integer> participantFoundInRegistry = new HashSet<>();
    for (org.gbif.api.model.registry.Node registryNode : registryNodes) {
      participantId = findParticipantId(registryNode);
      if (participantId != null) {
        participant = participantsById.get(participantId);
        directoryNode = nodeByParticipantsId.get(participantId);
        participantFoundInRegistry.add(participantId);
        if (participant != null) {
          if (shouldUpdateRegistryNode(registryNode, participant, directoryNode)) {
            updateRegistryNode(registryNode, participant, directoryNode);
          }
        } else {
          LOG.warn("Can't find participantId {} in the Directory", participantId);
        }
      }
    }

    // create Node in the Registry if present in the Directory
    // copy keys (participantId)
    for (Integer remainingParticipantId :
        Sets.difference(participantsById.keySet(), participantFoundInRegistry)) {
      participant = participantsById.get(remainingParticipantId);
      directoryNode = nodeByParticipantsId.get(remainingParticipantId);
      createRegistryNode(participant, directoryNode);
    }
  }

  /** Create a Registry Node (in the database) from a Directory Participant and Node. */
  private void createRegistryNode(Participant participant, @Nullable Node directoryNode) {

    org.gbif.api.model.registry.Node registryNode =
        fillRegistryNode(new org.gbif.api.model.registry.Node(), participant, directoryNode);
    registryNode.setCreatedBy(DIRECTORY_UPDATE_USER);

    UUID registryNodeKey = withMyBatis.create(nodeMapper, registryNode);

    Identifier registryIdentifier = new Identifier();
    registryIdentifier.setType(IdentifierType.GBIF_PARTICIPANT);
    registryIdentifier.setIdentifier(participant.getId().toString());
    registryIdentifier.setCreatedBy(DIRECTORY_UPDATE_USER);

    withMyBatis.addIdentifier(identifierMapper, nodeMapper, registryNodeKey, registryIdentifier);

    LOG.info(
        "Node {} ({}) created by {}",
        registryNode.getTitle(),
        registryNode.getKey(),
        DIRECTORY_UPDATE_USER);
  }

  /** Update a Registry Node (in the database) from a Directory Participant and Node. */
  private void updateRegistryNode(
      org.gbif.api.model.registry.Node registryNode,
      Participant participant,
      @Nullable Node directoryNode) {
    registryNode = fillRegistryNode(registryNode, participant, directoryNode);
    registryNode.setModifiedBy(DIRECTORY_UPDATE_USER);

    nodeMapper.update(registryNode);
    LOG.info(
        "Node {} ({}) updated by {}",
        registryNode.getTitle(),
        registryNode.getKey(),
        DIRECTORY_UPDATE_USER);
  }

  /**
   * Check if a registry Node requires an update. Must be in sync with {@link
   * #fillRegistryNode(org.gbif.api.model.registry.Node, Participant, Node)}
   */
  private boolean shouldUpdateRegistryNode(
      org.gbif.api.model.registry.Node registryNode,
      Participant participant,
      @Nullable Node directoryNode) {

    String titleFromDirectory =
        directoryNode != null ? directoryNode.getName() : participant.getName();
    return !Objects.equals(registryNode.getTitle(), titleFromDirectory)
        || !Objects.equals(registryNode.getCountry(), participant.getCountryCode())
        || !Objects.equals(
            DirectoryRegistryMapping.PARTICIPATION_STATUS.get(participant.getParticipationStatus()),
            registryNode.getParticipationStatus())
        || !Objects.equals(
            DirectoryRegistryMapping.PARTICIPATION_TYPE.get(participant.getType()),
            registryNode.getType())
        || !Objects.equals(registryNode.getGbifRegion(), participant.getGbifRegion());
  }

  /**
   * Note: registryNode title and abbreviation are set from the Node from the Directory with a
   * fallback on Participant if no Node is available in the Directory. Must be in sync with {@link
   * #shouldUpdateRegistryNode(org.gbif.api.model.registry.Node, Participant, Node)}
   */
  private org.gbif.api.model.registry.Node fillRegistryNode(
      org.gbif.api.model.registry.Node registryNode,
      Participant participant,
      @Nullable Node directoryNode) {
    String titleFromDirectory =
        directoryNode != null ? directoryNode.getName() : participant.getName();
    registryNode.setTitle(titleFromDirectory);
    registryNode.setCountry(participant.getCountryCode());
    registryNode.setParticipationStatus(
        DirectoryRegistryMapping.PARTICIPATION_STATUS.get(participant.getParticipationStatus()));
    registryNode.setType(DirectoryRegistryMapping.PARTICIPATION_TYPE.get(participant.getType()));
    registryNode.setGbifRegion(participant.getGbifRegion());
    registryNode.setContinent(
        DirectoryRegistryMapping.GBIF_REGION_CONTINENT.get(participant.getGbifRegion()));

    return registryNode;
  }

  private static Integer findParticipantId(org.gbif.api.model.registry.Node node) {
    for (Identifier id : node.getIdentifiers()) {
      if (IdentifierType.GBIF_PARTICIPANT == id.getType()) {
        try {
          return Integer.parseInt(id.getIdentifier());
        } catch (NumberFormatException e) {
          LOG.error("Directory participantId is no integer: {}", id.getIdentifier());
        }
      }
    }
    return null;
  }
}
