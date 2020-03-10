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
package org.gbif.registry.cli.directoryupdate;

import org.gbif.api.model.directory.Participant;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.Node;
import org.gbif.api.vocabulary.IdentifierType;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.beust.jcommander.internal.Lists;

/** Generates mock models */
public class MockDataGenerator {

  private static AtomicInteger NEXT_ID = new AtomicInteger(1);

  public static Node generateRegistryNode(String title, Integer participantId) {
    Node node = new Node();
    node.setKey(UUID.randomUUID());
    node.setTitle(title);

    if (participantId != null) {
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

  public static org.gbif.api.model.directory.Node generateDirectoryNode(
      String name, Integer participantId) {
    org.gbif.api.model.directory.Node node = new org.gbif.api.model.directory.Node();
    node.setId(NEXT_ID.getAndIncrement());
    node.setParticipantId(participantId);
    node.setName(name);
    return node;
  }

  public static Participant generateDirectoryParticipant(String name, Integer participantId) {
    Participant participant = new Participant();
    participant.setId(participantId);
    participant.setName(name);
    return participant;
  }
}
