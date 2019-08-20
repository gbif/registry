package org.gbif.registry.persistence.mapper;

import org.apache.ibatis.annotations.Param;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.vocabulary.IdentifierType;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Mappers that perform operations on network entities.
 */
public interface NetworkEntityMapper<T extends NetworkEntity> {

  /**
   * This gets the instance in question. Note that this does return deleted items.
   *
   * @param key of the network entity to fetch
   * @return either the requested network entity or {@code null} if it couldn't be found
   */
  T get(@Param("key") UUID key);

  String title(@Param("key") UUID key);

  void create(T entity);

  void delete(@Param("key") UUID key);

  void update(T entity);

  List<T> list(@Nullable @Param("page") Pageable page);

  List<T> search(@Nullable @Param("query") String query, @Nullable @Param("page") Pageable page);

  int count();

  int count(@Nullable @Param("query") String query);

  long countByIdentifier(@Nullable @Param("type") IdentifierType type, @Param("identifier") String identifier);

  List<T> listByIdentifier(@Nullable @Param("type") IdentifierType type, @Param("identifier") String identifier,
                           @Param("page") Pageable page);
}
