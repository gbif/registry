package org.gbif.registry.persistence.mapper.collections;

import org.gbif.api.model.collections.Person;
import org.gbif.api.model.common.paging.Pageable;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;

/**
 * Mapper for {@link Person} entities.
 */
public interface PersonMapper extends CrudMapper<Person> {

  List<Person> list(@Nullable @Param("institutionKey") UUID institutionKey,
                    @Nullable @Param("collectionKey") UUID collectionKey,
                    @Nullable @Param("query") String query,
                    @Nullable @Param("page") Pageable page);

  long count(@Nullable @Param("institutionKey") UUID institutionKey,
                    @Nullable @Param("collectionKey") UUID collectionKey,
                    @Nullable @Param("query") String query);

}
