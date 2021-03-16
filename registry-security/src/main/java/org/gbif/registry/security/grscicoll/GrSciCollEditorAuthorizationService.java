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
package org.gbif.registry.security.grscicoll;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.domain.collections.Constants;
import org.gbif.registry.persistence.mapper.UserRightsMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;
import org.gbif.registry.persistence.mapper.collections.PersonMapper;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GrSciCollEditorAuthorizationService {

  private static final Logger LOG =
      LoggerFactory.getLogger(GrSciCollEditorAuthorizationService.class);

  private final UserRightsMapper userRightsMapper;
  private final CollectionMapper collectionMapper;
  private final InstitutionMapper institutionMapper;
  private final PersonMapper personMapper;

  public GrSciCollEditorAuthorizationService(
      UserRightsMapper userRightsMapper,
      CollectionMapper collectionMapper,
      InstitutionMapper institutionMapper,
      PersonMapper personMapper) {
    this.userRightsMapper = userRightsMapper;
    this.collectionMapper = collectionMapper;
    this.institutionMapper = institutionMapper;
    this.personMapper = personMapper;
  }

  public boolean isIrnIdentifier(Identifier identifier) {
    return identifier != null && identifier.getType() == IdentifierType.IH_IRN;
  }

  public boolean isIrnIdentifier(String entityType, UUID entityKey, int identifierKey) {
    List<Identifier> entityIdentifiers = null;
    if ("collection".equalsIgnoreCase(entityType)) {
      entityIdentifiers = collectionMapper.listIdentifiers(entityKey);
    } else if ("institution".equalsIgnoreCase(entityType)) {
      entityIdentifiers = institutionMapper.listIdentifiers(entityKey);
    } else if ("person".equalsIgnoreCase(entityType)) {
      entityIdentifiers = personMapper.listIdentifiers(entityKey);
    } else {
      return false;
    }

    return entityIdentifiers.stream()
        .anyMatch(i -> i.getKey().equals(identifierKey) && isIrnIdentifier(i));
  }

  public boolean isIDigBioEntity(String entityType, UUID entityKey) {
    List<MachineTag> machineTags;
    if ("collection".equalsIgnoreCase(entityType)) {
      machineTags = collectionMapper.listMachineTags(entityKey);
    } else if ("institution".equalsIgnoreCase(entityType)) {
      machineTags = institutionMapper.listMachineTags(entityKey);
    } else {
      return false;
    }

    return machineTags.stream()
        .anyMatch(mt -> Constants.IDIGBIO_NAMESPACE.equals(mt.getNamespace()));
  }

  public boolean allowedToModifyEntity(String username, UUID key) {
    if (username == null || key == null) {
      return false;
    }
    boolean allowed = userRightsMapper.keyExistsForUser(username, key);
    LOG.debug(
        "User {} {} allowed to edit GrSciColl entity {}", username, allowed ? "is" : "is not", key);
    return allowed;
  }

  public boolean allowedToUpdateCollection(
      String username, UUID collectionKey, Collection collectionInMessageBody) {
    if (username == null || collectionKey == null) {
      return false;
    }

    UUID persistedInstitutionKey = collectionMapper.getInstitutionKey(collectionKey);
    if (collectionInMessageBody != null
        && collectionInMessageBody.getInstitutionKey() != null
        && !persistedInstitutionKey.equals(collectionInMessageBody.getInstitutionKey())) {
      // check if the user has permissions in the new institution
      if (!userRightsMapper.keyExistsForUser(
          username, collectionInMessageBody.getInstitutionKey())) {
        return false;
      }
    }

    // check permissions in the collection
    boolean allowed = userRightsMapper.keyExistsForUser(username, collectionKey);
    if (!allowed) {
      // we check institution rights
      allowed = userRightsMapper.keyExistsForUser(username, persistedInstitutionKey);
    }

    return allowed;
  }
}
