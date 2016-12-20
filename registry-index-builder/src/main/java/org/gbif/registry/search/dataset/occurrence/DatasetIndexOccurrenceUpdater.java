package org.gbif.registry.search.dataset.occurrence;

import org.gbif.registry.search.WorkflowUtils;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DatasetIndexOccurrenceUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(DatasetIndexOccurrenceUpdater.class);

  public void run() {
    LOG.info("Updating occurrence datasets");
  }

  public static void run (Properties props) {
    try {
      DatasetIndexOccurrenceUpdater updater = new DatasetIndexOccurrenceUpdater();
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
