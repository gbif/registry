package org.gbif.registry.persistence.mapper.collections;

import org.apache.ibatis.annotations.Param;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.registry.persistence.ContactableMapper;
import org.gbif.registry.persistence.mapper.IdentifiableMapper;
import org.gbif.registry.persistence.mapper.TaggableMapper;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Mapper for {@link Collection} entities.
 */
@Repository
public interface CollectionMapper
    extends BaseMapper<Collection>, ContactableMapper, TaggableMapper, IdentifiableMapper {

  List<Collection> list(@Nullable @Param("institutionKey") UUID institutionKey,
                        @Nullable @Param("contactKey") UUID contactKey,
                        @Nullable @Param("query") String query,
                        @Nullable @Param("code") String code,
                        @Nullable @Param("name") String name,
                        @Nullable @Param("page") Pageable page);

  long count(@Nullable @Param("institutionKey") UUID institutionKey,
             @Nullable @Param("contactKey") UUID contactKey,
             @Nullable @Param("query") String query,
             @Nullable @Param("code") String code,
             @Nullable @Param("name") String name);

  /**
   * A simple suggest by title service.
   */
  List<KeyCodeNameResult> suggest(@Nullable @Param("q") String q);

  /**
   * @return the collections marked as deleted
   */
  List<Collection> deleted(@Param("page") Pageable page);

  /**
   * @return the count of the collections marked as deleted.
   */
  long countDeleted();

  /**
   * Finds a collection by any of its identifiers.
   *
   * @return the keys of the collections
   */
  List<UUID> findByIdentifier(@Nullable @Param("identifier") String identifier);
}
