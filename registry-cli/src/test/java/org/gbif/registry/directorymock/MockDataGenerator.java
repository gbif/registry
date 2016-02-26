package org.gbif.registry.directorymock;

import org.gbif.api.model.directory.Participant;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.Node;
import org.gbif.api.vocabulary.IdentifierType;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.beust.jcommander.internal.Lists;

/**
 * Generates mock models
 */
public class MockDataGenerator {

  private static AtomicInteger NEXT_ID = new AtomicInteger(1);

  public static Node generateRegistryNode(String title, Integer participantId){
    Node node = new Node();
    node.setKey(UUID.randomUUID());
    node.setTitle(title);

    if(participantId != null) {
      List<Identifier> identifiers = Lists.newArrayList();
      Identifier identifier = new Identifier();
      identifier.setKey(NEXT_ID.getAndIncrement());
      identifier.setIdentifier(participantId.toString());
      identifier.setType(IdentifierType.GBIF_PARTICIPANT);
      identifiers.add(identifier);
      node.setIdentifiers(identifiers);
    }

    return node;
  }

  public static org.gbif.api.model.directory.Node generateDirectoryNode(String name, Integer participantId){
    org.gbif.api.model.directory.Node node = new org.gbif.api.model.directory.Node();
    node.setId(NEXT_ID.getAndIncrement());
    node.setParticipantId(participantId);
    node.setName(name);
    return node;
  }

  public static Participant generateDirectoryParticipant(String name, Integer participantId){
    Participant participant = new Participant();
    participant.setId(participantId);
    participant.setName(name);
    return participant;
  }
}
