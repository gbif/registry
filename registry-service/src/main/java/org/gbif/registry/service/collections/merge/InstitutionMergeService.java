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
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.Person;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;
import org.gbif.registry.persistence.mapper.collections.OccurrenceMappingMapper;
import org.gbif.registry.persistence.mapper.collections.PersonMapper;
import org.gbif.registry.persistence.mapper.collections.dto.CollectionDto;
import org.gbif.registry.persistence.mapper.collections.params.CollectionSearchParams;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;

import static org.gbif.common.shaded.com.google.common.base.Preconditions.checkArgument;

/** Service to merge duplicated {@link Institution}. */
@Service
public class InstitutionMergeService extends BaseMergeService<Institution> {

  private final InstitutionMapper institutionMapper;
  private final CollectionMapper collectionMapper;
  private final MachineTagMapper machineTagMapper;
  private final OccurrenceMappingMapper occurrenceMappingMapper;

  @Autowired
  public InstitutionMergeService(
      InstitutionMapper institutionMapper,
      CollectionMapper collectionMapper,
      IdentifierMapper identifierMapper,
      MachineTagMapper machineTagMapper,
      OccurrenceMappingMapper occurrenceMappingMapper,
      PersonMapper personMapper) {
    super(
        institutionMapper,
        institutionMapper,
        institutionMapper,
        identifierMapper,
        institutionMapper,
        personMapper,
        machineTagMapper,
        occurrenceMappingMapper);
    this.institutionMapper = institutionMapper;
    this.collectionMapper = collectionMapper;
    this.machineTagMapper = machineTagMapper;
    this.occurrenceMappingMapper = occurrenceMappingMapper;
  }

  public UUID convertToCollection(
      UUID institutionKey,
      @Nullable UUID institutionKeyForNewCollection,
      @Nullable String newInstitutionName,
      String user) {
    checkArgument(institutionKey != null, "Institution key is required");
    checkArgument(!Strings.isNullOrEmpty(user), "User is required");
    checkArgument(
        institutionKeyForNewCollection != null || !Strings.isNullOrEmpty(newInstitutionName),
        "Either the institution key for the new collection or a name to create a new institution are required");

    Institution institutionToConvert = institutionMapper.get(institutionKey);
    checkArgument(
        institutionToConvert.getDeleted() == null, "Cannot convert a deleted institution");

    Collection newCollection = new Collection();
    newCollection.setKey(UUID.randomUUID());
    newCollection.setCode(institutionToConvert.getCode());
    newCollection.setAlternativeCodes(institutionToConvert.getAlternativeCodes());
    newCollection.setName(institutionToConvert.getName());
    newCollection.setDescription(institutionToConvert.getDescription());
    newCollection.setGeography(institutionToConvert.getGeographicDescription());
    newCollection.setTaxonomicCoverage(institutionToConvert.getTaxonomicDescription());
    newCollection.setEmail(institutionToConvert.getEmail());
    newCollection.setPhone(institutionToConvert.getPhone());
    newCollection.setHomepage(institutionToConvert.getHomepage());
    newCollection.setCatalogUrl(institutionToConvert.getCatalogUrl());
    newCollection.setApiUrl(institutionToConvert.getApiUrl());
    newCollection.setAddress(institutionToConvert.getAddress());
    newCollection.setMailingAddress(institutionToConvert.getMailingAddress());
    newCollection.setCreatedBy(user);
    newCollection.setModifiedBy(user);

    // if there is no institution passed we need to create a new institution
    if (institutionKeyForNewCollection == null) {
      Institution newInstitution = new Institution();
      newInstitution.setKey(UUID.randomUUID());
      newInstitution.setCode(institutionToConvert.getCode());
      newInstitution.setName(newInstitutionName);
      newInstitution.setCreatedBy(user);
      newInstitution.setModifiedBy(user);
      institutionMapper.create(newInstitution);

      newCollection.setInstitutionKey(newInstitution.getKey());
    } else {
      Institution institutionForNewCollection =
          institutionMapper.get(institutionKeyForNewCollection);
      checkArgument(
          institutionForNewCollection.getDeleted() == null,
          "Cannot assign the new collection to a deleted institution");

      newCollection.setInstitutionKey(institutionKeyForNewCollection);
    }

    collectionMapper.create(newCollection);
    institutionMapper.convertToCollection(institutionKey, newCollection.getKey());

    // move the collections
    moveCollectionsToAnotherInstitution(
        institutionToConvert.getKey(), newCollection.getInstitutionKey());

    // move the identifiers
    institutionToConvert
        .getIdentifiers()
        .forEach(
            i -> {
              identifierMapper.createIdentifier(i);
              collectionMapper.addIdentifier(newCollection.getKey(), i.getKey());
            });

    // move the machine tags
    institutionToConvert
        .getMachineTags()
        .forEach(
            mt -> {
              machineTagMapper.createMachineTag(mt);
              collectionMapper.addMachineTag(newCollection.getKey(), mt.getKey());
            });

    // move the occurrence mappings
    institutionToConvert
        .getOccurrenceMappings()
        .forEach(
            om -> {
              occurrenceMappingMapper.createOccurrenceMapping(om);
              collectionMapper.addOccurrenceMapping(newCollection.getKey(), om.getKey());
            });

    // copy the contacts
    institutionToConvert
        .getContacts()
        .forEach(c -> collectionMapper.addContact(newCollection.getKey(), c.getKey()));

    return newCollection.getKey();
  }

  @Override
  void checkMergeExtraPreconditions(Institution entityToReplace, Institution replacement) {
    // there is no extra preconditions for the merge
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

    moveCollectionsToAnotherInstitution(entityToReplace.getKey(), replacement.getKey());
  }

  private void moveCollectionsToAnotherInstitution(
      UUID sourceInstitutionKey, UUID targetInstitutionKey) {
    // move the collections to the entity to keep
    List<CollectionDto> collections =
        collectionMapper.list(
            CollectionSearchParams.builder().institutionKey(sourceInstitutionKey).build(), null);
    collections.forEach(
        c -> {
          c.getCollection().setInstitutionKey(targetInstitutionKey);
          collectionMapper.update(c.getCollection());
        });
  }
}
