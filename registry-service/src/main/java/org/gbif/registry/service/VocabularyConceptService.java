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
import org.gbif.registry.persistence.mapper.GrScicollVocabConceptMapper;
import org.gbif.vocabulary.api.ConceptListParams;
import org.gbif.vocabulary.api.ConceptView;
import org.gbif.vocabulary.client.ConceptClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class VocabularyConceptService {

  private final GrScicollVocabConceptMapper grScicollVocabConceptMapper;
  private final ConceptClient conceptClient;
  private static final int DEFAULT_PAGE_SIZE = 100; // Default size for paging

  @Autowired
  public VocabularyConceptService(GrScicollVocabConceptMapper grScicollVocabConceptMapper, ConceptClient conceptClient) {
    this.grScicollVocabConceptMapper = grScicollVocabConceptMapper;
    this.conceptClient = conceptClient;
  }

  @Transactional
  public void populateConceptsForVocabulary(String vocabularyName) throws Exception {
    log.info("Starting concept population for vocabulary: {} using ConceptClient", vocabularyName);

    // 1. Fetch all concepts and their labels for the vocabulary using ConceptClient
    List<ConceptView> allConcepts = fetchAllConcepts(vocabularyName);

    if (allConcepts.isEmpty()) {
      log.warn("No concepts found for vocabulary: {}. Deleting existing concepts if any.", vocabularyName);
      grScicollVocabConceptMapper.deleteByVocabularyName(vocabularyName);
      return;
    }

    // Store concepts by their name in a map for easy parent lookup during path building
    Map<String, ConceptView> conceptViewMap =
      allConcepts.stream()
        .filter(cv -> cv.getConcept() != null && cv.getConcept().getName() != null)
        .collect(Collectors.toMap(cv -> cv.getConcept().getName(), cv -> cv, (cv1, cv2) -> cv1)); // Merge strategy for duplicate keys


    // 2. Create Concept DTOs
    List<GrSciCollVocabConceptDto> conceptDtos = new ArrayList<>();
    for (ConceptView conceptView : allConcepts) {
      if (conceptView.getConcept() == null || conceptView.getConcept().getName() == null) {
        log.warn("Skipping concept view with null concept or null concept name: {}", conceptView);
        continue;
      }

      String displayName = conceptView.getConcept().getName();
      String ltreePath = buildLtreePath(conceptView, conceptViewMap, vocabularyName);

      conceptDtos.add(
        GrSciCollVocabConceptDto.builder()
          .vocabularyName(vocabularyName)
          .name(displayName)
          .path(ltreePath)
          .build());
    }

    // 3. Delete existing concepts and add new ones
    log.debug("Deleting existing concepts for vocabulary: {}", vocabularyName);
    grScicollVocabConceptMapper.deleteByVocabularyName(vocabularyName);

    log.debug("Inserting {} new concept entries for vocabulary: {}", conceptDtos.size(), vocabularyName);
    for (GrSciCollVocabConceptDto dto : conceptDtos) {
      grScicollVocabConceptMapper.create(dto);
    }

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

  private String buildLtreePath(ConceptView currentConceptView, Map<String, ConceptView> conceptViewMap, String vocabularyName) {
    LinkedList<String> pathParts = new LinkedList<>();
    ConceptView tempConceptView = currentConceptView;
    int depth = 0;
    final int MAX_DEPTH = 20;

    // Map to look up concepts by their key (Integer) for parent traversal
    Map<Long, ConceptView> conceptViewKeyMap = conceptViewMap.values().stream()
      .filter(cv -> cv.getConcept() != null && cv.getConcept().getKey() != null)
      .collect(Collectors.toMap(cv -> cv.getConcept().getKey(), cv -> cv, (cv1, cv2) -> cv1));

    while (tempConceptView != null && tempConceptView.getConcept() != null && depth < MAX_DEPTH) {
      pathParts.addFirst(sanitizeLtreeLabel(tempConceptView.getConcept().getName()));

      Long parentKey = tempConceptView.getConcept().getParentKey(); // Use getParentKey()
      if (parentKey == null) {
        break;
      }

      tempConceptView = conceptViewKeyMap.get(parentKey); // Lookup by key
      if (tempConceptView == null) {
        log.warn(
          "Parent concept with key '{}' not found in conceptViewKeyMap during path construction for concept '{}' in vocabulary {}.",
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
}
