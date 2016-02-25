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

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.LocalTime;
import org.threeten.bp.temporal.ChronoUnit;

/**
 * WORK-IN-PROGRESS
 * Service that will periodically get the list of participants and nodes from the Directory and update the registry
 * data is required.
 *
 */
public class DirectoryUpdateService extends AbstractIdleService {

  private static final Logger LOG = LoggerFactory.getLogger(DirectoryUpdateService.class);
  //avoid using 0
  private static final int DEFAULT_START_HOUR = 5;
  private static final int DEFAULT_START_MINUTE = 34;

  private static final int DEFAULT_MAX_LIMIT = 1000;
  private final Integer frequencyInHour;
  private final Integer startHour;
  private final Integer startMinute;

  private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  private ParticipantService directoryParticipantService;
  private org.gbif.api.service.directory.NodeService directoryNodeService;

  private NodeMapper nodeMapper;

  public DirectoryUpdateService(DirectoryUpdateConfiguration cfg){
    Injector directoryUpdaterInj = cfg.createInjector();
    directoryParticipantService = directoryUpdaterInj.getInstance(ParticipantService.class);
    directoryNodeService = directoryUpdaterInj.getInstance(org.gbif.api.service.directory.NodeService.class);

    Injector registryInj = Guice.createInjector(cfg.db.createMyBatisModule());
    nodeMapper = registryInj.getInstance(NodeMapper.class);

    this.frequencyInHour = cfg.frequencyInHour;

    String[] timeParts = cfg.startTime.split(":");
    startHour = NumberUtils.toInt(timeParts[0], DEFAULT_START_HOUR);
    startMinute = NumberUtils.toInt(timeParts[1], DEFAULT_START_MINUTE);
  }

  @Override
  protected void startUp() throws Exception {

    LocalTime t = LocalTime.of(startHour, startMinute);
    long initialDelay = LocalTime.now().until(t, ChronoUnit.MINUTES);

    LOG.info("DirectoryUpdateService Starting in " + initialDelay + " minutes");

    scheduler.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        applyUpdates();
      }
    }, initialDelay, frequencyInHour*(ChronoUnit.MINUTES.getDuration().getSeconds()), TimeUnit.MINUTES);
  }

  @Override
  protected void shutDown() throws Exception {
    scheduler.shutdown();
  }

  /**
   * Apply the updates from the Directory to the Registry
   */
  public void applyUpdates(){
    //get all participant from the Directory
    PagingResponse<Participant> participants = directoryParticipantService.list(new PagingRequest(0, DEFAULT_MAX_LIMIT));
    PagingResponse<Node> nodes = directoryNodeService.list(new PagingRequest(0, DEFAULT_MAX_LIMIT));

    Map<Integer,Participant> participantsById = Maps.newHashMap();
    Map<Integer,Node> nodeByParticipantsId = Maps.newHashMap();

    for(Participant participant : participants.getResults()){
      participantsById.put(participant.getId(), participant);
    }

    for(Node node : nodes.getResults()){
      if(node.getParticipantId() != null){
        nodeByParticipantsId.put(node.getParticipantId(), node);
      }
      else{
        LOG.warn("Directory Node {} without link to a Participant skipped.", node.getId());
      }
    }

    List<org.gbif.api.model.registry.Node> registryNodes = nodeMapper.list(new PagingRequest(0, DEFAULT_MAX_LIMIT));
    Integer participantId;
    Participant participant;
    Node directoryNode;
    for(org.gbif.api.model.registry.Node registryNode : registryNodes){
      participantId = findParticipantId(registryNode);
      if(participantId != null){
        participant = participantsById.get(participantId);
        directoryNode = nodeByParticipantsId.get(participantId);
        // should we handle participant without node ?
        if(participant != null && directoryNode != null) {
          if (shouldUpdateNode(registryNode, participant, directoryNode)) {
            LOG.info("Requires update: " + registryNode.getTitle() + " should be update to " + directoryNode.getName());
          }
        }
        else{
          LOG.info("Can't find participantId " + participantId);
        }
      }
    }

  }

  /**
   * Check if a registry Node requires an update.
   *
   * @param registryNode
   * @param participant
   * @param directoryNode
   * @return
   */
  private boolean shouldUpdateNode(org.gbif.api.model.registry.Node registryNode, Participant participant, Node directoryNode) {
    return !Objects.equals(
            registryNode.getTitle(), directoryNode.getName())
            || !Objects.equals(registryNode.getCountry(), participant.getCountryCode())
            || !Objects.equals(DirectoryRegistryConstantsMapping.PARTICIPATION_STATUS.get(participant.getParticipationStatus()),
              registryNode.getParticipationStatus())
            || !Objects.equals(DirectoryRegistryConstantsMapping.PARTICIPATION_TYPE.get(participant.getType()),
              registryNode.getType())
            || !Objects.equals(registryNode.getGbifRegion(), participant.getGbifRegion());
  }

  /**
   * Update a Registry Node from a Directory Participant and Node.
   *
   * @param registryNode
   * @param participant
   * @param directoryNode
   */
  private void updateRegistryNode(org.gbif.api.model.registry.Node registryNode, Participant participant, Node directoryNode){
    registryNode.setTitle(directoryNode.getName());
    registryNode.setCountry(participant.getCountryCode());
    registryNode.setParticipationStatus(DirectoryRegistryConstantsMapping.PARTICIPATION_STATUS.get(participant.getParticipationStatus()));
    registryNode.setType(DirectoryRegistryConstantsMapping.PARTICIPATION_TYPE.get(participant.getType()));
    registryNode.setGbifRegion(participant.getGbifRegion());

   // participant.getContinent()
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
