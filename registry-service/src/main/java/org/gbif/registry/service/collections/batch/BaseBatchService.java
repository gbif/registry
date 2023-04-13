package org.gbif.registry.service.collections.batch;

import org.gbif.api.model.collections.Batch;
import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.service.collections.BatchService;
import org.gbif.registry.persistence.mapper.collections.BatchMapper;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.common.base.Preconditions;

import static org.gbif.registry.security.UserRoles.GRSCICOLL_ADMIN_ROLE;
import static org.gbif.registry.security.UserRoles.GRSCICOLL_EDITOR_ROLE;
import static org.gbif.registry.security.UserRoles.GRSCICOLL_MEDIATOR_ROLE;

public abstract class BaseBatchService implements BatchService {

  private final BatchMapper batchMapper;
  private final BatchHandler batchHandler;
  private final CollectionEntityType entityType;

  @Autowired
  BaseBatchService(
      BatchMapper batchMapper, BatchHandler batchHandler, CollectionEntityType entityType) {
    this.batchMapper = batchMapper;
    this.batchHandler = batchHandler;
    this.entityType = entityType;
  }

  @Override
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_MEDIATOR_ROLE, GRSCICOLL_EDITOR_ROLE})
  public int handleBatch(byte[] entitiesFile, byte[] contactsFile, ExportFormat format) {
    Objects.requireNonNull(entitiesFile);
    Objects.requireNonNull(contactsFile);
    Objects.requireNonNull(format);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Preconditions.checkArgument(
        authentication != null && authentication.getName() != null, "Authentication is required");

    // create entry in DB
    Batch batch = new Batch();
    batch.setCreatedBy(authentication.getName());
    batch.setState(Batch.State.IN_PROGRESS);
    batch.setEntityType(entityType);
    batchMapper.create(batch);

    // async handle
    batchHandler.handleBatch(entitiesFile, contactsFile, format, batch);

    return batch.getKey();
  }

  @Override
  public Batch get(int key) {
    return batchMapper.get(key);
  }
}
