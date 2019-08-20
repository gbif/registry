package org.gbif.registry.persistence;

import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.registry.persistence.mapper.NetworkEntityMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;

// TODO: 2019-08-20 it used to be a class with static util methods
// TODO: 2019-08-20 should be in the persistence module?
@Service
public class WithMyBatis {

  @Transactional
  public <T extends NetworkEntity> UUID create(NetworkEntityMapper<T> mapper, T entity) {
    checkArgument(entity.getKey() == null, "Unable to create an entity which already has a key");
    // REVIEW: If this call fails the entity will have been modified anyway! We could make a copy and return that instead
    entity.setKey(UUID.randomUUID());
    mapper.create(entity);
    return entity.getKey();
  }
}
