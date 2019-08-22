package org.gbif.registry.persistence;

import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.registry.persistence.mapper.NetworkEntityMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

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

  @Transactional
  public <T extends NetworkEntity> void update(NetworkEntityMapper<T> mapper, T entity) {
    checkNotNull(entity, "Unable to update an entity when it is not provided");
    T existing = mapper.get(entity.getKey());
    checkNotNull(existing, "Unable to update a non existing entity");

    if (existing.getDeleted() != null) {
      // allow updates ONLY if they are undeleting too
      checkArgument(entity.getDeleted() == null,
          "Unable to update a previously deleted entity unless you clear the deletion timestamp");
    } else {
      // do not allow deletion here (for safety) - we have an explicity deletion service
      checkArgument(entity.getDeleted() == null, "Cannot delete using the update service.  Use the deletion service");
    }

    mapper.update(entity);
  }
}
