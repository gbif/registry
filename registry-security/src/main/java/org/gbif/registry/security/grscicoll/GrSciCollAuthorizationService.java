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
package org.gbif.registry.security.grscicoll;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.collections.Contactable;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.suggestions.Type;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.persistence.mapper.UserRightsMapper;
import org.gbif.registry.persistence.mapper.collections.ChangeSuggestionMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;
import org.gbif.registry.persistence.mapper.collections.dto.ChangeSuggestionDto;
import org.gbif.registry.security.SecurityContextCheck;
import org.gbif.registry.security.UserRoles;

import java.util.UUID;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.gbif.registry.security.SecurityContextCheck.checkUserInRole;
import static org.gbif.registry.security.UserRoles.GRSCICOLL_ADMIN_ROLE;

@Service
public class GrSciCollAuthorizationService {

  private static final Logger LOG = LoggerFactory.getLogger(GrSciCollAuthorizationService.class);

  public static final String INSTITUTION = "institution";
  public static final String COLLECTION = "collection";

  private final UserRightsMapper userRightsMapper;
  private final CollectionMapper collectionMapper;
  private final InstitutionMapper institutionMapper;
  private final ChangeSuggestionMapper changeSuggestionMapper;
  private final ObjectMapper objectMapper;

  public GrSciCollAuthorizationService(
      UserRightsMapper userRightsMapper,
      CollectionMapper collectionMapper,
      InstitutionMapper institutionMapper,
      ChangeSuggestionMapper changeSuggestionMapper,
      @Qualifier("registryObjectMapper") ObjectMapper objectMapper) {
    this.userRightsMapper = userRightsMapper;
    this.collectionMapper = collectionMapper;
    this.institutionMapper = institutionMapper;
    this.changeSuggestionMapper = changeSuggestionMapper;
    this.objectMapper = objectMapper;
  }

  public boolean allowedToCreateMachineTag(Authentication authentication, MachineTag machineTag) {
    if (SecurityContextCheck.checkUserInRole(authentication, GRSCICOLL_ADMIN_ROLE)) {
      return true;
    }

    String username = authentication.getName();
    if (username == null || machineTag == null || machineTag.getNamespace() == null) {
      return false;
    }

    return userRightsMapper.namespaceExistsForUser(username, machineTag.getNamespace());
  }

  public boolean allowedToDeleteMachineTag(Authentication authentication, int machineTagKey) {
    if (SecurityContextCheck.checkUserInRole(authentication, GRSCICOLL_ADMIN_ROLE)) {
      return true;
    }

    String username = authentication.getName();
    if (username == null) {
      return false;
    }

    return userRightsMapper.allowedToDeleteMachineTag(username, machineTagKey);
  }

  private boolean allowedToModifyEntity(String username, UUID key) {
    if (username == null || key == null) {
      return false;
    }
    boolean allowed = userRightsMapper.keyExistsForUser(username, key);
    LOG.debug(
        "User {} {} allowed to edit GrSciColl entity {}", username, allowed ? "is" : "is not", key);
    return allowed;
  }

  private boolean allowedToModifyCountry(String username, Country country) {
    if (username == null || country == null) {
      return false;
    }
    boolean allowed = userRightsMapper.countryExistsForUser(username, country.getIso2LetterCode());
    LOG.debug(
        "User {} {} allowed to edit entity of country {}",
        username,
        allowed ? "is" : "is not",
        country);
    return allowed;
  }

  public boolean allowedToDeleteInstitution(Authentication authentication, UUID institutionKey) {
    if (!checkUserInRole(
        authentication, UserRoles.GRSCICOLL_ADMIN_ROLE, UserRoles.GRSCICOLL_MEDIATOR_ROLE)) {
      return false;
    }

    return allowedToModifyInstitution(authentication, institutionKey);
  }

