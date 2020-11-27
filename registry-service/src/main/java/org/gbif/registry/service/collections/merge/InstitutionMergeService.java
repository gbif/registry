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
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.Person;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;
import org.gbif.registry.persistence.mapper.collections.PersonMapper;
import org.gbif.registry.persistence.mapper.collections.dto.CollectionDto;
import org.gbif.registry.persistence.mapper.collections.params.CollectionSearchParams;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Service to merge duplicated {@link Institution}. */
@Service
public class InstitutionMergeService extends BaseMergeService<Institution> {

  private final CollectionMapper collectionMapper;

  @Autowired
  public InstitutionMergeService(
      InstitutionMapper institutionMapper,
      CollectionMapper collectionMapper,
      IdentifierMapper identifierMapper,
      PersonMapper personMapper) {
    super(
        institutionMapper,
        institutionMapper,
        institutionMapper,
        identifierMapper,
        institutionMapper,
        personMapper);
    this.collectionMapper = collectionMapper;
  }

  @Override
  void checkExtraPreconditions(Institution entityToReplace, Institution replacement) {
    // there is no extra preconditions
  }

  @Override
  Institution mergeEntityFields(Institution entityToReplace, Institution replacement) {
    // codes of the replaced entity are added as alternative codes of the replacement
    replacement
        .getAlternativeCodes()
        .add(
            new AlternativeCode(
                entityToReplace.getCode(),
                "Code from replaced entity " + entityToReplace.getKey()));
    replacement.getAlternativeCodes().addAll(entityToReplace.getAlternativeCodes());

    // Copy over information that would be lost when removing these duplicates
    setNullFields(replacement, entityToReplace);
    replacement.setEmail(mergeLists(entityToReplace.getEmail(), replacement.getEmail()));
    replacement.setPhone(mergeLists(entityToReplace.getPhone(), replacement.getPhone()));
    replacement.setDisciplines(
        mergeLists(entityToReplace.getDisciplines(), replacement.getDisciplines()));
    replacement.setAdditionalNames(
        mergeLists(entityToReplace.getAdditionalNames(), replacement.getAdditionalNames()));

    return replacement;
  }

  @Override
  void additionalOperations(Institution entityToReplace, Institution replacement) {
    // fix primary institution of contacts
    List<Person> persons = personMapper.list(entityToReplace.getKey(), null, null, null);
    persons.forEach(
        p -> {
          p.setPrimaryInstitutionKey(replacement.getKey());
          personMapper.update(p);
        });

    // move the collections to the entity to keep
    List<CollectionDto> collections =
        collectionMapper.list(
            CollectionSearchParams.builder().institutionKey(entityToReplace.getKey()).build(),
            null);
    collections.forEach(
        c -> {
          c.getCollection().setInstitutionKey(replacement.getKey());
          collectionMapper.update(c.getCollection());
        });
  }
}
