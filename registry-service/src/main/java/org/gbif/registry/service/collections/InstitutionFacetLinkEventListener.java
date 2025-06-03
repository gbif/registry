package org.gbif.registry.service.collections;

import java.util.Objects;

import org.gbif.api.model.collections.Institution;
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
 * Event listener that automatically manages institution_facet_links table
 * when Institution entities are created or updated.
 *
 * Only handles vocabulary fields that are actual facet parameters:
 * - Discipline (hierarchical)
 * - InstitutionType (hierarchical)
 *
 * Note: InstitutionalGovernance is excluded because it's not a facet parameter
 * in InstitutionFacetParameter enum and is typically flat (non-hierarchical).
 */
@Component
@Slf4j
public class InstitutionFacetLinkEventListener {

  private final GrScicollVocabFacetMapper grScicollVocabFacetMapper;

  @Autowired
  public InstitutionFacetLinkEventListener(GrScicollVocabFacetMapper grScicollVocabFacetMapper, EventManager eventManager) {
    this.grScicollVocabFacetMapper = grScicollVocabFacetMapper;
    eventManager.register(this);
  }

  @Subscribe
  @Transactional
  public void handleInstitutionCreate(CreateCollectionEntityEvent<Institution> event) {
    log.info("Handling Institution create event for: {}", event.getNewObject().getKey());
    updateInstitutionFacetLinks(event.getNewObject());
  }

  @Subscribe
  @Transactional
  public void handleInstitutionUpdate(UpdateCollectionEntityEvent<Institution> event) {
    log.info("Handling Institution update event for: {}", event.getNewObject().getKey());

    // Delete existing facet links for this institution
    grScicollVocabFacetMapper.deleteInstitutionFacetLinks(event.getNewObject().getKey());
    log.debug("Deleted existing facet links for institution: {}", event.getNewObject().getKey());

    // Create new facet links based on current values
    updateInstitutionFacetLinks(event.getNewObject());
  }

  private void updateInstitutionFacetLinks(Institution institution) {
    // Handle disciplines (hierarchical facet parameter)
    createFacetLinks(institution, "Discipline", institution.getDisciplines());

    // Handle types (hierarchical facet parameter)
    createFacetLinks(institution, "InstitutionType", institution.getTypes());

    // Note: InstitutionalGovernance is intentionally excluded because:
    // 1. It's not in InstitutionFacetParameter enum
    // 2. It's typically flat (non-hierarchical)
    // 3. Filtering still works via array-based queries
  }

  private void createFacetLinks(Institution institution, String vocabularyName, List<String> values) {
    if (values == null || values.isEmpty()) {
      log.debug("No {} values found for institution: {}", vocabularyName, institution.getKey());
      return;
    }

    // Get facet IDs for the vocabulary values
    List<Integer> facetIds = values.stream()
        .map(value -> grScicollVocabFacetMapper.getFacetIdByVocabularyAndName(vocabularyName, value))
        .filter(Objects::nonNull) // Filter out null values for unknown concepts
        .collect(Collectors.toList());

    if (facetIds.isEmpty()) {
      log.warn("No valid facets found for institution {} values: {} for institution: {}",
               vocabularyName, values, institution.getKey());
      return;
    }

    // Insert links for all valid facet IDs
    for (Integer facetId : facetIds) {
      grScicollVocabFacetMapper.insertInstitutionFacetLink(institution.getKey(), facetId);
      log.debug("Created {} facet link for institution: {} with facet ID: {}",
                vocabularyName, institution.getKey(), facetId);
    }

    log.info("Successfully created {} {} facet links for institution: {}",
             facetIds.size(), vocabularyName, institution.getKey());
  }
}
