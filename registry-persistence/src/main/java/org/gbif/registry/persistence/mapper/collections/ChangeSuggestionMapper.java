package org.gbif.registry.persistence.mapper.collections;

import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.collections.suggestions.Status;
import org.gbif.api.model.collections.suggestions.Type;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.registry.persistence.mapper.collections.dto.ChangeSuggestionDto;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChangeSuggestionMapper {

  void create(ChangeSuggestionDto suggestion);

  ChangeSuggestionDto get(@Param("key") int key);

  ChangeSuggestionDto getByKeyAndType(
      @Param("key") int key, @Param("entityType") CollectionEntityType entityType);

  List<ChangeSuggestionDto> list(
      @Param("status") Status status,
      @Param("type") Type type,
      @Param("entityType") CollectionEntityType entityType,
      @Param("proposerEmail") String proposerEmail,
      @Param("entityKey") UUID entityKey,
      @Nullable @Param("page") Pageable page);

  long count(
      @Param("status") Status status,
      @Param("type") Type type,
      @Param("entityType") CollectionEntityType entityType,
      @Param("proposerEmail") String proposerEmail,
      @Param("entityKey") UUID entityKey);

  void update(ChangeSuggestionDto suggestion);
}
