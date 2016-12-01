package org.gbif.registry.search.util;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.registry.search.DatasetIndexService;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A builder that will clear and build a new dataset index by paging over the given service.
 */
@Singleton
public class DatasetIndexBuilder {

  // controls how many results we request while paging over the WS
  private static final int PAGE_SIZE = 100;
  private static final Logger LOG = LoggerFactory.getLogger(DatasetIndexBuilder.class);
  private final DatasetIndexService indexService;
  private final DatasetService datasetService;

  @Inject
  public DatasetIndexBuilder(DatasetService datasetService,
                             DatasetIndexService indexService) {
    this.datasetService = datasetService;
    this.indexService = indexService;
  }

  /**
   * Pages over all datasets and adds them to SOLR.
   */
  public void build() throws SolrServerException, IOException {
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
    LOG.info("Finished building Dataset index in {} secs", stopwatch.elapsed(TimeUnit.SECONDS));
  }

}
