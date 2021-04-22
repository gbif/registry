package org.gbif.registry.service.collections;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.registry.Commentable;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.Taggable;
import org.gbif.registry.persistence.mapper.collections.BaseMapper;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static com.google.common.base.Preconditions.checkArgument;

// TODO: move events here too?
// TODO: this should be implement the CrudService instead of the web resource
public abstract class BaseCollectionsService<
    T extends CollectionEntity & Taggable & Identifiable & MachineTaggable & Commentable> {

  private final BaseMapper<T> baseMapper;

  protected BaseCollectionsService(BaseMapper<T> baseMapper) {
    this.baseMapper = baseMapper;
  }

  public T get(UUID key) {
    return baseMapper.get(key);
  }

  public void delete(UUID key) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    T entityToDelete = get(key);
    checkArgument(entityToDelete != null, "Entity to delete doesn't exist");

    entityToDelete.setModifiedBy(authentication.getName());
    update(entityToDelete);

    baseMapper.delete(key);
  }

  protected void preCreate(T entity) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final String username = authentication.getName();
    entity.setCreatedBy(username);
    entity.setModifiedBy(username);
  }

  protected void preUpdate(T entity) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    entity.setModifiedBy(authentication.getName());
  }

  /**
   * Some iDigBio collections and institutions don't have code and we allow that in the DB but not
   * in the API.
   */
  protected void checkCodeUpdate(T newEntity, T oldEntity) {
    if (newEntity instanceof Institution) {
      Institution newInstitution = (Institution) newEntity;
      Institution oldInstitution = (Institution) oldEntity;

      if (newInstitution.getCode() == null && oldInstitution.getCode() != null) {
        throw new IllegalArgumentException("Not allowed to delete the code of an institution");
      }
    } else if (newEntity instanceof Collection) {
      Collection newCollection = (Collection) newEntity;
      Collection oldCollection = (Collection) oldEntity;

      if (newCollection.getCode() == null && oldCollection.getCode() != null) {
        throw new IllegalArgumentException("Not allowed to delete the code of a collection");
      }
    }
  }

  /**
   * Replaced and converted entities cannot be updated or restored. Also, they can't be replaced or
   * converted in an update
   */
  protected void checkReplacedEntitiesUpdate(T newEntity, T oldEntity) {
    if (newEntity instanceof Institution) {
      Institution newInstitution = (Institution) newEntity;
      Institution oldInstitution = (Institution) oldEntity;

      if (oldInstitution.getReplacedBy() != null
          || oldInstitution.getConvertedToCollection() != null) {
        throw new IllegalArgumentException(
            "Not allowed to update a replaced or converted institution");
      } else if (newInstitution.getReplacedBy() != null
          || newInstitution.getConvertedToCollection() != null) {
        throw new IllegalArgumentException(
            "Not allowed to replace or convert an institution while updating");
      }
    } else if (newEntity instanceof Collection) {
      Collection newCollection = (Collection) newEntity;
      Collection oldCollection = (Collection) oldEntity;

      if (oldCollection.getReplacedBy() != null) {
        throw new IllegalArgumentException("Not allowed to update a replaced collection");
      } else if (newCollection.getReplacedBy() != null) {
        throw new IllegalArgumentException("Not allowed to replace a collection while updating");
      }
    }
  }

  protected abstract void update(T entity);
}
