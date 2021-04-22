package org.gbif.registry.service.collections.suggestions;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.EntityType;
import org.gbif.api.model.collections.suggestions.ChangeSuggestion;
import org.gbif.api.model.collections.suggestions.CollectionChangeSuggestion;
import org.gbif.api.model.collections.suggestions.Status;
import org.gbif.api.model.collections.suggestions.Type;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.persistence.mapper.collections.ChangeSuggestionMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.persistence.mapper.collections.dto.ChangeSuggestionDto;
import org.gbif.registry.service.collections.DefaultCollectionService;
import org.gbif.registry.service.collections.merge.CollectionMergeService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class CollectionChangeSuggestionService extends BaseChangeSuggestionService<Collection> {

  private final ChangeSuggestionMapper changeSuggestionMapper;

  @Autowired
  public CollectionChangeSuggestionService(
      ChangeSuggestionMapper changeSuggestionMapper,
      CollectionMapper collectionMapper,
      DefaultCollectionService collectionService, // TODO: interfaces
      CollectionMergeService collectionMergeService,
      ObjectMapper objectMapper) {
    super(
        changeSuggestionMapper,
        collectionMapper,
        collectionMergeService,
        collectionService,
        Collection.class,
        objectMapper);
    this.changeSuggestionMapper = changeSuggestionMapper;
  }

  @Override
  public CollectionChangeSuggestion getChangeSuggestion(int key) {
    return (CollectionChangeSuggestion) dtoToChangeSuggestion(changeSuggestionMapper.get(key));
  }

  @Override
  protected CollectionChangeSuggestion newEmptyChangeSuggestion() {
    return new CollectionChangeSuggestion();
  }

  @Override
  protected int createConvertToCollectionSuggestion(ChangeSuggestion<Collection> changeSuggestion) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void applyConversionToCollection(ChangeSuggestionDto dto) {
    throw new UnsupportedOperationException();
  }
}
