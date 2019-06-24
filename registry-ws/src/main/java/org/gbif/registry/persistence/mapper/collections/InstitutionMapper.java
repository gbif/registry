package org.gbif.registry.persistence.mapper.collections;

import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.registry.persistence.mapper.IdentifiableMapper;
import org.gbif.registry.persistence.mapper.TaggableMapper;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;

/**
 * Mapper for {@link Institution} entities.
 */
public interface InstitutionMapper extends CrudMapper<Institution>, ContactableMapper, TaggableMapper, IdentifiableMapper {

  List<Institution> list(@Nullable @Param("query") String query,
                         @Nullable @Param("contactKey") UUID contactKey,
                         @Nullable @Param("page") Pageable page);

  long count(@Nullable @Param("query") String query, @Nullable @Param("contactKey") UUID contactKey);

  /**
   * A simple suggest by title service.
   */
  List<KeyCodeNameResult> suggest(@Nullable @Param("q") String q);

  /**
   * @return the institutions marked as deleted
   */
  List<Institution> deleted(@Param("page") Pageable page);

  /**
   * @return the count of the institutions marked as deleted.
   */
  long countDeleted();

  /**
   * Finds an institution by any of its identifiers.
   *
   * @return the keys of the institutions
   */
  List<UUID> findByIdentifier(@Nullable @Param("identifier") String identifier);
}
