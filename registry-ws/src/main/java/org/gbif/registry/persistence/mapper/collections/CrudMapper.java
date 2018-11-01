package org.gbif.registry.persistence.mapper.collections;

import org.gbif.api.model.common.paging.Pageable;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;

/** Generic mapper for CRUD operations. Initially implemented for collections. */
public interface CrudMapper<T> {

  T get(@Param("key") UUID key);

  void create(T entity);

  void delete(@Param("key") UUID key);

  void update(T entity);

  List<T> list(@Nullable @Param("page") Pageable page);

  List<T> search(@Nullable @Param("query") String query, @Nullable @Param("page") Pageable page);

  long count();

  long countWithFilter(@Nullable @Param("query") String query);
}
