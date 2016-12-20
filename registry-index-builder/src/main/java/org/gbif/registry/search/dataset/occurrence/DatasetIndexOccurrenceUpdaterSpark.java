package org.gbif.registry.search.dataset.occurrence;

import org.gbif.common.search.solr.SolrConfig;
import org.gbif.registry.search.WorkflowUtils;

import java.io.IOException;
import java.util.Properties;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.registry.search.guice.RegistrySearchModule.SOLR_DATASET_PREFIX;


/**
 * A builder that will clear and build a new dataset index by paging over the given service.
 */
public class DatasetIndexOccurrenceUpdaterSpark {
  private final static String P_OCC_HDFS_PATH = "";

  private static final Logger LOG = LoggerFactory.getLogger(DatasetIndexOccurrenceUpdaterSpark.class);

  private final Config cfg;

  public DatasetIndexOccurrenceUpdaterSpark(Config cfg) {
    this.cfg = cfg;
  }

  static class Config {
    public SparkConf spark;
    public SolrConfig solr;
  }

  public void run() {
    LOG.info("Updating occurrence datasets in index {} on {}", cfg.solr.collection, cfg.solr.serverHome);
    try (JavaSparkContext sc = new JavaSparkContext(cfg.spark)) {

      JavaRDD<String> logData = sc.textFile("hdfs://nameservice1/user/mdoering/dracut.log").cache();

      long numMacs = logData.filter(new Function<String, Boolean>() {
        public Boolean call(String s) {
          return s.contains("mac");
        }
      }).count();

      System.out.println("Lines with mac: " + numMacs);

      //HiveContext ctx = new HiveContext(sc);
      // Queries are expressed in HiveQL
      //Row[] results = ctx.sql("SELECT datasetKey, taxonKey FROM occurrence_hdfs").collect();

      sc.stop();
      LOG.info("Occurrence dataset indexing completed successfully.");
    }
  }

  public static void run (Properties props) {
    try {
      // read properties and check args
      Config cfg = new Config();
      cfg.spark = new SparkConf().setAppName(DatasetIndexOccurrenceUpdaterSpark.class.getSimpleName());
      cfg.solr = SolrConfig.fromProperties(props, SOLR_DATASET_PREFIX);

      DatasetIndexOccurrenceUpdaterSpark updater = new DatasetIndexOccurrenceUpdaterSpark(cfg);
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
