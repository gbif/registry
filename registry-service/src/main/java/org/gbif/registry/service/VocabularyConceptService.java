package org.gbif.registry.service;

// import java.net.URI; // No longer needed if ConceptClient handles endpoint resolution

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.registry.persistence.mapper.dto.GrSciCollVocabConceptDto;
import org.gbif.registry.persistence.mapper.dto.GrsciCollConceptLinkDto;
import org.gbif.registry.persistence.mapper.GrScicollVocabConceptMapper;
import org.gbif.vocabulary.api.ConceptListParams;
import org.gbif.vocabulary.api.ConceptView;
import org.gbif.vocabulary.client.ConceptClient;
import org.gbif.vocabulary.model.Concept;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class VocabularyConceptService implements VocabularyPostProcessor {

  private final GrScicollVocabConceptMapper grScicollVocabConceptMapper;
  private final ConceptClient conceptClient;
  private static final int DEFAULT_PAGE_SIZE = 100; // Default size for paging

  @Autowired
  public VocabularyConceptService(GrScicollVocabConceptMapper grScicollVocabConceptMapper, ConceptClient conceptClient) {
    this.grScicollVocabConceptMapper = grScicollVocabConceptMapper;
    this.conceptClient = conceptClient;
  }

  @Override
  public boolean canHandle(String vocabularyName) {
    // Handle all vocabularies except DatasetCategory (which is handled by DatasetCategoryService)
    return !"DatasetCategory".equals(vocabularyName);
  }

  @Override
  @Transactional
  public int process(String vocabularyName) {
    try {
      populateConceptsForVocabulary(vocabularyName);
      log.info("Successfully processed vocabulary: {}", vocabularyName);
      return 1; // Return 1 to indicate successful processing
    } catch (Exception e) {
      log.error("Error processing vocabulary: {}", vocabularyName, e);
      throw new RuntimeException("Failed to process vocabulary: " + vocabularyName, e);
    }
  }

  public void populateConceptsForVocabulary(String vocabularyName) throws Exception {
    log.info("Starting concept population for vocabulary: {} using ConceptClient", vocabularyName);

    // 1. Get current concepts from registry
    List<GrSciCollVocabConceptDto> currentConcepts = grScicollVocabConceptMapper.getAllConceptsByVocabulary(vocabularyName);
    // 2. Get latest concepts from vocabulary release
    List<ConceptView> latestConcepts = fetchAllConcepts(vocabularyName);

    // Store concepts by their key for easy lookup during path building
    Map<Long, ConceptView> conceptViewMap = latestConcepts.stream()
        .filter(cv -> cv.getConcept() != null && cv.getConcept().getKey() != null)
        .collect(Collectors.toMap(cv -> cv.getConcept().getKey(), cv -> cv, (cv1, cv2) -> cv1));

    log.info("Found {} current concepts and {} latest concepts for vocabulary: {}",
             currentConcepts.size(), latestConcepts.size(), vocabularyName);

    // 3. Sync concepts using create + update approach
    for (ConceptView latestConceptView : latestConcepts) {
      if (latestConceptView.getConcept() == null || latestConceptView.getConcept().getKey() == null) {
        log.warn("Skipping concept view with null concept or null concept key: {}", latestConceptView);
        continue;
      }

      Concept concept = latestConceptView.getConcept();
      String ltreePath = buildLtreePath(latestConceptView, conceptViewMap, vocabularyName);

      GrSciCollVocabConceptDto conceptDto = GrSciCollVocabConceptDto.builder()
          .conceptKey(concept.getKey())
          .vocabularyKey(concept.getVocabularyKey())
          .vocabularyName(vocabularyName)
          .name(concept.getName())
          .path(ltreePath)
          .parentKey(concept.getParentKey())
          .replacedByKey(concept.getReplacedByKey())
          .deprecated(concept.getDeprecated() != null ? concept.getDeprecated().toLocalDateTime() : null)
          .deprecatedBy(concept.getDeprecatedBy())
          .build();

      // Try to create new concept (will do nothing if already exists)
      grScicollVocabConceptMapper.create(conceptDto);

      // Update changeable fields (deprecated, deprecated_by, replaced_by_key)
      grScicollVocabConceptMapper.update(conceptDto);

      log.debug("Processed concept: {} (key: {}) for vocabulary: {}",
               concept.getName(), concept.getKey(), vocabularyName);
    }

    // 4. Update entity concept links to use active concepts only
    updateEntityConceptLinks(vocabularyName);

    log.info("Successfully populated concepts for vocabulary: {}", vocabularyName);
  }

  private List<ConceptView> fetchAllConcepts(String vocabularyName) {
    List<ConceptView> allConcepts = new ArrayList<>();
    PagingRequest page = new PagingRequest(0, DEFAULT_PAGE_SIZE);
    PagingResponse<ConceptView> response;

    do {
      ConceptListParams params =
        ConceptListParams.builder().includeChildren(true).includeParents(true).offset(page.getOffset()).limit(page.getLimit()).build();
      log.debug("Fetching concepts for vocabulary: {}, page offset: {}, limit: {}", vocabularyName, page.getOffset(), page.getLimit());
      response = conceptClient.listConceptsLatestRelease(vocabularyName, params);

      if (response == null || response.getResults() == null) {
        log.warn("Received null response or null results from ConceptClient for vocabulary: {} at page: {}", vocabularyName, page);
        break; // Exit loop on error
      }

      allConcepts.addAll(response.getResults());
      log.debug("Fetched {} concepts in this page, total so far: {}", response.getResults().size(), allConcepts.size());

      // Update offset for next page
      page = new PagingRequest(page.getOffset() + response.getResults().size(), DEFAULT_PAGE_SIZE);
    } while (!response.isEndOfRecords());

    log.info("Fetched a total of {} concepts for vocabulary: {}", allConcepts.size(), vocabularyName);
    return allConcepts;
  }

  private String sanitizeLtreeLabel(String name) {
    if (name == null || name.trim().isEmpty()) {
      return "_unknown_";
    }
    return name.trim().replaceAll("[^a-zA-Z0-9_\\s-]", "").replaceAll("\\s+", "_").toLowerCase();
  }

  private String buildLtreePath(ConceptView currentConceptView, Map<Long, ConceptView> conceptViewMap, String vocabularyName) {
    LinkedList<String> pathParts = new LinkedList<>();
    ConceptView tempConceptView = currentConceptView;
    int depth = 0;
    final int MAX_DEPTH = 20;

    while (tempConceptView != null && tempConceptView.getConcept() != null && depth < MAX_DEPTH) {
      pathParts.addFirst(sanitizeLtreeLabel(tempConceptView.getConcept().getName()));

      Long parentKey = tempConceptView.getConcept().getParentKey();
      if (parentKey == null) {
        break;
      }

      tempConceptView = conceptViewMap.get(parentKey);
      if (tempConceptView == null) {
        log.warn(
          "Parent concept with key '{}' not found in conceptViewMap during path construction for concept '{}' in vocabulary {}.",
          parentKey,
          currentConceptView.getConcept().getName(),
          vocabularyName);
        pathParts.addFirst("_orphanparent_");
        break;
      }
      depth++;
    }
    if (depth >= MAX_DEPTH) {
      log.warn("Reached maximum path depth for concept {} (name: {}), vocabulary {}. Path might be truncated or incorrect due to potential circular dependency.",
        currentConceptView.getConcept().getKey(), currentConceptView.getConcept().getName(), vocabularyName);
    }
    return String.join(".", pathParts);
  }

  /**
   * Updates entity concept links to use only active (non-deprecated) concepts.
   * If an entity is linked to a deprecated concept, attempts to link to its replacement.
   */
  private void updateEntityConceptLinks(String vocabularyName) {
    log.debug("Updating entity concept links for vocabulary: {}", vocabularyName);

    // Update institution links
    List<GrsciCollConceptLinkDto> institutionLinks = grScicollVocabConceptMapper.getInstitutionConceptLinksByVocabulary(vocabularyName);
    for (GrsciCollConceptLinkDto link : institutionLinks) {
      Long activeConceptKey = grScicollVocabConceptMapper.getActiveConceptKey(link.getConceptKey());

      if (activeConceptKey != null && !activeConceptKey.equals(link.getConceptKey())) {
        // The concept this entity is linked to is deprecated, update to active concept
        grScicollVocabConceptMapper.deleteInstitutionConcept(link.getEntityKey(), link.getConceptKey());
        grScicollVocabConceptMapper.insertInstitutionConcept(link.getEntityKey(), activeConceptKey);
        log.debug("Updated institution {} concept link from {} to {} for vocabulary: {}",
                 link.getEntityKey(), link.getConceptKey(), activeConceptKey, vocabularyName);
      } else if (activeConceptKey == null) {
        // No active concept found - remove the link
        grScicollVocabConceptMapper.deleteInstitutionConcept(link.getEntityKey(), link.getConceptKey());
        log.debug("Removed institution {} concept link for deprecated concept with no replacement: {}",
                 link.getEntityKey(), link.getConceptKey());
      }
    }

    // Update collection links (similar logic)
    List<GrsciCollConceptLinkDto> collectionLinks = grScicollVocabConceptMapper.getCollectionConceptLinksByVocabulary(vocabularyName);
    for (GrsciCollConceptLinkDto link : collectionLinks) {
      Long activeConceptKey = grScicollVocabConceptMapper.getActiveConceptKey(link.getConceptKey());

      if (activeConceptKey != null && !activeConceptKey.equals(link.getConceptKey())) {
        grScicollVocabConceptMapper.deleteCollectionConcept(link.getEntityKey(), link.getConceptKey());
        grScicollVocabConceptMapper.insertCollectionConcept(link.getEntityKey(), activeConceptKey);
        log.debug("Updated collection {} concept link from {} to {} for vocabulary: {}",
                 link.getEntityKey(), link.getConceptKey(), activeConceptKey, vocabularyName);
      } else if (activeConceptKey == null) {
        grScicollVocabConceptMapper.deleteCollectionConcept(link.getEntityKey(), link.getConceptKey());
        log.debug("Removed collection {} concept link for deprecated concept with no replacement: {}",
                 link.getEntityKey(), link.getConceptKey());
      }
    }

    log.debug("Finished updating entity concept links for vocabulary: {}", vocabularyName);
  }
}
