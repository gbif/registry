package org.gbif.registry.cli.vocabularysynchronizer;

import java.util.Set;

import org.gbif.common.messaging.AbstractMessageCallback;
import org.gbif.common.messaging.api.messages.VocabularyReleasedMessage;
import org.gbif.registry.service.VocabularyPostProcessor;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class VocabularySynchronizerCallback extends
  AbstractMessageCallback<VocabularyReleasedMessage> {

  private final List<VocabularyPostProcessor> vocabularyPostProcessors;
  private final Set<String> vocabulariesToProcess;

  @Override
  public void handleMessage(VocabularyReleasedMessage message) {
    log.info(
        "Received VocabularyReleasedMessage for vocabulary: {}, version: {}",
        message.getVocabularyName(),
        message.getVersion());

    if (shouldProcessVocabulary(message.getVocabularyName())) {
      try {
        log.info("Processing vocabulary: {} for synchronization.", message.getVocabularyName());

        // Find and run appropriate post-processors
        for (VocabularyPostProcessor processor : vocabularyPostProcessors) {
          if (processor.canHandle(message.getVocabularyName())) {
            log.info("Running post-processor for vocabulary: {}", message.getVocabularyName());
            int processedCount = processor.process(message.getVocabularyName());
            log.info("Successfully processed vocabulary: {} with {} items", message.getVocabularyName(), processedCount);
          }
        }

      } catch (Exception e) {
        log.error(
            "Failed to process vocabulary: {} for synchronization. URI: {}. Error: {}",
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

  private boolean shouldProcessVocabulary(String vocabularyName) {
    // Only process vocabularies that are explicitly configured
    return vocabulariesToProcess != null
        && !vocabulariesToProcess.isEmpty()
        && vocabulariesToProcess.contains(vocabularyName);
  }
}
