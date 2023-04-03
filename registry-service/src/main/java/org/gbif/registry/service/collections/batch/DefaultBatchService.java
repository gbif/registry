package org.gbif.registry.service.collections.batch;

import org.gbif.api.model.collections.Batch;
import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.service.collections.BatchService;
import org.gbif.registry.persistence.mapper.collections.BatchMapper;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;

@Service
public class DefaultBatchService implements BatchService {

  private final BatchMapper batchMapper;
  private final InstitutionBatchHandler institutionBatchHandler;
  private final CollectionBatchHandler collectionBatchHandler;

  @Autowired
  DefaultBatchService(
      BatchMapper batchMapper,
      InstitutionBatchHandler institutionBatchHandler,
      CollectionBatchHandler collectionBatchHandler) {
    this.batchMapper = batchMapper;
    this.institutionBatchHandler = institutionBatchHandler;
    this.collectionBatchHandler = collectionBatchHandler;
  }

  @Override
  public int handleBatchAsync(
      byte[] entitiesFile,
      byte[] contactsFile,
      ExportFormat format,
      boolean update,
      CollectionEntityType entityType) {
    Objects.requireNonNull(entitiesFile);
    Objects.requireNonNull(contactsFile);
    Objects.requireNonNull(format);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Preconditions.checkArgument(
        authentication != null && authentication.getName() != null, "Authentication is required");

    // create entry in DB
    Batch batch = new Batch();
    batch.setCreatedBy(authentication.getName());
    batch.setState(Batch.State.SUCCESSFUL);
    batch.setEntityType(entityType);

    // async import
    if (update) {
      batch.setOperation(Batch.Operation.UPDATE);
      if (entityType == CollectionEntityType.INSTITUTION) {
        institutionBatchHandler.updateBatch(
          entitiesFile, contactsFile, format, batch, authentication.getName());
      } else if (entityType == CollectionEntityType.COLLECTION) {
        collectionBatchHandler.updateBatch(
          entitiesFile, contactsFile, format, batch, authentication.getName());
      }
    } else {
      batch.setOperation(Batch.Operation.CREATE);
      if (entityType == CollectionEntityType.INSTITUTION) {
        institutionBatchHandler.importBatch(
            entitiesFile, contactsFile, format, batch, authentication.getName());
      } else if (entityType == CollectionEntityType.COLLECTION) {
        collectionBatchHandler.importBatch(
            entitiesFile, contactsFile, format, batch, authentication.getName());
      }
    }

    batchMapper.create(batch);

    return batch.getKey();
  }

  @Override
  public Batch get(int key) {
    return batchMapper.get(key);
  }
}
