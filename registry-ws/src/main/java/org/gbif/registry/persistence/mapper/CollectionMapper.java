package org.gbif.registry.persistence.mapper;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.common.paging.Pageable;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;

public interface CollectionMapper extends TaggableMapper, IdentifiableMapper {

  Collection get(@Param("key") UUID key);

  void create(Collection collection);

  void delete(@Param("key") UUID key);

  void update(Collection collection);

  List<Collection> list(@Nullable @Param("page") Pageable page);

  List<Collection> search(@Nullable @Param("query") String query, @Nullable @Param("page") Pageable page);

  // TODO: collections by institution or by staff

}
