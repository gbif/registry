package org.gbif.registry.search.dataset;

/**
 *
 */
public class DatasetIndexApp {

  public static void main(String[] args) throws Exception {
    // run dataset index builder just as in oozie
    DatasetIndexBuilder.main(args);

    // run checklist updaters just as in oozie
    DatasetIndexChecklistUpdater.main(args);

    System.exit(0);
  }

}