  public boolean allowedToMergeInstitution(
      Authentication authentication, UUID institutionKey, UUID targetEntityKey) {
    if (!checkUserInRole(
        authentication, UserRoles.GRSCICOLL_ADMIN_ROLE, UserRoles.GRSCICOLL_MEDIATOR_ROLE)) {
      return false;
    }

    return allowedToModifyInstitution(authentication, institutionKey)
        && allowedToModifyInstitution(authentication, targetEntityKey);
  }

  public boolean allowedToConvertInstitution(
      Authentication authentication,
      UUID institutionKey,
      @Nullable UUID institutionForNewCollectionKey) {
    if (!checkUserInRole(
        authentication, UserRoles.GRSCICOLL_ADMIN_ROLE, UserRoles.GRSCICOLL_MEDIATOR_ROLE)) {
      return false;
    }

    return allowedToModifyInstitution(authentication, institutionKey)
        && (institutionForNewCollectionKey == null
            || allowedToModifyInstitution(authentication, institutionForNewCollectionKey));
  }

  public boolean allowedToModifyInstitution(Authentication authentication, UUID institutionKey) {
    if (SecurityContextCheck.checkUserInRole(authentication, GRSCICOLL_ADMIN_ROLE)) {
      return true;
    }

    Institution institution = institutionMapper.get(institutionKey);

    if (institution == null) {
      return false;
    }

    return allowedToModifyEntity(authentication.getName(), institutionKey)
        || allowedToModifyCountry(authentication.getName(), extractCountry(institution));
  }

  public boolean allowedToDeleteCollection(Authentication authentication, UUID collectionKey) {
    if (!checkUserInRole(
        authentication, UserRoles.GRSCICOLL_ADMIN_ROLE, UserRoles.GRSCICOLL_MEDIATOR_ROLE)) {
      return false;
    }

    return allowedToModifyCollection(authentication, collectionKey, null);
  }

  public boolean allowedToMergeCollection(
      Authentication authentication, UUID collectionKey, UUID targetEntityKey) {
    if (!checkUserInRole(
        authentication, UserRoles.GRSCICOLL_ADMIN_ROLE, UserRoles.GRSCICOLL_MEDIATOR_ROLE)) {
      return false;
    }

    return allowedToModifyCollection(authentication, collectionKey, null)
        && allowedToModifyCollection(authentication, targetEntityKey, null);
  }

  public boolean allowedToModifyCollection(
      Authentication authentication, UUID collectionKey, Collection collectionInMessageBody) {
    if (SecurityContextCheck.checkUserInRole(authentication, GRSCICOLL_ADMIN_ROLE)) {
      return true;
    }

    String username = authentication.getName();
    Collection persistedCollection = collectionMapper.get(collectionKey);
    if (username == null || collectionKey == null || persistedCollection == null) {
      return false;
    }

    // check if the user has changed the institution and has permissions in the new one. This has to
    // be the first check since it's more restrictive.
    if (collectionInMessageBody != null) {
      UUID newInstitutionKey = collectionInMessageBody.getInstitutionKey();
      if (newInstitutionKey != null
          && !newInstitutionKey.equals(persistedCollection.getInstitutionKey())) {
        Institution newInstitution = institutionMapper.get(newInstitutionKey);

        if (!allowedToModifyEntity(username, newInstitutionKey)
            && !allowedToModifyCountry(username, extractCountry(newInstitution))) {
          return false;
        }
      }
    }

    // check permissions in the collection and the country
    if (allowedToModifyEntity(username, collectionKey)
        || allowedToModifyCountry(username, extractCountry(persistedCollection))) {
      return true;
    }

    // we check institution rights
    if (persistedCollection.getInstitutionKey() != null) {
      if (allowedToModifyEntity(username, persistedCollection.getInstitutionKey())) {
        return true;
      }

      Institution persistedInstitution =
          institutionMapper.get(persistedCollection.getInstitutionKey());
      if (persistedInstitution != null) {
        return allowedToModifyCountry(username, extractCountry(persistedInstitution));
      }
    }

    return false;
  }

