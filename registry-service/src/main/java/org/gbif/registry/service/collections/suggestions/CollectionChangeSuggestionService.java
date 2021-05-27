package org.gbif.registry.service.collections.suggestions;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.collections.suggestions.CollectionChangeSuggestion;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.mail.EmailSender;
import org.gbif.registry.mail.collections.CollectionsEmailManager;
import org.gbif.registry.persistence.mapper.collections.ChangeSuggestionMapper;
import org.gbif.registry.persistence.mapper.collections.dto.ChangeSuggestionDto;
import org.gbif.registry.security.grscicoll.GrSciCollEditorAuthorizationService;
import org.gbif.registry.service.collections.merge.CollectionMergeService;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.databind.ObjectMapper;

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
      ObjectMapper objectMapper,
      EmailSender emailSender,
      CollectionsEmailManager emailManager,
      EventManager eventManager,
      GrSciCollEditorAuthorizationService grSciCollEditorAuthorizationService) {
    super(
        changeSuggestionMapper,
        collectionMergeService,
        collectionService,
        Collection.class,
        objectMapper,
        emailSender,
        emailManager,
        eventManager,
        grSciCollEditorAuthorizationService);
    this.changeSuggestionMapper = changeSuggestionMapper;
  }

  @Override
  public CollectionChangeSuggestion getChangeSuggestion(int key) {
    ChangeSuggestionDto dto = changeSuggestionMapper.get(key);

    if (dto.getEntityType() != CollectionEntityType.COLLECTION) {
      throw new IllegalArgumentException("Wrong key for collection change suggestion: " + key);
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
