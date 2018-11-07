package org.gbif.registry.persistence.mapper.collections;

import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.registry.persistence.mapper.IdentifiableMapper;
import org.gbif.registry.persistence.mapper.TaggableMapper;

import java.util.List;
import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;

/**
 * Mapper for {@link Institution} entities.
 */
public interface InstitutionMapper extends CrudMapper<Institution>, ContactableMapper, TaggableMapper, IdentifiableMapper {

  List<Institution> list(@Nullable @Param("query") String query, @Nullable @Param("page") Pageable page);

  long count(@Nullable @Param("query") String query);

}
