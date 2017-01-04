package org.gbif.registry.search.dataset.occurrence;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.common.search.solr.SolrConfig;
import org.gbif.registry.search.WorkflowUtils;

import java.io.IOException;
import java.util.Properties;

import com.google.inject.Injector;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.registry.search.dataset.DatasetIndexBuilder.registryInjector;
import static org.gbif.registry.search.dataset.checklist.DatasetIndexChecklistUpdater.atomicUpdate;
import static org.gbif.registry.search.guice.RegistrySearchModule.SOLR_DATASET_PREFIX;


public class DatasetIndexOccurrenceUpdater {
  private static final String SOLR_OCCURRENCE_PREFIX = "solr.occurrence.";
  private static final Logger LOG = LoggerFactory.getLogger(DatasetIndexOccurrenceUpdater.class);
  private final int PAGE_SIZE = 100;

  private final OccSearchClient occ;
  private final SolrConfig datasetSolr;
  private final DatasetService datasetService;

  public DatasetIndexOccurrenceUpdater(OccSearchClient occ, SolrConfig datasetSolr, DatasetService datasetService) {
    this.occ = occ;
    this.datasetSolr = datasetSolr;
    this.datasetService = datasetService;
  }

  public void run() {
    LOG.info("Updating occurrence datasets with taxon keys");
    try (SolrClient solr = datasetSolr.buildSolr()
    ) {
      PagingRequest page = new PagingRequest(0, PAGE_SIZE);
      PagingResponse<Dataset> response;
      do {
        LOG.debug("Requesting {} datasets starting at offset {}", page.getLimit(), page.getOffset());
        // TODO we need to index all types with occurrences ultimately
        response = datasetService.listByType(DatasetType.OCCURRENCE, page);
        for (Dataset d : response.getResults()) {
          SolrInputDocument doc = new SolrInputDocument();
          doc.addField("key", d.getKey().toString());

          OccSearchClient.FacetResult taxKeys = occ.taxonKeys(d.getKey());
          doc.addField("taxon_key", atomicUpdate(taxKeys.values));

          OccSearchClient.FacetResult years = occ.years(d.getKey());
          doc.addField("year", atomicUpdate(years.values));

          OccSearchClient.FacetResult countries = occ.countries(d.getKey());
          doc.addField("country", atomicUpdate(countries.values));

          doc.addField("record_count", atomicUpdate(taxKeys.records));
          solr.add( doc );

          LOG.debug("Indexed {} dataset {} with {} records, {} taxa, {} countries and {} years: {}",
              d.getType(),
              d.getKey(),
              taxKeys.records,
              taxKeys.values.size(),
              countries.values.size(),
              years.values.size(),
              d.getTitle()
          );
        }
        page.nextPage();

      } while (!response.isEndOfRecords());

      solr.commit();
      LOG.info("Finished updating Dataset index with occurrence taxon keys");

    } catch (Exception e) {
      LOG.error("Failed to index occurrence taxon keys", e);
    }

  }

  public static void run (Properties props) {
    try {
      Injector registryInjector = registryInjector(props);
      DatasetService datasetService = registryInjector.getInstance(DatasetService.class);

      SolrConfig datasetSolr = SolrConfig.fromProperties(props, SOLR_DATASET_PREFIX);

      SolrConfig occSolr = SolrConfig.fromProperties(props, SOLR_OCCURRENCE_PREFIX);
      OccSearchClient occ = new OccSearchClient(occSolr);

      DatasetIndexOccurrenceUpdater updater = new DatasetIndexOccurrenceUpdater(occ, datasetSolr, datasetService);
      updater.run();

    } catch (Exception e) {
      LOG.error("Failed to run dataset index occurrence updater", e);
      System.exit(1);
    }
  }

  public static void main (String[] args) throws IOException {
    run(WorkflowUtils.loadProperties(args));
    System.exit(0);
  }
}
