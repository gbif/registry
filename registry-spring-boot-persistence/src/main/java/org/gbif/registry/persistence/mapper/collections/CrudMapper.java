package org.gbif.registry.persistence.mapper.collections;

import java.util.UUID;

/** Generic mapper for CRUD operations. Initially implemented for collections. */
public interface CrudMapper<T> {

  T get(UUID key);

  void create(T entity);

  void delete(UUID key);

  void update(T entity);
}
