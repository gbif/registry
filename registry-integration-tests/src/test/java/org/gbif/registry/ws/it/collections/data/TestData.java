package org.gbif.registry.ws.it.collections.data;

import org.gbif.api.model.collections.CollectionEntity;

public interface TestData<T extends CollectionEntity> {

  T newEntity();

  T updateEntity(T entity);

  T newInvalidEntity();
}
