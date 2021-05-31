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
import org.gbif.api.model.collections.Contactable;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.suggestions.CollectionChangeSuggestion;
import org.gbif.api.model.collections.suggestions.InstitutionChangeSuggestion;
import org.gbif.api.model.collections.suggestions.Type;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.persistence.mapper.UserRightsMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;
import org.gbif.registry.persistence.mapper.collections.PersonMapper;
import org.gbif.registry.security.SecurityContextCheck;
import org.gbif.registry.security.UserRoles;
import org.gbif.registry.service.collections.suggestions.CollectionChangeSuggestionService;
import org.gbif.registry.service.collections.suggestions.InstitutionChangeSuggestionService;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

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
  private final PersonMapper personMapper;
  private final InstitutionChangeSuggestionService institutionChangeSuggestionService;
  private final CollectionChangeSuggestionService collectionChangeSuggestionService;
  private final ObjectMapper objectMapper;

  public GrSciCollAuthorizationService(
      UserRightsMapper userRightsMapper,
      CollectionMapper collectionMapper,
      InstitutionMapper institutionMapper,
      PersonMapper personMapper,
      InstitutionChangeSuggestionService institutionChangeSuggestionService,
      CollectionChangeSuggestionService collectionChangeSuggestionService,
      @Qualifier("registryObjectMapper") ObjectMapper objectMapper) {
    this.userRightsMapper = userRightsMapper;
    this.collectionMapper = collectionMapper;
    this.institutionMapper = institutionMapper;
    this.personMapper = personMapper;
    this.institutionChangeSuggestionService = institutionChangeSuggestionService;
    this.collectionChangeSuggestionService = collectionChangeSuggestionService;
    this.objectMapper = objectMapper;
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

  public boolean allowedToModifyInstitution(
      Authentication authentication, UUID institutionKey, boolean isDeleteOrMergeOrConversion) {
    if (SecurityContextCheck.checkUserInRole(authentication, GRSCICOLL_ADMIN_ROLE)) {
      return true;
    }

    if (isDeleteOrMergeOrConversion
        && !checkUserInRole(authentication, UserRoles.GRSCICOLL_MEDIATOR_ROLE)) {
      return false;
    }

    Institution institution = institutionMapper.get(institutionKey);
    return allowedToModifyEntity(authentication.getName(), institutionKey)
        || allowedToModifyCountry(authentication.getName(), extractCountry(institution));
  }

  public boolean allowedToModifyCollection(
      Authentication authentication,
      UUID collectionKey,
      Collection collectionInMessageBody,
      boolean isDeleteOrMerge) {
    if (SecurityContextCheck.checkUserInRole(authentication, GRSCICOLL_ADMIN_ROLE)) {
      return true;
    }

    if (isDeleteOrMerge && !checkUserInRole(authentication, UserRoles.GRSCICOLL_MEDIATOR_ROLE)) {
      return false;
    }

    String username = authentication.getName();
    if (username == null || collectionKey == null) {
      return false;
    }

    Collection persistedCollection = collectionMapper.get(collectionKey);

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

    // check permissions in the collection
    if (allowedToModifyEntity(username, collectionKey)) {
      return true;
    }

    // check country of the collection
    if (allowedToModifyCountry(username, extractCountry(persistedCollection))) {
      return true;
    }

    // we check institution rights
    if (persistedCollection.getInstitutionKey() != null) {
      if (allowedToModifyEntity(username, persistedCollection.getInstitutionKey())) {
        return true;
      }

      Institution persistedInstitution =
          institutionMapper.get(persistedCollection.getInstitutionKey());
      return allowedToModifyCountry(username, extractCountry(persistedInstitution));
    }

    return false;
  }

  /** An editor or mediator can create an institution if they have their country in the scopes. */
  public boolean allowedToCreateInstitution(
      Institution institution, Authentication authentication) {
    if (SecurityContextCheck.checkUserInRole(authentication, GRSCICOLL_ADMIN_ROLE)) {
      return true;
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

    if (collection == null || collection.getInstitutionKey() == null) {
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
      InstitutionChangeSuggestion changeSuggestion =
          institutionChangeSuggestionService.getChangeSuggestion(key);

      if (changeSuggestion == null) {
        return false;
      }

      if (changeSuggestion.getType() == Type.CREATE) {
        return allowedToCreateInstitution(changeSuggestion.getSuggestedEntity(), authentication);
      }

      boolean isDeleteOrMergeOrConversion =
          changeSuggestion.getType() == Type.DELETE
              || changeSuggestion.getType() == Type.MERGE
              || changeSuggestion.getType() == Type.CONVERSION_TO_COLLECTION;

      return allowedToModifyInstitution(
          authentication, changeSuggestion.getEntityKey(), isDeleteOrMergeOrConversion);
    } else if (COLLECTION.equalsIgnoreCase(entityType)) {
      CollectionChangeSuggestion changeSuggestion =
          collectionChangeSuggestionService.getChangeSuggestion(key);

      if (changeSuggestion == null) {
        return false;
      }

      if (changeSuggestion.getType() == Type.CREATE) {
        return allowedToCreateCollection(changeSuggestion.getSuggestedEntity(), authentication);
      }

      boolean isDeleteOrMerge =
          changeSuggestion.getType() == Type.DELETE || changeSuggestion.getType() == Type.MERGE;

      return allowedToModifyCollection(
          authentication,
          changeSuggestion.getEntityKey(),
          changeSuggestion.getSuggestedEntity(),
          isDeleteOrMerge);
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
}
