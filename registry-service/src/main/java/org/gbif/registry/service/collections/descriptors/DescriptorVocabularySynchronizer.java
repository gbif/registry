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
package org.gbif.registry.service.collections.descriptors;

import lombok.Getter;

import lombok.Setter;

import org.gbif.api.model.collections.descriptors.DescriptorGroup;
import org.gbif.api.model.collections.descriptors.DescriptorValidationResult;
import org.gbif.registry.persistence.mapper.collections.DescriptorsMapper;
import org.gbif.registry.persistence.mapper.collections.dto.DescriptorDto;
import org.gbif.registry.persistence.mapper.collections.dto.VerbatimDto;
import org.gbif.registry.service.VocabularyPostProcessor;
import org.gbif.registry.service.collections.utils.Vocabularies;
import org.gbif.vocabulary.client.ConceptClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for re-interpreting collection descriptors when vocabulary releases are updated.
 * This allows previously invalid vocabulary values to become valid, or previously valid
 * values to become deprecated/invalid.
 */
@Slf4j
@Service
public class DescriptorVocabularySynchronizer implements VocabularyPostProcessor {

  private final DescriptorsMapper descriptorsMapper;
  private final ConceptClient conceptClient;

  @Autowired
  public DescriptorVocabularySynchronizer(
      DescriptorsMapper descriptorsMapper, ConceptClient conceptClient) {
    this.descriptorsMapper = descriptorsMapper;
    this.conceptClient = conceptClient;
  }

  @Override
  public boolean canHandle(String vocabularyName) {
    return Vocabularies.BIOME_TYPE.equals(vocabularyName) ||
           Vocabularies.OBJECT_CLASSIFICATION.equals(vocabularyName);
  }

  @Override
  @Transactional
  public int process(String vocabularyName) {
    log.info("Processing vocabulary release for: {}", vocabularyName);
    SynchronizationResult result = reInterpretDescriptorsForVocabulary(vocabularyName);
    log.info("Vocabulary synchronization completed for {}. Updated {} out of {} descriptors",
             vocabularyName, result.getUpdatedCount(), result.getUpdatedCount());
    return 1;
  }

  /**
   * Re-interprets all descriptors to update vocabulary field values based on the latest
   * vocabulary releases. This is useful when vocabulary releases are updated and previously
   * invalid values might now be valid, or vice versa.
   *
   * @param vocabularyName The name of the vocabulary that was updated (e.g., "Biome", "ObjectClassification")
   * @return SynchronizationResult containing statistics about the re-interpretation
   */
  public SynchronizationResult reInterpretDescriptorsForVocabulary(String vocabularyName) {
    log.info("Starting re-interpretation of descriptors for vocabulary: {}", vocabularyName);

    SynchronizationResult result = new SynchronizationResult();
    result.setVocabularyName(vocabularyName);

    // Get all descriptors that might be affected by this vocabulary
    List<DescriptorDto> descriptors = descriptorsMapper.listDescriptorsWithVocabularyField(vocabularyName);
    log.info("Found {} descriptors to re-interpret for vocabulary: {}", descriptors.size(), vocabularyName);

    AtomicInteger updatedCount = new AtomicInteger(0);
    AtomicInteger errorCount = new AtomicInteger(0);

      descriptors.forEach(descriptor -> {
        try {
          boolean wasUpdated = reInterpretDescriptor(descriptor, vocabularyName);
          if (wasUpdated) {
            updatedCount.incrementAndGet();
            log.debug("Updated descriptor {} for vocabulary {}", descriptor.getKey(), vocabularyName);
          }
        } catch (Exception e) {
          errorCount.incrementAndGet();
          log.error("Error re-interpreting descriptor {} for vocabulary {}: {}",
                   descriptor.getKey(), vocabularyName, e.getMessage(), e);
        }
      });

    result.setTotalProcessed(descriptors.size());
    result.setUpdatedCount(updatedCount.get());
    result.setErrorCount(errorCount.get());

    log.info("Completed re-interpretation for vocabulary {}: {} processed, {} updated, {} errors",
             vocabularyName, result.getTotalProcessed(), result.getUpdatedCount(), result.getErrorCount());

    return result;
  }

