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
import org.gbif.vocabulary.api.ConceptView;
import org.gbif.vocabulary.client.ConceptClient;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;

import com.google.common.base.Strings;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Vocabularies {

  static final Map<String, Function<Institution, java.util.Collection<String>>>
      INSTITUTION_VOCAB_FIELDS = new HashMap<>();
  static final Map<String, Function<Collection, java.util.Collection<String>>>
      COLLECTION_VOCAB_FIELDS = new HashMap<>();

  public static final String DISCIPLINE = "Discipline";

  public static final String INSTITUTION_TYPE = "InstitutionType";

  public static final String INSTITUTIONAL_GOVERNANCE = "InstitutionalGovernance";

  public static final String COLLECTION_CONTENT_TYPE = "CollectionContentType";

  public static final String ACCESSION_STATUS = "AccessionStatus";

  public static final String PRESERVATION_TYPE = "PreservationType";

  static {
    INSTITUTION_VOCAB_FIELDS.put(DISCIPLINE, Institution::getDisciplines);
    INSTITUTION_VOCAB_FIELDS.put(INSTITUTION_TYPE, Institution::getTypes);
    INSTITUTION_VOCAB_FIELDS.put(
        INSTITUTIONAL_GOVERNANCE, Institution::getInstitutionalGovernances);

    COLLECTION_VOCAB_FIELDS.put(COLLECTION_CONTENT_TYPE, Collection::getContentTypes);
    COLLECTION_VOCAB_FIELDS.put(
        ACCESSION_STATUS, c -> Collections.singletonList(c.getAccessionStatus()));
    COLLECTION_VOCAB_FIELDS.put(PRESERVATION_TYPE, Collection::getPreservationTypes);
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
