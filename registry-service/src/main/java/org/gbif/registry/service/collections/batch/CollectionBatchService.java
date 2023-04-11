package org.gbif.registry.service.collections.batch;

import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.registry.persistence.mapper.collections.BatchMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CollectionBatchService extends BaseBatchService {

  @Autowired
  CollectionBatchService(BatchMapper batchMapper, CollectionBatchHandler batchHandler) {
    super(batchMapper, batchHandler, CollectionEntityType.COLLECTION);
  }
}
