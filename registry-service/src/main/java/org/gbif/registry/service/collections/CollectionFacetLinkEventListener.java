package org.gbif.registry.service.collections;

import java.util.Objects;

import org.gbif.api.model.collections.Collection;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.events.collections.CreateCollectionEntityEvent;
import org.gbif.registry.events.collections.UpdateCollectionEntityEvent;
import org.gbif.registry.persistence.mapper.GrScicollVocabFacetMapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.eventbus.Subscribe;

import lombok.extern.slf4j.Slf4j;

/**
 * Event listener that automatically manages collection_facet_links table
 * when Collection entities are created or updated.
 *
 * Only handles vocabulary fields that are actual facet parameters:
 * - CollectionContentType (hierarchical)
 * - PreservationType (hierarchical)
 *
 * Note: AccessionStatus is excluded because it's typically a single value
 * and may not require hierarchical facet treatment.
 */
@Slf4j
@Component
public class CollectionFacetLinkEventListener {

  private final GrScicollVocabFacetMapper grScicollVocabFacetMapper;

  @Autowired
  public CollectionFacetLinkEventListener(GrScicollVocabFacetMapper grScicollVocabFacetMapper, EventManager eventManager) {
    this.grScicollVocabFacetMapper = grScicollVocabFacetMapper;
    eventManager.register(this);
  }

  @Subscribe
  @Transactional
  public void handleCollectionCreate(CreateCollectionEntityEvent event) {
    if (event.getCollectionEntityClass().equals(Collection.class)) {
      Collection collection = (Collection) event.getNewObject();
      log.debug("Handling collection create event for collection: {}", collection.getKey());
      updateCollectionFacetLinks(collection);
    }
  }

  @Subscribe
  @Transactional
  public void handleCollectionUpdate(UpdateCollectionEntityEvent event) {
    if (event.getCollectionEntityClass().equals(Collection.class)) {
      Collection collection = (Collection) event.getNewObject();
      log.debug("Handling collection update event for collection: {}", collection.getKey());
      updateCollectionFacetLinks(collection);
    }
  }

  private void updateCollectionFacetLinks(Collection collection) {
    if (collection == null || collection.getKey() == null) {
      log.warn("Cannot update facet links for null collection or collection without key");
      return;
    }

    try {
      // Delete existing facet links for this collection
      grScicollVocabFacetMapper.deleteCollectionFacetLinks(collection.getKey());

      // Create facet links for content types (hierarchical)
      if (collection.getContentTypes() != null && !collection.getContentTypes().isEmpty()) {
        List<String> validContentTypes = collection.getContentTypes().stream()
            .filter(Objects::nonNull)
            .filter(type -> !type.trim().isEmpty())
            .collect(Collectors.toList());

        for (String contentType : validContentTypes) {
          try {
            Integer facetId = grScicollVocabFacetMapper.getFacetIdByVocabularyAndName("CollectionContentType", contentType);
            if (facetId != null) {
              grScicollVocabFacetMapper.insertCollectionFacetLink(collection.getKey(), facetId);
              log.debug("Created facet link for collection {} with content type: {}", collection.getKey(), contentType);
            } else {
              log.warn("No facet found for CollectionContentType: {}", contentType);
            }
          } catch (Exception e) {
            log.error("Error creating facet link for collection {} with content type {}: {}",
                     collection.getKey(), contentType, e.getMessage());
          }
        }
      }

      // Create facet links for preservation types (hierarchical)
      if (collection.getPreservationTypes() != null && !collection.getPreservationTypes().isEmpty()) {
        List<String> validPreservationTypes = collection.getPreservationTypes().stream()
            .filter(Objects::nonNull)
            .filter(type -> !type.trim().isEmpty())
            .collect(Collectors.toList());

        for (String preservationType : validPreservationTypes) {
          try {
            Integer facetId = grScicollVocabFacetMapper.getFacetIdByVocabularyAndName("PreservationType", preservationType);
            if (facetId != null) {
              grScicollVocabFacetMapper.insertCollectionFacetLink(collection.getKey(), facetId);
              log.debug("Created facet link for collection {} with preservation type: {}", collection.getKey(), preservationType);
            } else {
              log.warn("No facet found for PreservationType: {}", preservationType);
            }
          } catch (Exception e) {
            log.error("Error creating facet link for collection {} with preservation type {}: {}",
                     collection.getKey(), preservationType, e.getMessage());
          }
        }
      }

      log.debug("Successfully updated facet links for collection: {}", collection.getKey());

    } catch (Exception e) {
      log.error("Error updating facet links for collection {}: {}", collection.getKey(), e.getMessage(), e);
      throw new RuntimeException("Failed to update collection facet links", e);
    }
  }
}
