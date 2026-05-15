package org.gbif.registry.ws.it;

import org.gbif.registry.search.test.ElasticsearchTestContainerConfiguration;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.gbif.api.model.registry.search.DatasetSuggestRequest;
import org.gbif.api.model.registry.search.DatasetSuggestResult;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import static org.junit.jupiter.api.Assertions.*;

/** Simple integration test verifying async suggest wiring. */
public class DatasetSearchAsyncIT extends BaseItTest {

  @Autowired
  @Qualifier("datasetSearchServiceEs")
  private org.gbif.registry.search.dataset.service.AsyncDatasetSearchService asyncSearchService;

  public DatasetSearchAsyncIT(SimplePrincipalProvider simplePrincipalProvider,
      ElasticsearchTestContainerConfiguration elasticsearchTestContainer) {
    super(simplePrincipalProvider, elasticsearchTestContainer);
  }

  @Test
  public void testSuggestAsyncReturns() throws Exception {
    DatasetSuggestRequest req = new DatasetSuggestRequest();
    req.setQ("test");
    java.util.concurrent.CompletableFuture<List<DatasetSuggestResult>> fut = asyncSearchService.suggestAsync(req);
    assertNotNull(fut);
    List<DatasetSuggestResult> results = fut.get();
    assertNotNull(results);
  }
}

