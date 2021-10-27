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
import org.gbif.api.model.collections.suggestions.CollectionChangeSuggestion;
import org.gbif.api.model.collections.suggestions.Type;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.mail.EmailSender;
import org.gbif.registry.mail.collections.CollectionsEmailManager;
import org.gbif.registry.mail.config.CollectionsMailConfigurationProperties;
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

  @Autowired
  public CollectionChangeSuggestionService(
      ChangeSuggestionMapper changeSuggestionMapper,
      CollectionService collectionService,
      CollectionMergeService collectionMergeService,
      @Qualifier("registryObjectMapper") ObjectMapper objectMapper,
      EmailSender emailSender,
      CollectionsEmailManager emailManager,
      EventManager eventManager,
      GrSciCollAuthorizationService grSciCollAuthorizationService,
      CollectionsMailConfigurationProperties collectionsMailConfigurationProperties) {
    super(
        changeSuggestionMapper,
        collectionMergeService,
        collectionService,
        collectionService,
        Collection.class,
        objectMapper,
        emailSender,
        emailManager,
        eventManager,
        grSciCollAuthorizationService,
        collectionsMailConfigurationProperties);
    this.changeSuggestionMapper = changeSuggestionMapper;
  }

  @Override
  public int createChangeSuggestion(CollectionChangeSuggestion changeSuggestion) {
    checkArgument(
        changeSuggestion.getType() != Type.CONVERSION_TO_COLLECTION,
        "Conversion type is not allowed for collections");
    return super.createChangeSuggestion(changeSuggestion);
  }

  @Override
  public CollectionChangeSuggestion getChangeSuggestion(int key) {
    ChangeSuggestionDto dto = changeSuggestionMapper.get(key);

    if (dto == null || dto.getEntityType() != CollectionEntityType.COLLECTION) {
      return null;
    }

    return dtoToChangeSuggestion(dto);
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
}
