package org.gbif.registry.persistence.mapper.collections;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.registry.persistence.mapper.IdentifiableMapper;
import org.gbif.registry.persistence.mapper.TaggableMapper;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;

/**
 * Mapper for {@link Collection} entities.
 */
public interface CollectionMapper
    extends CrudMapper<Collection>, ContactableMapper, TaggableMapper, IdentifiableMapper {

  List<Collection> list(@Nullable @Param("institutionKey") UUID institutionKey,
                    @Nullable @Param("query") String query,
                    @Nullable @Param("page") Pageable page);

  long count(@Nullable @Param("institutionKey") UUID institutionKey,
             @Nullable @Param("query") String query);

}
