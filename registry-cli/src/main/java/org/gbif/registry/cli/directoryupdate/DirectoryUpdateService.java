package org.gbif.registry.cli.directoryupdate;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.directory.Node;
import org.gbif.api.model.directory.Participant;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.service.directory.ParticipantService;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.directory.DirectoryRegistryConstantsMapping;
import org.gbif.registry.persistence.mapper.NodeMapper;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Injector;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.LocalTime;
import org.threeten.bp.temporal.ChronoUnit;

/**
 *
 * Service that will periodically get the list of participants and nodes from the Directory and update the registry
 * data if required.
 * This service will NOT remove nodes that are not available in the Directory.
 *
 * Only a limited set of fields are updated see {@link #shouldUpdateNode} and {@link #updateRegistryNode}
 */
public class DirectoryUpdateService extends AbstractIdleService {

  private static final Logger LOG = LoggerFactory.getLogger(DirectoryUpdateService.class);
  private static final String DIRECTORY_UPDATE_USER = "directory-update-robot";
  //avoid using 0
  private static final int DEFAULT_START_HOUR = 5;
  private static final int DEFAULT_START_MINUTE = 34;
  private static final int DEFAULT_FREQUENCY = 24;

  private static final int DEFAULT_MAX_LIMIT = 1000;
  private final Integer frequencyInHour;
  private final Integer startHour;
  private final Integer startMinute;

  private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  private ParticipantService directoryParticipantService;
  private org.gbif.api.service.directory.NodeService directoryNodeService;

  private NodeMapper nodeMapper;

  public DirectoryUpdateService(DirectoryUpdateConfiguration cfg) {
    Injector directoryUpdaterInj = cfg.createInjector();
    directoryParticipantService = directoryUpdaterInj.getInstance(ParticipantService.class);
    directoryNodeService = directoryUpdaterInj.getInstance(org.gbif.api.service.directory.NodeService.class);

    Injector registryInj = cfg.createMyBatisInjector();
    nodeMapper = registryInj.getInstance(NodeMapper.class);

    this.frequencyInHour = ObjectUtils.defaultIfNull(cfg.frequencyInHour, DEFAULT_FREQUENCY);

    if(StringUtils.contains(cfg.startTime, ":")) {
      String[] timeParts = cfg.startTime.split(":");
      startHour = NumberUtils.toInt(timeParts[0], DEFAULT_START_HOUR);
      startMinute = NumberUtils.toInt(timeParts[1], DEFAULT_START_MINUTE);
    }
    else{
      startHour = null;
      startMinute = null;
    }
  }

  @Override
  protected void startUp() throws Exception {
    long initialDelay = 0;
    if(startHour != null && startMinute != null) {
      LocalTime t = LocalTime.of(startHour, startMinute);
      initialDelay = LocalTime.now().until(t, ChronoUnit.MINUTES);
    }

    //if the delay is passed,
    if (initialDelay < 0) {
      initialDelay = initialDelay + ChronoUnit.DAYS.getDuration().toMinutes();
    }

    LOG.info("DirectoryUpdateService Starting in " + initialDelay + " minute(s)");

    scheduler.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        applyUpdates();
      }
    }, initialDelay, frequencyInHour * (ChronoUnit.MINUTES.getDuration().getSeconds()), TimeUnit.MINUTES);
  }

  @Override
  protected void shutDown() throws Exception {
    scheduler.shutdown();
  }

  /**
   * Apply the updates from the Directory to the Registry (if required)
   *
   */
  public void applyUpdates() {
    //get all participant from the Directory
    PagingResponse<Participant> participants = directoryParticipantService.list(new PagingRequest(0, DEFAULT_MAX_LIMIT));
    PagingResponse<Node> nodes = directoryNodeService.list(new PagingRequest(0, DEFAULT_MAX_LIMIT));

    Map<Integer, Participant> participantsById = Maps.newHashMap();
    Map<Integer, Node> nodeByParticipantsId = Maps.newHashMap();

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

    List<org.gbif.api.model.registry.Node> registryNodes = nodeMapper.list(new PagingRequest(0, DEFAULT_MAX_LIMIT));
    Integer participantId;
    Participant participant;
    Node directoryNode;
    for (org.gbif.api.model.registry.Node registryNode : registryNodes) {
      participantId = findParticipantId(registryNode);
      if (participantId != null) {
        participant = participantsById.get(participantId);
        directoryNode = nodeByParticipantsId.get(participantId);
        if (participant != null) {
          if (shouldUpdateNode(registryNode, participant, directoryNode)) {
            updateRegistryNode(registryNode, participant, directoryNode);
          }
        } else {
          LOG.warn("Can't find participantId {} in the Directory", participantId);
        }
      }
    }

  }

  /**
   * Check if a registry Node requires an update.
   *
   */
  private boolean shouldUpdateNode(org.gbif.api.model.registry.Node registryNode, Participant participant,
                                   @Nullable Node directoryNode) {

    String titleFromDirectory = directoryNode != null ? directoryNode.getName() : participant.getName();
    return !Objects.equals(
            registryNode.getTitle(), titleFromDirectory)
            || !Objects.equals(registryNode.getCountry(), participant.getCountryCode())
            || !Objects.equals(DirectoryRegistryConstantsMapping.PARTICIPATION_STATUS.get(participant.getParticipationStatus()),
            registryNode.getParticipationStatus())
            || !Objects.equals(DirectoryRegistryConstantsMapping.PARTICIPATION_TYPE.get(participant.getType()),
            registryNode.getType())
            || !Objects.equals(registryNode.getGbifRegion(), participant.getGbifRegion());
  }

  /**
   * Update a Registry Node from a Directory Participant and Node.
   * registryNode title is set to the name of the Node from the Directory with a fallback on participant name is
   * no node is available in the Directory.
   *
   */
  private void updateRegistryNode(org.gbif.api.model.registry.Node registryNode, Participant participant,
                                  @Nullable Node directoryNode) {
    String titleFromDirectory = directoryNode != null ? directoryNode.getName() : participant.getName();
    registryNode.setTitle(titleFromDirectory);
    registryNode.setCountry(participant.getCountryCode());
    registryNode.setParticipationStatus(DirectoryRegistryConstantsMapping.PARTICIPATION_STATUS.get(participant.getParticipationStatus()));
    registryNode.setType(DirectoryRegistryConstantsMapping.PARTICIPATION_TYPE.get(participant.getType()));
    registryNode.setGbifRegion(participant.getGbifRegion());

    registryNode.setModifiedBy(DIRECTORY_UPDATE_USER);

    nodeMapper.update(registryNode);
    LOG.info("Node {} ({}) updated by {}", registryNode.getTitle(), registryNode.getKey(), DIRECTORY_UPDATE_USER);
  }

  private static Integer findParticipantId(org.gbif.api.model.registry.Node node) {
    for (Identifier id : node.getIdentifiers()) {
      if (IdentifierType.GBIF_PARTICIPANT == id.getType()) {
        try {
          return Integer.parseInt(id.getIdentifier());
        } catch (NumberFormatException e) {
          LOG.error("Directory participantId is no integer: %s", id.getIdentifier());
        }
      }
    }
    return null;
  }

}
