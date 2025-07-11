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
package org.gbif.registry.service.collections.merge;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.AlternativeCode;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.OccurrenceMapping;
import org.gbif.api.model.collections.request.CollectionSearchRequest;
import org.gbif.api.model.collections.view.CollectionView;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.InstitutionService;

import java.util.Collections;
import java.util.UUID;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkArgument;
import static org.gbif.registry.security.UserRoles.GRSCICOLL_ADMIN_ROLE;
import static org.gbif.registry.security.UserRoles.GRSCICOLL_MEDIATOR_ROLE;

/** Service to merge duplicated {@link Institution}. */
@Service
public class InstitutionMergeService extends BaseMergeService<Institution> {

  private final InstitutionService institutionService;
  private final CollectionService collectionService;

  @Autowired
  public InstitutionMergeService(
      InstitutionService institutionService, CollectionService collectionService) {
    super(institutionService);
    this.institutionService = institutionService;
    this.collectionService = collectionService;
  }

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  public UUID convertToCollection(
      UUID institutionKey,
      @Nullable UUID institutionKeyForNewCollection,
      @Nullable String newInstitutionName) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    checkArgument(!Strings.isNullOrEmpty(authentication.getName()));
    checkArgument(institutionKey != null, "Institution key is required");
    checkArgument(
        institutionKeyForNewCollection != null || !Strings.isNullOrEmpty(newInstitutionName),
        "Either the institution key for the new collection or a name to create a new institution are required");

    Institution institutionToConvert = institutionService.get(institutionKey);
    checkArgument(
        institutionToConvert.getDeleted() == null, "Cannot convert a deleted institution");
    checkArgument(
        institutionToConvert.getConvertedToCollection() == null,
        "Cannot convert an already converted institution");

    Collection newCollection = new Collection();
    newCollection.setCode(institutionToConvert.getCode());
    newCollection.setAlternativeCodes(institutionToConvert.getAlternativeCodes());
    newCollection.setName(institutionToConvert.getName());
    newCollection.setDescription(institutionToConvert.getDescription());
    newCollection.setEmail(institutionToConvert.getEmail());
    newCollection.setPhone(institutionToConvert.getPhone());
    newCollection.setHomepage(institutionToConvert.getHomepage());
    newCollection.setCatalogUrls(institutionToConvert.getCatalogUrls());
    newCollection.setApiUrls(institutionToConvert.getApiUrls());
    newCollection.setActive(institutionToConvert.isActive());

    if (institutionToConvert.getAddress() != null) {
      Address address = institutionToConvert.getAddress();
      address.setKey(null);
      newCollection.setAddress(address);
    }
    if (institutionToConvert.getMailingAddress() != null) {
      Address address = institutionToConvert.getMailingAddress();
      address.setKey(null);
      newCollection.setMailingAddress(address);
    }

    // if there is no institution passed we need to create a new institution
    if (institutionKeyForNewCollection == null) {
      Institution newInstitution = new Institution();
      newInstitution.setCode(institutionToConvert.getCode());
      newInstitution.setName(newInstitutionName);
      institutionService.create(newInstitution);

      newCollection.setInstitutionKey(newInstitution.getKey());
    } else {
      Institution institutionForNewCollection =
          institutionService.get(institutionKeyForNewCollection);
      checkArgument(
          institutionForNewCollection.getDeleted() == null,
          "Cannot assign the new collection to a deleted institution");

      newCollection.setInstitutionKey(institutionKeyForNewCollection);
    }

    collectionService.create(newCollection);
    institutionService.convertToCollection(institutionKey, newCollection.getKey());

    // move the collections
    moveCollectionsToAnotherInstitution(
        institutionToConvert.getKey(), newCollection.getInstitutionKey());

    // move the identifiers
    institutionToConvert
        .getIdentifiers()
        .forEach(
            i ->
                collectionService.addIdentifier(
                    newCollection.getKey(), new Identifier(i.getType(), i.getIdentifier())));

    // move the machine tags
    institutionToConvert
        .getMachineTags()
        .forEach(
            mt ->
                collectionService.addMachineTag(
                    newCollection.getKey(),
                    new MachineTag(mt.getNamespace(), mt.getName(), mt.getValue())));

    // move the occurrence mappings
    institutionToConvert
        .getOccurrenceMappings()
        .forEach(
            om -> {
              om.setKey(null);
              collectionService.addOccurrenceMapping(
                  newCollection.getKey(),
                  new OccurrenceMapping(om.getCode(), om.getIdentifier(), om.getDatasetKey()));
            });

    // copy the contact persons
    institutionToConvert
        .getContactPersons()
        .forEach(
            c -> {
              c.setKey(null);
              collectionService.addContactPerson(newCollection.getKey(), c);
            });

    return newCollection.getKey();
  }

  @Override
  void checkMergeExtraPreconditions(Institution entityToReplace, Institution replacement) {
    checkArgument(
        entityToReplace.getReplacedBy() == null, "Cannot merge an entity that was replaced");
    checkArgument(
        replacement.getReplacedBy() == null, "Cannot do a merge with an entity that was replaced");
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
    setNullFieldsInTarget(replacement, entityToReplace);
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
    moveCollectionsToAnotherInstitution(entityToReplace.getKey(), replacement.getKey());
  }

  private void moveCollectionsToAnotherInstitution(
      UUID sourceInstitutionKey, UUID targetInstitutionKey) {
    // move the collections to the entity to keep
    PagingResponse<CollectionView> collections =
        collectionService.list(
            CollectionSearchRequest.builder()
                .institution(Collections.singletonList(sourceInstitutionKey))
                .build());
    collections
        .getResults()
        .forEach(
            c -> {
              c.getCollection().setInstitutionKey(targetInstitutionKey);
              collectionService.update(c.getCollection());
            });
  }
}
