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
package org.gbif.registry.service.collections.suggestions;

import com.google.common.eventbus.Subscribe;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.descriptors.DescriptorChangeSuggestion;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.events.collections.UpdateCollectionEntityEvent;
import org.gbif.registry.persistence.mapper.collections.ChangeSuggestionMapper;
import org.gbif.registry.persistence.mapper.collections.DescriptorChangeSuggestionMapper;
import org.gbif.registry.persistence.mapper.collections.dto.ChangeSuggestionDto;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Event listener that updates the country field in change_suggestion and descriptor_change_suggestion tables
 * when a collection or institution's country changes.
 */
@Component
public class ChangeSuggestionCountryUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(ChangeSuggestionCountryUpdater.class);

  private final ChangeSuggestionMapper changeSuggestionMapper;
  private final DescriptorChangeSuggestionMapper descriptorChangeSuggestionMapper;

  public ChangeSuggestionCountryUpdater(ChangeSuggestionMapper changeSuggestionMapper, EventManager eventManager,
    DescriptorChangeSuggestionMapper descriptorChangeSuggestionMapper) {
    this.changeSuggestionMapper = changeSuggestionMapper;
    this.descriptorChangeSuggestionMapper = descriptorChangeSuggestionMapper;
    eventManager.register(this);
  }

  @Subscribe
  @Transactional
  public final <T extends CollectionEntity> void updatedCollection(
    UpdateCollectionEntityEvent<T> event) {
    CollectionEntity newEntity = event.getNewObject();
    CollectionEntity oldEntity = event.getOldObject();

    Country oldCountry = getCountry(oldEntity);
    Country newCountry = getCountry(newEntity);

    if (!Objects.equals(oldCountry, newCountry) && newCountry != null) {
      UUID entityKey = newEntity.getKey();
      CollectionEntityType entityType = getEntityType(newEntity);

      LOG.info("Updating country to {} for change suggestions related to {} with key {}",
             newCountry.getIso2LetterCode(), entityType, entityKey);

      // Get all change suggestions for this entity
      List<ChangeSuggestionDto> suggestions =
          changeSuggestionMapper.list(null, null, entityType, null, entityKey, null, null, null);

      // Update each suggestion's country
      for (ChangeSuggestionDto suggestion : suggestions) {
        suggestion.setCountryIsoCode(newCountry.getIso2LetterCode());
        changeSuggestionMapper.update(suggestion);
      }

      // Check if the entity is collection and it has descriptor change suggestions
      if (entityType.equals(CollectionEntityType.COLLECTION)) {
        List<DescriptorChangeSuggestion> descriptorChangeSuggestions = descriptorChangeSuggestionMapper.list(null, null, null, null, entityKey, null);

        for (DescriptorChangeSuggestion descriptorChangeSuggestion: descriptorChangeSuggestions) {
          descriptorChangeSuggestion.setCountry(newCountry);
          descriptorChangeSuggestionMapper.updateSuggestion(descriptorChangeSuggestion);
        }
      }
    }
  }

  private Country getCountry(CollectionEntity entity) {
    if (entity instanceof Institution) {
      Institution institution = (Institution) entity;
      if (institution.getAddress() != null && institution.getAddress().getCountry() != null) {
        return institution.getAddress().getCountry();
      } else if (institution.getMailingAddress() != null && institution.getMailingAddress().getCountry() != null) {
        return institution.getMailingAddress().getCountry();
      }
    } else if (entity instanceof Collection) {
      Collection collection = (Collection) entity;
      if (collection.getAddress() != null && collection.getAddress().getCountry() != null) {
        return collection.getAddress().getCountry();
      } else if (collection.getMailingAddress() != null && collection.getMailingAddress().getCountry() != null) {
        return collection.getMailingAddress().getCountry();
      }
    }
    return null;
  }

  private CollectionEntityType getEntityType(CollectionEntity entity) {
    if (entity instanceof Institution) {
      return CollectionEntityType.INSTITUTION;
    } else if (entity instanceof Collection) {
      return CollectionEntityType.COLLECTION;
    }
    throw new IllegalArgumentException("Unknown entity type: " + entity.getClass().getName());
  }
}
