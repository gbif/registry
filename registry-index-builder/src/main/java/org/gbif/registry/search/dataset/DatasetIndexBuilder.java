package org.gbif.registry.search.dataset;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.common.search.solr.SolrConfig;
import org.gbif.registry.directory.DirectoryModule;
import org.gbif.registry.events.EventModule;
import org.gbif.registry.persistence.guice.RegistryMyBatisModule;
import org.gbif.registry.search.DatasetIndexService;
import org.gbif.registry.search.WorkflowUtils;
import org.gbif.registry.search.guice.RegistrySearchModule;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.registry.search.guice.RegistrySearchModule.SOLR_DATASET_PREFIX;


/**
 * A builder that will clear and build a new dataset index by paging over the given service.
 */
public class DatasetIndexBuilder {

  // controls how many results we request while paging over the WS
  private static final int PAGE_SIZE = 100;
  private static final Logger LOG = LoggerFactory.getLogger(DatasetIndexBuilder.class);
  private final DatasetIndexService indexService;
  private final DatasetService datasetService;

  public DatasetIndexBuilder(DatasetService datasetService, DatasetIndexService indexService) {
    this.datasetService = datasetService;
    this.indexService = indexService;
  }

  /**
   * Pages over all datasets and adds them to SOLR.
   */
  public void build() throws Exception {
    LOG.info("Building a new Dataset index");
    Stopwatch stopwatch = Stopwatch.createStarted();
    PagingRequest page = new PagingRequest(0, PAGE_SIZE);
    PagingResponse<Dataset> response = null;
    do {
      LOG.debug("Requesting {} datasets starting at offset {}", page.getLimit(), page.getOffset());
      response = datasetService.list(page);
      // Batching updates to SOLR proves quicker with batches of 100 - 1000 showing similar performance
      LOG.debug("Indexing {} datasets starting at offset {}", page.getLimit(), page.getOffset());
      indexService.add(response.getResults());
      page.nextPage();

    } while (!response.isEndOfRecords());

    indexService.closeAndAwaitTermination();
    LOG.info("Finished building Dataset index in {} secs", stopwatch.elapsed(TimeUnit.SECONDS));
  }

  public static Injector registryInjector(Properties props) {
    return Guice.createInjector(
        new RegistryMyBatisModule(props),
        new RegistrySearchModule(props),
        new DirectoryModule(props),
        new StubModule(),
        EventModule.withoutRabbit(props)
    );
  }

  public static void run (Properties props) {
    try {
      // read properties and check args
      Injector inj = registryInjector(props);
      SolrConfig solr = SolrConfig.fromProperties(props, SOLR_DATASET_PREFIX);
      DatasetIndexBuilder idxBuilder = new DatasetIndexBuilder(inj.getInstance(DatasetService.class), inj.getInstance(DatasetIndexService.class));
      LOG.info("Building new solr index for collection {} on {}", solr.collection, solr.serverHome);
      idxBuilder.build();

      LOG.info("Indexing completed successfully.");

    } catch (Exception e) {
      LOG.error("Failed to run index builder", e);
      System.exit(1);
    }
  }

  public static void main (String[] args) throws IOException {
    run(WorkflowUtils.loadProperties(args));
    System.exit(0);
  }
}
