package org.gbif.registry.directory;

import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.NodeType;
import org.gbif.api.vocabulary.ParticipationStatus;
import org.gbif.api.vocabulary.directory.NodePersonRole;
import org.gbif.api.vocabulary.directory.ParticipantPersonRole;
import org.gbif.api.vocabulary.directory.ParticipantType;

import com.google.common.collect.ImmutableBiMap;

/**
 * Provides mapping between Directory and Registry enumerations
 */
public class DirectoryRegistryConstantsMapping {

  /**
   * Maps contact types to participant roles.
   */
  public static final ImmutableBiMap<ParticipantPersonRole, ContactType> PARTICIPANT_ROLE_TO_CONTACT_TYPE =
          ImmutableBiMap.of(ParticipantPersonRole.ADDITIONAL_DELEGATE, ContactType.ADDITIONAL_DELEGATE,
                  ParticipantPersonRole.HEAD_OF_DELEGATION, ContactType.HEAD_OF_DELEGATION,
                  ParticipantPersonRole.TEMPORARY_DELEGATE, ContactType.TEMPORARY_DELEGATE,
                  ParticipantPersonRole.TEMPORARY_HEAD_OF_DELEGATION, ContactType.TEMPORARY_HEAD_OF_DELEGATION);

  public static final ImmutableBiMap<NodePersonRole, ContactType> NODE_ROLE_TO_CONTACT_TYPE =
          ImmutableBiMap.of(NodePersonRole.NODE_MANAGER, ContactType.NODE_MANAGER,
                  NodePersonRole.NODE_STAFF, ContactType.NODE_STAFF);

  /**
   * Return the Directory ParticipationStatus from a Registry ParticipationStatus
   * PARTICIPATION_STATUS.get(org.gbif.api.vocabulary.directory.ParticipationStatus) returns org.gbif.api.vocabulary.ParticipationStatus
   */
  public static final ImmutableBiMap<org.gbif.api.vocabulary.directory.ParticipationStatus, ParticipationStatus> PARTICIPATION_STATUS =
          ImmutableBiMap.of(org.gbif.api.vocabulary.directory.ParticipationStatus.VOTING, ParticipationStatus.VOTING,
                  org.gbif.api.vocabulary.directory.ParticipationStatus.OBSERVER, ParticipationStatus.OBSERVER,
                  org.gbif.api.vocabulary.directory.ParticipationStatus.ASSOCIATE, ParticipationStatus.ASSOCIATE,
                  org.gbif.api.vocabulary.directory.ParticipationStatus.AFFILIATE, ParticipationStatus.AFFILIATE,
                  org.gbif.api.vocabulary.directory.ParticipationStatus.FORMER, ParticipationStatus.FORMER);

  public static final ImmutableBiMap<ParticipantType, NodeType> PARTICIPATION_TYPE =
          ImmutableBiMap.of(ParticipantType.COUNTRY, NodeType.COUNTRY,
                  ParticipantType.OTHER, NodeType.OTHER);

  }
