package org.gbif.registry.persistence.mapper.collections;

import java.util.UUID;

import org.apache.ibatis.annotations.Param;

/** Generic mapper for CRUD operations. Initially implemented for collections. */
public interface CrudMapper<T> {

  T get(@Param("key") UUID key);

  void create(T entity);

  void delete(@Param("key") UUID key);

  void update(T entity);
}
