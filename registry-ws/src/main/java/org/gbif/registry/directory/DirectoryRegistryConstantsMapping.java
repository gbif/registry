package org.gbif.registry.directory;

import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.ParticipationStatus;
import org.gbif.api.vocabulary.directory.NodePersonRole;
import org.gbif.api.vocabulary.directory.ParticipantPersonRole;

import com.google.common.collect.ImmutableMap;

/**
 *
 */
public class DirectoryRegistryConstantsMapping {

  /**
   * Maps contact types to participant roles.
   */
  public static final ImmutableMap<ParticipantPersonRole, ContactType> PARTICIPANT_ROLE_TO_CONTACT_TYPE =
          ImmutableMap.of(ParticipantPersonRole.ADDITIONAL_DELEGATE, ContactType.ADDITIONAL_DELEGATE,
                  ParticipantPersonRole.HEAD_OF_DELEGATION, ContactType.HEAD_OF_DELEGATION,
                  ParticipantPersonRole.TEMPORARY_DELEGATE, ContactType.TEMPORARY_DELEGATE,
                  ParticipantPersonRole.TEMPORARY_HEAD_OF_DELEGATION, ContactType.TEMPORARY_HEAD_OF_DELEGATION);

  public static final ImmutableMap<NodePersonRole, ContactType> NODE_ROLE_TO_CONTACT_TYPE =
          ImmutableMap.of(NodePersonRole.NODE_MANAGER, ContactType.NODE_MANAGER,
                  NodePersonRole.NODE_STAFF, ContactType.NODE_STAFF);

  public static final ImmutableMap<org.gbif.api.vocabulary.directory.ParticipationStatus, ParticipationStatus> PARTICIPATION_STATUS =
          ImmutableMap.of(org.gbif.api.vocabulary.directory.ParticipationStatus.VOTING, ParticipationStatus.VOTING,
                  org.gbif.api.vocabulary.directory.ParticipationStatus.OBSERVER, ParticipationStatus.OBSERVER,
                  org.gbif.api.vocabulary.directory.ParticipationStatus.ASSOCIATE, ParticipationStatus.ASSOCIATE,
                  org.gbif.api.vocabulary.directory.ParticipationStatus.AFFILIATE, ParticipationStatus.AFFILIATE,
                  org.gbif.api.vocabulary.directory.ParticipationStatus.FORMER, ParticipationStatus.FORMER);
}
