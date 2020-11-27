/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.registry.service.collections.merge;

import org.gbif.api.model.collections.AlternativeCode;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Person;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.persistence.mapper.collections.PersonMapper;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Service to merge duplicated {@link Collection}. */
@Service
public class CollectionMergeService extends BaseMergeService<Collection> {

  @Autowired
  protected CollectionMergeService(
      CollectionMapper collectionMapper,
      IdentifierMapper identifierMapper,
      PersonMapper personMapper) {
    super(
        collectionMapper,
        collectionMapper,
        collectionMapper,
        identifierMapper,
        collectionMapper,
        personMapper);
  }

  @Override
  void checkExtraPreconditions(Collection entityToReplace, Collection replacement) {
    if (entityToReplace.getInstitutionKey() != null
        && !entityToReplace.getInstitutionKey().equals(replacement.getInstitutionKey())) {
      throw new IllegalArgumentException(
          "Cannot do the replacement because the collections don't belong to the same institution");
    }
  }

  @Override
  Collection mergeEntityFields(Collection entityToReplace, Collection replacement) {
    setNullFields(replacement, entityToReplace);
    replacement.setEmail(mergeLists(entityToReplace.getEmail(), replacement.getEmail()));
    replacement.setPhone(mergeLists(entityToReplace.getPhone(), replacement.getPhone()));
    replacement.setContentTypes(
        mergeLists(entityToReplace.getContentTypes(), replacement.getContentTypes()));
    replacement.setPreservationTypes(
        mergeLists(entityToReplace.getPreservationTypes(), replacement.getPreservationTypes()));
    replacement.setIncorporatedCollections(
        mergeLists(
            entityToReplace.getIncorporatedCollections(),
            replacement.getIncorporatedCollections()));
    replacement.setImportantCollectors(
        mergeLists(entityToReplace.getImportantCollectors(), replacement.getImportantCollectors()));

    // codes of the replaced entity are added as alternative codes of the replacement
    replacement
        .getAlternativeCodes()
        .add(
            new AlternativeCode(
                entityToReplace.getCode(),
                "Code from replaced entity " + entityToReplace.getKey()));
    replacement.getAlternativeCodes().addAll(entityToReplace.getAlternativeCodes());

    return replacement;
  }

  @Override
  void additionalOperations(Collection entityToReplace, Collection replacement) {
    // fix primary collection of contacts
    List<Person> persons = personMapper.list(null, entityToReplace.getKey(), null, null);
    persons.forEach(
        p -> {
          p.setPrimaryCollectionKey(replacement.getKey());
          personMapper.update(p);
        });
  }
}
