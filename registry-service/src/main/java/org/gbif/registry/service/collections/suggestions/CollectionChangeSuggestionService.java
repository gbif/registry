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
package org.gbif.registry.service.collections.suggestions;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.suggestions.CollectionChangeSuggestion;
import org.gbif.api.model.collections.suggestions.Type;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.mail.EmailSender;
import org.gbif.registry.mail.collections.CollectionsEmailManager;
import org.gbif.registry.mail.config.CollectionsMailConfigurationProperties;
import org.gbif.registry.persistence.mapper.UserMapper;
import org.gbif.registry.persistence.mapper.collections.ChangeSuggestionMapper;
import org.gbif.registry.persistence.mapper.collections.dto.ChangeSuggestionDto;
import org.gbif.registry.security.grscicoll.GrSciCollAuthorizationService;
import org.gbif.registry.service.collections.merge.CollectionMergeService;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.databind.ObjectMapper;

import static com.google.common.base.Preconditions.checkArgument;

@Service
@Validated
public class CollectionChangeSuggestionService
    extends BaseChangeSuggestionService<Collection, CollectionChangeSuggestion> {

  private final ChangeSuggestionMapper changeSuggestionMapper;
  private final InstitutionService institutionService;

  @Autowired
  public CollectionChangeSuggestionService(
      ChangeSuggestionMapper changeSuggestionMapper,
      CollectionService collectionService,
      CollectionMergeService collectionMergeService,
      UserMapper userMapper,
      @Qualifier("registryObjectMapper") ObjectMapper objectMapper,
      EmailSender emailSender,
      CollectionsEmailManager emailManager,
      EventManager eventManager,
      GrSciCollAuthorizationService grSciCollAuthorizationService,
      CollectionsMailConfigurationProperties collectionsMailConfigurationProperties,
      InstitutionService institutionService) {
    super(
        changeSuggestionMapper,
        collectionMergeService,
        collectionService,
        collectionService,
        userMapper,
        Collection.class,
        objectMapper,
        emailSender,
        emailManager,
        eventManager,
        grSciCollAuthorizationService,
        collectionsMailConfigurationProperties);
    this.changeSuggestionMapper = changeSuggestionMapper;
    this.institutionService = institutionService;
  }

  @Override
  public int createChangeSuggestion(CollectionChangeSuggestion changeSuggestion) {
    checkArgument(
        changeSuggestion.getType() != Type.CONVERSION_TO_COLLECTION,
        "Conversion type is not allowed for collections");
    return super.createChangeSuggestion(changeSuggestion);
  }

  @Override
  public  UUID applyChangeSuggestion(int suggestionKey){
    ChangeSuggestionDto dto = changeSuggestionMapper.get(suggestionKey);
    if (dto.getType() == Type.CREATE) {
      if (dto.getCreateInstitution()) {
        UUID createdInstitution = createInstitutionForCollectionSuggestion(dto);
        Collection suggestedCollection = readJson(dto.getSuggestedEntity(), Collection.class);
        suggestedCollection.setInstitutionKey(createdInstitution);
        dto.setSuggestedEntity(toJson(suggestedCollection));
        changeSuggestionMapper.update(dto);
      }
    }
    return super.applyChangeSuggestion(suggestionKey);
  }

  @Override
  public CollectionChangeSuggestion getChangeSuggestion(int key) {
    ChangeSuggestionDto dto = changeSuggestionMapper.get(key);

    if (dto == null || dto.getEntityType() != CollectionEntityType.COLLECTION) {
      return null;
    }
    CollectionChangeSuggestion changeSuggestion = dtoToChangeSuggestion(dto);
    changeSuggestion.setCreateInstitution(dto.getCreateInstitution());
    changeSuggestion.setIhIdentifier(dto.getIhIdentifier());
    return changeSuggestion;
  }

  @Override
  protected CollectionChangeSuggestion newEmptyChangeSuggestion() {
    return new CollectionChangeSuggestion();
  }

  @Override
  protected ChangeSuggestionDto createConvertToCollectionSuggestionDto(
      CollectionChangeSuggestion changeSuggestion) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected UUID applyConversionToCollection(ChangeSuggestionDto dto) {
    throw new UnsupportedOperationException();
  }

  public UUID createInstitutionForCollectionSuggestion(ChangeSuggestionDto dto){
    CollectionChangeSuggestion changeSuggestion = dtoToChangeSuggestion(dto);
    UUID createdEntity = null;
    if (dto.getType() == Type.CREATE) {

      if (dto.getCreateInstitution()) {
        Institution institution = collectionChangeSuggestionToInstitution(dto);
        createdEntity = institutionService.create(institution);
        createContacts(changeSuggestion,createdEntity);
      }
    }
    return createdEntity;
  }

  private Institution collectionChangeSuggestionToInstitution(ChangeSuggestionDto dto) {
    Institution institution = new Institution();

    if (dto.getSuggestedEntity() != null) {
      Collection collection = readJson(dto.getSuggestedEntity(), Collection.class);
      institution.setName(collection.getName());
      institution.setCode(collection.getCode());
      institution.setActive(collection.isActive());

      institution.setAddress(collection.getAddress());
      institution.setEmail(collection.getEmail());
      institution.setPhone(collection.getPhone());
      institution.setComments(collection.getComments());
      institution.setMasterSourceMetadata(collection.getMasterSourceMetadata());
      institution.setMasterSource(collection.getMasterSource());
      institution.setContactPersons(collection.getContactPersons());

      institution.setDescription(collection.getDescription());

    }

    return institution;
  }

  private void createContacts(CollectionChangeSuggestion changeSuggestion, UUID createdEntity) {
    if (changeSuggestion.getSuggestedEntity().getContactPersons() != null
      && !changeSuggestion.getSuggestedEntity().getContactPersons().isEmpty()) {
      changeSuggestion
        .getSuggestedEntity()
        .getContactPersons()
        .forEach(c -> institutionService.addContactPerson(createdEntity, c));
    }
  }
}
