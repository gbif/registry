package org.gbif.registry.service.collections.utils;

import org.elasticsearch.common.Strings;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Institution;
import org.gbif.vocabulary.api.ConceptView;
import org.gbif.vocabulary.client.ConceptClient;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;

public class Vocabularies {

  static final Map<String, Function<Institution, java.util.Collection<String>>>
      INSTITUTION_VOCAB_FIELDS = new HashMap<>();
  static final Map<String, Function<Collection, java.util.Collection<String>>>
      COLLECTION_VOCAB_FIELDS = new HashMap<>();

  static {
    INSTITUTION_VOCAB_FIELDS.put("Discipline", Institution::getDisciplines);
    INSTITUTION_VOCAB_FIELDS.put("InstitutionType", Institution::getTypes);
    INSTITUTION_VOCAB_FIELDS.put(
        "InstitutionalGovernance", Institution::getInstitutionalGovernances);

    COLLECTION_VOCAB_FIELDS.put("CollectionContentType", Collection::getContentTypes);
    COLLECTION_VOCAB_FIELDS.put(
        "AccessionStatus", c -> Collections.singletonList(c.getAccessionStatus()));
    COLLECTION_VOCAB_FIELDS.put("PreservationType", Collection::getPreservationTypes);
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

  private static void checkConcept(
      ConceptClient conceptClient, String vocabName, String conceptValue, StringJoiner errors) {
    ConceptView conceptFound =
        conceptClient.getFromLatestRelease(vocabName, conceptValue, false, false);

    if (conceptFound == null) {
      errors.add(conceptValue + " is not a concept of the " + vocabName + " vocabulary");
    }
  }
}
