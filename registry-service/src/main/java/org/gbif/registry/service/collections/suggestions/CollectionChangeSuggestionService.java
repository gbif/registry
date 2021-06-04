package org.gbif.registry.service.collections.suggestions;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.collections.suggestions.CollectionChangeSuggestion;
import org.gbif.api.model.collections.suggestions.Type;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.mail.EmailSender;
import org.gbif.registry.mail.collections.CollectionsEmailManager;
import org.gbif.registry.persistence.mapper.collections.ChangeSuggestionMapper;
import org.gbif.registry.persistence.mapper.collections.dto.ChangeSuggestionDto;
import org.gbif.registry.security.grscicoll.GrSciCollAuthorizationService;
import org.gbif.registry.service.collections.merge.CollectionMergeService;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
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
      ObjectMapper objectMapper,
      EmailSender emailSender,
      CollectionsEmailManager emailManager,
      EventManager eventManager,
      GrSciCollAuthorizationService grSciCollAuthorizationService
  ) {
    super(
        changeSuggestionMapper,
        collectionMergeService,
        collectionService,
        Collection.class,
        objectMapper,
        emailSender,
        emailManager,
        eventManager, grSciCollAuthorizationService);
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
