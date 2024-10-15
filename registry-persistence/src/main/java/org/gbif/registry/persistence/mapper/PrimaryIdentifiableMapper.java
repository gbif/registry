package org.gbif.registry.persistence.mapper;

import java.util.UUID;

import org.apache.ibatis.annotations.Param;

public interface PrimaryIdentifiableMapper extends IdentifiableMapper {

  /**
   * Adds a collection identifier to the specified entity.
   *
   * @param entityKey    the UUID of the target entity to which the identifier will be added
   * @param identifierKey the key of the identifier to be added
   * @param isPrimary     true if the identifier should be marked as primary; false otherwise
   * @return the identifier key of the added identifier
   */
  int addCollectionIdentifier(
    @Param("targetEntityKey") UUID entityKey,
    @Param("identifierKey") int identifierKey,
    @Param("isPrimary") boolean isPrimary);

  /**
   * Updates the identifiers of the given entity, making any existing primary identifier non-primary.
   *
   * @param entityKey the UUID of the target entity whose primary identifier will be updated
   */
  void updatePrimaryIdentifier(
    @Param("targetEntityKey") UUID entityKey);

  /**
   * Updates the current primary identifier of the specified entity, making it non-primary.
   *
   * @param entityKey     the UUID of the target entity
   * @param primary       true if the identifier is the new primary identifier; false otherwise
   * @param identifierKey the key of the identifier to update
   */
  void updateIdentifier(
    @Param("targetEntityKey") UUID entityKey,
    @Param("primary") boolean primary,
    @Param("identifierKey") int identifierKey);

  /**
   * Checks if the specified identifier is associated with the given entity.
   *
   * @param targetEntityKey the UUID of the target entity
   * @param identifierKey   the key of the identifier to check
   * @return true if the identifier is associated with the entity; false otherwise
   */
  Boolean areRelated(
    @Param("targetEntityKey") UUID targetEntityKey,
    @Param("identifierKey") int identifierKey);
}
