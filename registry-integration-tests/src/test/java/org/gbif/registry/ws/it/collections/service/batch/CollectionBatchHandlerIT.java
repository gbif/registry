package org.gbif.registry.ws.it.collections.service.batch;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.registry.service.collections.batch.CollectionBatchHandler;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import org.springframework.beans.factory.annotation.Autowired;

public class CollectionBatchHandlerIT extends BaseBatchHandlerIT<Collection> {

  @Autowired
  public CollectionBatchHandlerIT(
      SimplePrincipalProvider simplePrincipalProvider,
      CollectionService collectionService,
      CollectionBatchHandler collectionBatchHandler) {
    super(
        simplePrincipalProvider,
        collectionBatchHandler,
        collectionService,
        CollectionEntityType.COLLECTION);
  }

  @Override
  Collection newEntity() {
    return new Collection();
  }
}
