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
package org.gbif.registry.directory;

import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.Node;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.directory.NodePersonRole;
import org.gbif.api.vocabulary.directory.ParticipantPersonRole;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableBiMap;

/** Provides mapping between Directory and Registry concepts (e.g. enumerations) */
public class DirectoryRegistryMapping {

  private static final Logger LOG = LoggerFactory.getLogger(DirectoryRegistryMapping.class);

  private DirectoryRegistryMapping() {}

  /** Maps contact types to participant roles. */
  public static final ImmutableBiMap<ParticipantPersonRole, ContactType>
      PARTICIPANT_ROLE_TO_CONTACT_TYPE =
          ImmutableBiMap.of(
              ParticipantPersonRole.ADDITIONAL_DELEGATE,
              ContactType.ADDITIONAL_DELEGATE,
              ParticipantPersonRole.HEAD_OF_DELEGATION,
              ContactType.HEAD_OF_DELEGATION,
              ParticipantPersonRole.TEMPORARY_DELEGATE,
              ContactType.TEMPORARY_DELEGATE,
              ParticipantPersonRole.TEMPORARY_HEAD_OF_DELEGATION,
              ContactType.TEMPORARY_HEAD_OF_DELEGATION);

  public static final ImmutableBiMap<NodePersonRole, ContactType> NODE_ROLE_TO_CONTACT_TYPE =
      ImmutableBiMap.of(
          NodePersonRole.NODE_MANAGER,
          ContactType.NODE_MANAGER,
          NodePersonRole.NODE_STAFF,
          ContactType.NODE_STAFF);

  /** Gets the Directory participantID from a Registry Node. */
  public static Integer findParticipantID(Node node) {
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
