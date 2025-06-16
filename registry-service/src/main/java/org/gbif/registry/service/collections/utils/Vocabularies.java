package org.gbif.registry.service.collections.utils;

import com.google.common.base.Strings;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Duration;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.request.CollectionSearchRequest;
import org.gbif.api.model.collections.request.InstitutionSearchRequest;
import org.gbif.api.model.collections.request.SearchRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.vocabulary.api.ConceptListParams;
import org.gbif.vocabulary.api.ConceptView;
import org.gbif.vocabulary.client.ConceptClient;
import org.gbif.vocabulary.model.search.LookupResult;

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
