package org.gbif.registry.persistence.mapper.collections;

import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.Taggable;
import org.gbif.registry.persistence.mapper.IdentifiableMapper;
import org.gbif.registry.persistence.mapper.MachineTaggableMapper;
import org.gbif.registry.persistence.mapper.TaggableMapper;

import java.util.UUID;

import org.apache.ibatis.annotations.Param;

/** Generic mapper for CRUD operations. Initially implemented for collections. */
public interface BaseMapper<T extends Taggable & Identifiable & MachineTaggable>
  extends TaggableMapper, IdentifiableMapper, MachineTaggableMapper<T> {

  T get(@Param("key") UUID key);

  void create(T entity);

  void delete(@Param("key") UUID key);

  void update(T entity);
}