  /** An editor or mediator can create an institution if they have their country in the scopes. */
  public boolean allowedToCreateInstitution(
      Institution institution, Authentication authentication) {
    if (SecurityContextCheck.checkUserInRole(authentication, GRSCICOLL_ADMIN_ROLE)) {
      return true;
    }

    if (institution == null) {
      return false;
    }

    return allowedToModifyCountry(authentication.getName(), extractCountry(institution));
  }

  /**
   * An editor or mediator can create collections if they have their institution or country in the
   * scopes.
   */
  public boolean allowedToCreateCollection(Collection collection, Authentication authentication) {
    if (SecurityContextCheck.checkUserInRole(authentication, GRSCICOLL_ADMIN_ROLE)) {
      return true;
    }

    if (collection == null) {
      return false;
    }

    return allowedToModifyEntity(authentication.getName(), collection.getInstitutionKey())
        || allowedToModifyCountry(authentication.getName(), extractCountry(collection));
  }

  public boolean allowedToUpdateChangeSuggestion(
      int key, String entityType, Authentication authentication) {
    if (SecurityContextCheck.checkUserInRole(authentication, GRSCICOLL_ADMIN_ROLE)) {
      return true;
    }

    if (INSTITUTION.equalsIgnoreCase(entityType)) {
      ChangeSuggestionDto changeSuggestion =
          changeSuggestionMapper.getByKeyAndType(key, CollectionEntityType.INSTITUTION);

      if (changeSuggestion == null) {
        return false;
      }

      if (changeSuggestion.getType() == Type.CREATE) {
        return allowedToCreateInstitution(
            readEntity(changeSuggestion.getSuggestedEntity(), Institution.class), authentication);
      } else if (changeSuggestion.getType() == Type.DELETE) {
        return allowedToDeleteInstitution(authentication, changeSuggestion.getEntityKey());
      } else if (changeSuggestion.getType() == Type.MERGE) {
        return allowedToMergeInstitution(
            authentication, changeSuggestion.getEntityKey(), changeSuggestion.getMergeTargetKey());
      } else if (changeSuggestion.getType() == Type.CONVERSION_TO_COLLECTION) {
        return allowedToConvertInstitution(
            authentication,
            changeSuggestion.getEntityKey(),
            changeSuggestion.getInstitutionConvertedCollection());
      } else {
        return allowedToModifyInstitution(authentication, changeSuggestion.getEntityKey());
      }
    } else if (COLLECTION.equalsIgnoreCase(entityType)) {
      ChangeSuggestionDto changeSuggestion =
          changeSuggestionMapper.getByKeyAndType(key, CollectionEntityType.COLLECTION);

      if (changeSuggestion == null) {
        return false;
      }

      Collection suggestedEntity =
          readEntity(changeSuggestion.getSuggestedEntity(), Collection.class);
      if (changeSuggestion.getType() == Type.CREATE) {
        return allowedToCreateCollection(suggestedEntity, authentication);
      } else if (changeSuggestion.getType() == Type.DELETE) {
        return allowedToDeleteCollection(authentication, changeSuggestion.getEntityKey());
      } else if (changeSuggestion.getType() == Type.MERGE) {
        return allowedToMergeCollection(
            authentication, changeSuggestion.getEntityKey(), changeSuggestion.getMergeTargetKey());
      } else {
        return allowedToModifyCollection(
            authentication, changeSuggestion.getEntityKey(), suggestedEntity);
      }
    }

    return false;
  }

  private <T extends Contactable> Country extractCountry(T entity) {
    if (entity.getAddress() != null && entity.getAddress().getCountry() != null) {
      return entity.getAddress().getCountry();
    }
    if (entity.getMailingAddress() != null) {
      return entity.getMailingAddress().getCountry();
    }
    return null;
  }

  private <T> T readEntity(String content, Class<T> clazz) {
    if (content == null) {
      return null;
    }

    try {
      return objectMapper.readValue(content, clazz);
    } catch (JsonProcessingException e) {
      LOG.warn("Couldn't read json content", e);
      return null;
    }
  }
}
