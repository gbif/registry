package org.gbif.registry.search.dataset.checklist;

import org.gbif.common.search.solr.SolrConfig;
import org.gbif.registry.search.WorkflowUtils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.registry.search.guice.RegistrySearchModule.SOLR_DATASET_PREFIX;


/**
 * A builder that will clear and build a new dataset index by paging over the given service.
 */
public class DatasetIndexChecklistUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(DatasetIndexChecklistUpdater.class);
  private static final String SQL = "SELECT dataset_key, array_agg(nub_fk) as keys " +
                                    "FROM nub_rel " +
                                    "GROUP BY dataset_key";
  private final ClbConnection clb;
  private final SolrConfig solrConfig;

  public DatasetIndexChecklistUpdater(ClbConnection clb, SolrConfig solr) {
    this.clb = clb;
    this.solrConfig = solr;
  }

  /**
   * Pages over all datasets and adds them to SOLR.
   */
  public void build() throws Exception {
    Stopwatch stopwatch = Stopwatch.createStarted();
    try (SolrClient solr = solrConfig.buildSolr();
         Connection conn = clb.connect()
    ) {
      LOG.info("Updating all checklists in dataset index");

      // use streaming cursor for large result sets
      conn.setAutoCommit(false);
      Statement st = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
      st.setFetchSize(2);

      ResultSet rs = st.executeQuery(SQL);
      while (rs.next()) {
        try {
          Integer[] keys = (Integer[])rs.getArray("keys").getArray();

          SolrInputDocument doc = new SolrInputDocument();
          doc.addField("key", rs.getString("dataset_key"));
          doc.addField("taxon_key", atomicUpdate(keys));
          doc.addField("record_count", atomicUpdate(keys.length));

          solr.add( doc );

        } catch (Exception e) {
          Throwables.propagate(e);
        }
      }
      rs.close();
      solr.commit(solrConfig.collection);
      LOG.info("Finished updating all checklists in dataset index in {} secs", stopwatch.elapsed(TimeUnit.SECONDS));

    } catch (Exception e) {
      LOG.error("Failed to index taxon keys for dataset index", e);
    }
  }

  /**
   * Atomic/partial updates require updateLog to be present in the schema!
   */
  public static Map<String, Object> atomicUpdate(Object value) {
    Map<String, Object> atomic = Maps.newHashMap();
    atomic.put("set", value); // set, add, remove, inc
    return atomic;
  }

  public static void run (Properties props) {
    try {
      // read properties and check args
      SolrConfig solr = SolrConfig.fromProperties(props, SOLR_DATASET_PREFIX);
      ClbConnection clb = new ClbConnection(props);

      DatasetIndexChecklistUpdater idxBuilder = new DatasetIndexChecklistUpdater(clb, solr);
      LOG.info("Updating checklists in dataset index {} on {}", solr.collection, solr.serverHome);
      idxBuilder.build();
      LOG.info("Checklist indexing completed successfully.");

    } catch (Exception e) {
      LOG.error("Failed to run dataset index checklist updater", e);
      System.exit(1);
    }
  }

  public static void main (String[] args) throws IOException {
    run(WorkflowUtils.loadProperties(args));
    System.exit(0);
  }
}
