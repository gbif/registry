package org.gbif.registry.cli.vocabularyfacetupdater;

import org.gbif.common.messaging.AbstractMessageCallback;
import org.gbif.common.messaging.api.messages.VocabularyReleasedMessage;
import org.gbif.registry.service.VocabularyConceptService;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class VocabularyFacetUpdaterCallback extends
  AbstractMessageCallback<VocabularyReleasedMessage> {

  private final VocabularyConceptService vocabularyConceptService;
  private final Set<String> vocabulariesToProcess; // From configuration

  @Override
  public void handleMessage(VocabularyReleasedMessage message) {
    log.info(
        "Received VocabularyReleasedMessage for vocabulary: {}, version: {}",
        message.getVocabularyName(),
        message.getVersion());

    if (vocabulariesToProcess == null
        || vocabulariesToProcess.isEmpty()
        || vocabulariesToProcess.contains(message.getVocabularyName())) {
      try {
        log.info("Processing vocabulary: {} for facet update.", message.getVocabularyName());
        // Call the service using only the vocabulary name
        vocabularyConceptService.populateConceptsForVocabulary(message.getVocabularyName());
        log.info("Successfully processed vocabulary: {} for facet update.", message.getVocabularyName());
      } catch (Exception e) {
        log.error(
            "Failed to process vocabulary: {} for facet update. URI: {}. Error: {}",
            message.getVocabularyName(),
            message.getReleaseDownloadUrl(),
            e.getMessage(),
            e);
      }
    } else {
      log.info(
          "Skipping vocabulary: {} as it is not in the configured list of vocabularies to process.",
          message.getVocabularyName());
    }
  }
}
