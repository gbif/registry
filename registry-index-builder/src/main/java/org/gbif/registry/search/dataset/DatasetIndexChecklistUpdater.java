package org.gbif.registry.search.dataset;

import org.gbif.checklistbank.cli.analysis.DatasetIndexUpdater;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.common.search.solr.SolrConfig;
import org.gbif.registry.search.WorkflowUtils;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.registry.search.guice.RegistrySearchModule.SOLR_DATASET_PREFIX;


/**
 * A builder that will clear and build a new dataset index by paging over the given service.
 */
public class DatasetIndexChecklistUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(DatasetIndexChecklistUpdater.class);
  private final DatasetIndexUpdater updater;

  public DatasetIndexChecklistUpdater(ClbConfiguration clb, SolrConfig solr) {
    updater = new DatasetIndexUpdater(clb, solr);
  }

  /**
   * Pages over all datasets and adds them to SOLR.
   */
  public void build() throws Exception {
    Stopwatch stopwatch = Stopwatch.createStarted();
    LOG.info("Updating all checklists in dataset index");
    updater.indexAll();
    LOG.info("Finished updating all checklists in dataset index in {} secs", stopwatch.elapsed(TimeUnit.SECONDS));
    updater.close();
  }

  public static void main (String[] args) {
    try {
      // read properties and check args
      Properties props = WorkflowUtils.loadProperties(args);
      SolrConfig solr = SolrConfig.fromProperties(props, SOLR_DATASET_PREFIX);
      ClbConfiguration clb = ClbConfiguration.fromProperties(props);

      DatasetIndexChecklistUpdater idxBuilder = new DatasetIndexChecklistUpdater(clb, solr);
      LOG.info("Updating checklists in dataset index {} on {}", solr.collection, solr.serverHome);
      idxBuilder.build();
      LOG.info("Checklist indexing completed successfully.");
      System.exit(0);

    } catch (Exception e) {
      LOG.error("Failed to run checklist index updater", e);
      System.exit(1);
    }
  }
}