  /**
   * Re-interprets a specific descriptor for a given vocabulary.
   * This method re-validates vocabulary fields against the latest vocabulary release
   * and updates both the interpreted values and verbatim values if needed.
   *
   * @param descriptor The descriptor to re-interpret
   * @param vocabularyName The vocabulary name
   * @return true if the descriptor was updated, false otherwise
   */
  private boolean reInterpretDescriptor(DescriptorDto descriptor, String vocabularyName) {
    boolean wasUpdated = false;

    // Get the original verbatim values
    Map<String, String> verbatimValues = getVerbatimValues(descriptor.getKey());

    log.debug("Processing descriptor {} for vocabulary '{}'", descriptor.getKey(), vocabularyName);

    if (Vocabularies.BIOME_TYPE.equals(vocabularyName)) {
      String verbatimBiomeType = verbatimValues.get("ltc:biomeType");
      if (verbatimBiomeType != null && !verbatimBiomeType.trim().isEmpty()) {
        wasUpdated = updateVocabularyField(descriptor, verbatimBiomeType,
          descriptor::setBiomeType, descriptor::getBiomeType,
            DescriptorValidationResult::getValidBiomeType, "BIOME_TYPE_VALIDATION_WARNING:",
            "Biome type", vocabularyName) || wasUpdated;
      } else {
        log.debug("Descriptor {} - No valid verbatim biome type found", descriptor.getKey());
      }
    }

    if (Vocabularies.OBJECT_CLASSIFICATION.equals(vocabularyName)) {
      String verbatimObjectClassification = verbatimValues.get("ltc:objectClassificationName");
      if (verbatimObjectClassification != null && !verbatimObjectClassification.trim().isEmpty()) {
        wasUpdated = updateVocabularyField(descriptor, verbatimObjectClassification,
          descriptor::setObjectClassificationName, descriptor::getObjectClassificationName,
            DescriptorValidationResult::getValidObjectClassification, "OBJECT_CLASSIFICATION_VALIDATION_ISSUE:",
            "Object classification", vocabularyName) || wasUpdated;
      } else {
        log.debug("Descriptor {} - No valid verbatim object classification found", descriptor.getKey());
      }
    }

    // Update the descriptor in the database if it was modified
    if (wasUpdated) {
      DescriptorGroup descriptorGroup = descriptorsMapper.getDescriptorGroup(descriptor.getDescriptorGroupKey());
      descriptorGroup.setModifiedBy("VocabularySynchronizer");
      descriptorsMapper.updateDescriptorGroup(descriptorGroup);
      descriptorsMapper.updateDescriptor(descriptor);
      log.info("Descriptor {} updated successfully for vocabulary {}", descriptor.getKey(), vocabularyName);
    }
    return wasUpdated;
  }

  /**
   * Gets the verbatim values for a descriptor.
   */
  private Map<String, String> getVerbatimValues(Long descriptorKey) {
    List<VerbatimDto> verbatimDtos = descriptorsMapper.getVerbatimValues(descriptorKey);
    return verbatimDtos.stream()
        .collect(Collectors.toMap(
            VerbatimDto::getFieldName,
            VerbatimDto::getFieldValue,
            (existing, replacement) -> existing, // Keep first value if duplicates
            LinkedHashMap::new // Preserve order
        ));
  }

  /**
   * Updates a vocabulary field for a descriptor based on re-validation.
   *
   * @param descriptor The descriptor to update
   * @param verbatimValue The verbatim value to validate
   * @param setter The setter method for the field
   * @param getter The getter method for the field
   * @param validationResultGetter The method to get the validated value from the result
   * @param warningPrefix The prefix for warnings in the issues array
   * @param fieldDisplayName The display name for logging
   * @param vocabularyName The vocabulary name for logging
   * @return true if the descriptor was updated, false otherwise
   */
  private boolean updateVocabularyField(
      DescriptorDto descriptor,
      String verbatimValue,
      java.util.function.Consumer<String> setter,
      java.util.function.Supplier<String> getter,
      java.util.function.Function<DescriptorValidationResult, String> validationResultGetter,
      String warningPrefix,
      String fieldDisplayName,
      String vocabularyName) {

    // Create a temporary descriptor for validation
    DescriptorDto tempDescriptor = new DescriptorDto();
    if (Vocabularies.BIOME_TYPE.equals(vocabularyName)) {
      tempDescriptor.setBiomeType(verbatimValue);
    } else if (Vocabularies.OBJECT_CLASSIFICATION.equals(vocabularyName)) {
      tempDescriptor.setObjectClassificationName(verbatimValue);
    }

    // Validate the value
    DescriptorValidationResult validationResult =
        Vocabularies.validateDescriptorVocabsValues(conceptClient, tempDescriptor);

    String newValue = validationResultGetter.apply(validationResult);
    String currentValue = getter.get();

    log.debug("Descriptor {} - {} validation: Current: '{}', Verbatim: '{}', New: '{}'",
             descriptor.getKey(), fieldDisplayName, currentValue, verbatimValue, newValue);

    // Update if the value is different or if there are warnings to update
    if (!Objects.equals(currentValue, newValue) || validationResult.hasIssues()) {
      setter.accept(newValue);

      // Update issues array to reflect new validation result
      List<String> currentIssues = descriptor.getIssues() != null ? new ArrayList<>(descriptor.getIssues()) : new ArrayList<>();
      currentIssues.removeIf(issue -> issue.startsWith(warningPrefix));
      if (validationResult.hasIssues()) {
        currentIssues.addAll(validationResult.getIssues());
      }

      descriptor.setIssues(currentIssues);

      log.info("{} value updated for descriptor {}: '{}' -> '{}' (verbatim: '{}')",
              fieldDisplayName, descriptor.getKey(), currentValue, newValue, verbatimValue);

      if (validationResult.hasIssues()) {
        log.info("{} validation warnings for descriptor {}: {}",
                fieldDisplayName, descriptor.getKey(), validationResult.getIssues());
      }

      return true;
    } else {
      log.debug("{} value for descriptor {} unchanged: '{}' (verbatim: '{}')",
               fieldDisplayName, descriptor.getKey(), newValue, verbatimValue);
      return false;
    }
  }

  /**
   * Result class for vocabulary synchronization operations.
   */
  @Setter
  @Getter
  public static class SynchronizationResult {
    private String vocabularyName;
    private int totalProcessed;
    private int updatedCount;
    private int errorCount;
  }
}
