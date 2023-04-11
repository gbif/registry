package org.gbif.registry.ws.it.collections.service.batch;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.collections.request.CollectionSearchRequest;
import org.gbif.api.model.collections.view.CollectionView;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.service.collections.batch.CollectionBatchService;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CollectionBatchServiceIT extends BaseBatchServiceIT<Collection> {

  private final CollectionService collectionService;

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule = new TestCaseDatabaseInitializer();

  @Autowired
  public CollectionBatchServiceIT(
      SimplePrincipalProvider simplePrincipalProvider,
      CollectionService collectionService,
      CollectionBatchService batchService) {
    super(
        simplePrincipalProvider, collectionService, batchService, CollectionEntityType.COLLECTION);
    this.collectionService = collectionService;
  }

  @Override
  List<Collection> listAllEntities() {
    return collectionService.list(CollectionSearchRequest.builder().build()).getResults().stream()
        .map(CollectionView::getCollection)
        .collect(Collectors.toList());
  }

  @Override
  Collection newInstance() {
    return new Collection();
  }

  @Override
  void assertUpdatedEntity(Collection updated) {
    assertEquals(2, updated.getContentTypes().size());
  }
}
