package org.gbif.registry.persistence.mapper;

import org.gbif.api.model.registry.NetworkEntity;

/**
 * The BaseNetworkEntityMapper defines a common interface for all our Network entities.
 */
public interface BaseNetworkEntityMapper<T extends NetworkEntity> extends NetworkEntityMapper<T>,
    ContactableMapper, CommentableMapper, MachineTaggableMapper, TaggableMapper, EndpointableMapper, IdentifiableMapper {

}
