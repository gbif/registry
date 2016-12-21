package org.gbif.registry.search.dataset.occurrence;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.common.search.Facet;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.model.occurrence.Occurrence;
import org.gbif.api.model.occurrence.search.OccurrenceSearchParameter;
import org.gbif.api.model.occurrence.search.OccurrenceSearchRequest;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.common.search.solr.SolrConfig;
import org.gbif.registry.search.WorkflowUtils;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.registry.search.dataset.DatasetIndexBuilder.registryInjector;
import static org.gbif.registry.search.dataset.checklist.DatasetIndexChecklistUpdater.atomicUpdate;
import static org.gbif.registry.search.guice.RegistrySearchModule.SOLR_DATASET_PREFIX;


public class DatasetIndexOccurrenceUpdater {
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
          LOG.debug("Indexing {} dataset {}: {}", d.getType(), d.getKey(), d.getTitle());
          Set<Integer> taxKeys = taxonKeys(d.getKey());

          SolrInputDocument doc = new SolrInputDocument();
          doc.addField("key", d.getKey().toString());
          doc.addField("taxon_key", atomicUpdate(taxKeys));
          doc.addField("record_count", atomicUpdate(taxKeys.size()));

          solr.add( doc );
        }
        page.nextPage();

      } while (!response.isEndOfRecords());

      solr.commit();
      LOG.info("Finished updating Dataset index with occurrence taxon keys");

    } catch (Exception e) {
      LOG.error("Failed to index occurrence taxon keys", e);
    }

  }

  private Set<Integer> taxonKeys(UUID datasetKey) {
    Set<Integer> keys = Sets.newHashSet();
    try {
      OccurrenceSearchRequest req = new OccurrenceSearchRequest(0, 0);
      req.addDatasetKeyFilter(datasetKey);
      req.addFacets(OccurrenceSearchParameter.TAXON_KEY);
      req.setFacetLimit(PAGE_SIZE);
      req.setFacetOffset(0);

      List<Facet.Count> counts;
      do {
        SearchResponse<Occurrence, OccurrenceSearchParameter> resp = occ.search(req);
        counts = resp.getFacets().get(0).getCounts();
        for (Facet.Count count : counts) {
          keys.add(Integer.valueOf(count.getName()));
        }
        req.setFacetOffset(req.getFacetOffset()+PAGE_SIZE);
      } while (!counts.isEmpty());

    } catch (RuntimeException e) {
      LOG.error("Failed to query solr for taxon key facets of dataset {}. Index out of date", datasetKey, e);
    }

    LOG.debug("Found {} taxon keys for dataset {}", keys.size(), datasetKey);
    return keys;
  }

  public static void run (Properties props) {
    try {
      SolrConfig datasetSolr = SolrConfig.fromProperties(props, SOLR_DATASET_PREFIX);

      Injector occInj = Guice.createInjector(new OccClientModule(props));
      OccSearchClient occ = occInj.getInstance(OccSearchClient.class);

      Injector registryInjector = registryInjector(props);
      DatasetService datasetService = registryInjector.getInstance(DatasetService.class);

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
