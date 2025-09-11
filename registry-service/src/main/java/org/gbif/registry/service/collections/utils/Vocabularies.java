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
package org.gbif.registry.service.collections.utils;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.request.CollectionSearchRequest;
import org.gbif.api.model.collections.request.InstitutionSearchRequest;
import org.gbif.api.model.collections.request.SearchRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.collections.descriptors.DescriptorValidationResult;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.vocabulary.DescriptorIssue;
import org.gbif.registry.persistence.mapper.collections.dto.DescriptorDto;
import org.gbif.vocabulary.api.ConceptListParams;
import org.gbif.vocabulary.api.ConceptView;
import org.gbif.vocabulary.client.ConceptClient;
import org.gbif.vocabulary.model.search.LookupResult;

import java.time.Duration;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;

import com.google.common.base.Strings;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Vocabularies {

  private static final Retry RETRY =
      Retry.of(
          "vocabularyCall",
          RetryConfig.custom()
              .maxAttempts(7)
              .intervalFunction(IntervalFunction.ofExponentialBackoff(Duration.ofSeconds(1)))
              .build());

  static final Map<String, Function<Institution, java.util.Collection<String>>>
      INSTITUTION_VOCAB_FIELDS = new HashMap<>();
  static final Map<String, Function<Collection, java.util.Collection<String>>>
      COLLECTION_VOCAB_FIELDS = new HashMap<>();
  static final Map<String, Function<DescriptorDto, String>>
      DESCRIPTOR_VOCAB_FIELDS = new HashMap<>();
  static final List<SearchRequestField<InstitutionSearchRequest>>
      INSTITUTION_SEARCH_REQ_VOCAB_FIELDS = new ArrayList<>();
  static final List<SearchRequestField<CollectionSearchRequest>>
      COLLECTION_SEARCH_REQ_VOCAB_FIELDS = new ArrayList<>();

  public static final String DISCIPLINE = "Discipline";

  public static final String INSTITUTION_TYPE = "InstitutionType";

  public static final String INSTITUTIONAL_GOVERNANCE = "InstitutionalGovernance";

  public static final String COLLECTION_CONTENT_TYPE = "CollectionContentType";

  public static final String ACCESSION_STATUS = "AccessionStatus";

  public static final String PRESERVATION_TYPE = "PreservationType";
  public static final String TYPE_STATUS = "TypeStatus";

  public static final String COLLECTION_DESCRIPTOR_GROUP_TYPE = "CollectionDescriptorGroupTypes";

  // Collection descriptor vocabulary fields
  public static final String BIOME_TYPE = "BiomeType";
  public static final String OBJECT_CLASSIFICATION = "ObjectClassificationName";

  // Dataset vocabulary fields
  public static final String DATASET_CATEGORY = "DatasetCategory";

  // Map of vocabulary names to field getters for datasets
  private static final Map<String, Function<Dataset, java.util.Collection<String>>> DATASET_VOCAB_FIELDS = new HashMap<>();

  private static final Cache<String, Set<String>> childrenConceptsCache =
      new Cache2kBuilder<String, Set<String>>() {}.eternal(true).build();

  static {
    INSTITUTION_VOCAB_FIELDS.put(DISCIPLINE, Institution::getDisciplines);
    INSTITUTION_VOCAB_FIELDS.put(INSTITUTION_TYPE, Institution::getTypes);
    INSTITUTION_VOCAB_FIELDS.put(
        INSTITUTIONAL_GOVERNANCE, Institution::getInstitutionalGovernances);

    COLLECTION_VOCAB_FIELDS.put(COLLECTION_CONTENT_TYPE, Collection::getContentTypes);
    COLLECTION_VOCAB_FIELDS.put(
        ACCESSION_STATUS, c -> Collections.singletonList(c.getAccessionStatus()));
    COLLECTION_VOCAB_FIELDS.put(PRESERVATION_TYPE, Collection::getPreservationTypes);

    DESCRIPTOR_VOCAB_FIELDS.put(BIOME_TYPE, DescriptorDto::getBiomeType);
    DESCRIPTOR_VOCAB_FIELDS.put(OBJECT_CLASSIFICATION, DescriptorDto::getObjectClassificationName);

    DATASET_VOCAB_FIELDS.put(DATASET_CATEGORY, d -> d.getCategory() != null ? new ArrayList<>(d.getCategory()) : Collections.emptyList());

    INSTITUTION_SEARCH_REQ_VOCAB_FIELDS.add(
        SearchRequestField.of(
            DISCIPLINE,
            InstitutionSearchRequest::getDisciplines,
            InstitutionSearchRequest::setDisciplines));
    INSTITUTION_SEARCH_REQ_VOCAB_FIELDS.add(
        SearchRequestField.of(
            INSTITUTION_TYPE,
            InstitutionSearchRequest::getType,
            InstitutionSearchRequest::setType));
    INSTITUTION_SEARCH_REQ_VOCAB_FIELDS.add(
        SearchRequestField.of(
            INSTITUTIONAL_GOVERNANCE,
            InstitutionSearchRequest::getInstitutionalGovernance,
            InstitutionSearchRequest::setInstitutionalGovernance));

    COLLECTION_SEARCH_REQ_VOCAB_FIELDS.add(
        SearchRequestField.of(
            COLLECTION_CONTENT_TYPE,
            CollectionSearchRequest::getContentTypes,
            CollectionSearchRequest::setContentTypes));
    COLLECTION_SEARCH_REQ_VOCAB_FIELDS.add(
        SearchRequestField.of(
            ACCESSION_STATUS,
            CollectionSearchRequest::getAccessionStatus,
            CollectionSearchRequest::setAccessionStatus));
    COLLECTION_SEARCH_REQ_VOCAB_FIELDS.add(
        SearchRequestField.of(
            PRESERVATION_TYPE,
            CollectionSearchRequest::getPreservationTypes,
            CollectionSearchRequest::setPreservationTypes));
  }

  public static <T extends CollectionEntity> void checkVocabsValues(
      ConceptClient conceptClient, T entity) {
    StringJoiner errors = new StringJoiner(";\n");
    if (entity instanceof Institution) {
      INSTITUTION_VOCAB_FIELDS.forEach(
          (vocabName, getter) ->
              getter.apply((Institution) entity).stream()
                  .filter(s -> !Strings.isNullOrEmpty(s))
                  .forEach(
                      conceptValue ->
                          checkConcept(conceptClient, vocabName, conceptValue, errors)));
    } else if (entity instanceof Collection) {
      COLLECTION_VOCAB_FIELDS.forEach(
          (vocabName, getter) ->
              getter.apply((Collection) entity).stream()
                  .filter(s -> !Strings.isNullOrEmpty(s))
                  .forEach(
                      conceptValue ->
                          checkConcept(conceptClient, vocabName, conceptValue, errors)));
    }

    if (errors.length() > 0) {
      throw new IllegalArgumentException(errors.toString());
    }
  }

  public static void checkDatasetVocabsValues(ConceptClient conceptClient, Dataset dataset) {
    StringJoiner errors = new StringJoiner(";\n");
    DATASET_VOCAB_FIELDS.forEach(
        (vocabName, getter) ->
            getter.apply(dataset).stream()
                .filter(s -> !Strings.isNullOrEmpty(s))
                .forEach(
                    conceptValue ->
                        checkConcept(conceptClient, vocabName, conceptValue, errors)));

    if (errors.length() > 0) {
      throw new IllegalArgumentException(errors.toString());
    }
  }

  public static void checkDescriptorGroupTags(ConceptClient conceptClient, Set<String> tags) {
    if (tags != null && !tags.isEmpty()) {
      StringJoiner errors = new StringJoiner(";\n");
      tags.stream()
          .filter(s -> !Strings.isNullOrEmpty(s))
          .forEach(
              tag ->
                  checkConcept(conceptClient, COLLECTION_DESCRIPTOR_GROUP_TYPE, tag, errors));
      if (errors.length() > 0) {
        throw new IllegalArgumentException(errors.toString());
      }
    }
  }

  /**
   * Validates descriptor vocabulary fields gracefully - invalid values are simply ignored
   * rather than causing the entire operation to fail.
   * This method checks both concept names and labels/hidden labels for matches.
   *
   * @param conceptClient The concept client for vocabulary validation
   * @param descriptor The descriptor to validate
   * @return ValidationResult containing valid values and any warnings
   */
  public static DescriptorValidationResult validateDescriptorVocabsValues(
      ConceptClient conceptClient, DescriptorDto descriptor) {

    DescriptorValidationResult result = DescriptorValidationResult.builder().build();

    // Validate all descriptor vocabulary fields
    DESCRIPTOR_VOCAB_FIELDS.forEach((vocabName, getter) -> {
      String fieldValue = getter.apply(descriptor);
      if (!Strings.isNullOrEmpty(fieldValue)) {
        String validValue = findValidConceptName(conceptClient, vocabName, fieldValue, result);

        // Set the valid value in the result based on the vocabulary type
        if (BIOME_TYPE.equals(vocabName)) {
          result.setValidBiomeType(validValue);
        } else if (OBJECT_CLASSIFICATION.equals(vocabName)) {
          result.setValidObjectClassification(validValue);
        }
      }
    });

    return result;
  }


  /**
   * Gets the field name for a vocabulary (used in warning messages).
   */
  public static String getFieldNameForVocabulary(String vocabularyName) {
    if (BIOME_TYPE.equals(vocabularyName)) {
      return "ltc:biomeType";
    } else if (OBJECT_CLASSIFICATION.equals(vocabularyName)) {
      return "ltc:objectClassificationName";
    }
    return vocabularyName;
  }

  /**
   * Finds a valid concept name by checking both direct concept names and labels/hidden labels.
   *
   * @param conceptClient The concept client
   * @param vocabularyName The vocabulary name
   * @param inputValue The input value to validate
   * @param result The validation result to add warnings to
   * @return The valid concept name, or null if not found
   */
  private static String findValidConceptName(ConceptClient conceptClient, String vocabularyName,
                                           String inputValue, DescriptorValidationResult result) {

    // First try direct concept name lookup
    ConceptView directConcept = getConceptLatestRelease(vocabularyName, inputValue, conceptClient);
    if (directConcept != null && directConcept.getConcept().getDeprecated() == null) {
      return directConcept.getConcept().getName();
    } else if (directConcept != null && directConcept.getConcept().getDeprecated() != null) {
      result.addIssue(DescriptorIssue.VOCAB_VALUE_DEPRECATED.getId());
      return directConcept.getConcept().getName();
    }

    // If direct lookup fails, try lookup through labels and hidden labels
    List<LookupResult> lookupResults = lookupLatestRelease(vocabularyName, inputValue, conceptClient);

    if (lookupResults != null && !lookupResults.isEmpty()) {
      // Use the first match
      LookupResult match = lookupResults.get(0);

      if (match.getConceptName() != null) {
        // Check if the matched concept is deprecated
        ConceptView matchedConcept = getConceptLatestRelease(vocabularyName, match.getConceptName(), conceptClient);
        if (matchedConcept != null && matchedConcept.getConcept().getDeprecated() == null) {
          result.addIssue(DescriptorIssue.VOCAB_VALUE_MATCHED_LABEL.getId());
          return match.getConceptName();
        } else if (matchedConcept != null && matchedConcept.getConcept().getDeprecated() != null) {
          result.addIssue(DescriptorIssue.VOCAB_VALUE_MATCHED_DEPRECATED_LABEL.getId());
          return match.getConceptName();
        }
      }
    }

    // No valid match found
    result.addIssue(DescriptorIssue.VOCAB_VALUE_NOT_FOUND.getId());
    return null;
  }

  private static void checkConcept(
      ConceptClient conceptClient, String vocabName, String conceptValue, StringJoiner errors) {
    ConceptView conceptFound =
        Retry.decorateSupplier(
                RETRY,
                () -> conceptClient.getFromLatestRelease(vocabName, conceptValue, false, false))
            .get();

    if (conceptFound == null) {
      errors.add(conceptValue + " is not a concept of the " + vocabName + " vocabulary");
    } else if (conceptFound.getConcept() != null && conceptFound.getConcept().getDeprecated() != null) {
      errors.add(conceptValue + " is a deprecated concept in the " + vocabName + " vocabulary");
    }
  }

  public static <T extends SearchRequest> void addChildrenConcepts(
      T request, ConceptClient conceptClient) {

    BiFunction<String, List<String>, List<String>> handleValues =
        (vocabName, values) -> {
          if (values == null) {
            return Collections.emptyList();
          }

          Set<String> allConceptsAndChildren = new HashSet<>(values);
          values.stream()
              .filter(s -> !Strings.isNullOrEmpty(s))
              .forEach(
                  conceptValue ->
                      allConceptsAndChildren.addAll(
                          childrenConceptsCache.computeIfAbsent(
                              vocabName + conceptValue,
                              k ->
                                  findChildren(
                                      conceptClient, vocabName, conceptValue, new HashSet<>()))));
          return new ArrayList<>(allConceptsAndChildren);
        };

    if (request instanceof InstitutionSearchRequest) {
      INSTITUTION_SEARCH_REQ_VOCAB_FIELDS.forEach(
          f -> {
            InstitutionSearchRequest institutionSearchRequest = (InstitutionSearchRequest) request;
            List<String> allConceptsAndChildren =
                handleValues.apply(f.vocabName, f.getter.apply(institutionSearchRequest));
            f.setter.accept(institutionSearchRequest, allConceptsAndChildren);
          });
    } else if (request instanceof CollectionSearchRequest) {
      COLLECTION_SEARCH_REQ_VOCAB_FIELDS.forEach(
          f -> {
            CollectionSearchRequest collectionSearchRequest = (CollectionSearchRequest) request;
            List<String> allConceptsAndChildren =
                handleValues.apply(f.vocabName, f.getter.apply(collectionSearchRequest));
            f.setter.accept(collectionSearchRequest, allConceptsAndChildren);
          });
    }
  }

  private static Set<String> findChildren(
      ConceptClient conceptClient, String vocabName, String conceptName, Set<String> allChildren) {
    PagingResponse<ConceptView> result =
        Retry.decorateSupplier(
                RETRY,
                () ->
                    conceptClient.listConceptsLatestRelease(
                        vocabName,
                        ConceptListParams.builder()
                            .name(conceptName)
                            .includeChildren(true)
                            .build()))
            .get();

    if (result == null
        ||result.getResults() == null
        || result.getResults().isEmpty()
        || result.getResults().get(0).getChildren() == null
        || result.getResults().get(0).getChildren().isEmpty()) {
      return allChildren;
    }

    result
        .getResults()
        .get(0)
        .getChildren()
        .forEach(
            ch -> {
              allChildren.add(ch);
              findChildren(conceptClient, vocabName, ch, allChildren);
            });

    return allChildren;
  }

  public static List<String> getVocabularyConcepts(String vocabulary, ConceptClient conceptClient) {
    int limit = 100;
    long offset = 0;
    PagingResponse<ConceptView> response;
    List<String> concepts = new ArrayList<>();
    do {
      ConceptListParams params = ConceptListParams.builder().limit(limit).offset(offset).build();
      response =
          Retry.decorateSupplier(
                  RETRY, () -> conceptClient.listConceptsLatestRelease(vocabulary, params))
              .get();
      response.getResults().forEach(r -> concepts.add(r.getConcept().getName()));
      offset += limit;
    } while (!response.isEndOfRecords());

    return concepts;
  }

  public static ConceptView getConceptLatestRelease(
      String vocabulary, String conceptName, ConceptClient conceptClient) {
    return Retry.decorateSupplier(
            RETRY, () -> conceptClient.getFromLatestRelease(vocabulary, conceptName, false, false))
        .get();
  }

  public static List<LookupResult> lookupLatestRelease(
      String vocabulary, String query, ConceptClient conceptClient) {
    return Retry.decorateSupplier(
            RETRY,
            () ->
                conceptClient.lookupInLatestRelease(
                    vocabulary, ConceptClient.LookupParams.of(query, null)))
        .get();
  }

  public static Set<String> getChildrenConcepts(
      String vocabulary, String conceptName, ConceptClient conceptClient) {
    return childrenConceptsCache.computeIfAbsent(
        vocabulary + conceptName,
        k -> findChildren(conceptClient, vocabulary, conceptName, new HashSet<>()));
  }

  @AllArgsConstructor(staticName = "of")
  @NoArgsConstructor
  private static class SearchRequestField<T extends SearchRequest> {
    String vocabName;
    Function<T, List<String>> getter;
    BiConsumer<T, List<String>> setter;
  }
}
