package org.gbif.registry.search.dataset;

/**
 *
 */
public class DatasetIndexApp {

  public static void main(String[] args) throws Exception {
    // run dataset index builder just as in oozie
    DatasetIndexBuilder.main(args);

    // This should not be used in production at the moment (not ready)
    // run checklist updaters just as in oozie
    //DatasetIndexChecklistUpdater.main(args);

    // run occurrence updaters just as in oozie
    //DatasetIndexOccurrenceUpdater.main(args);

    // success
    System.exit(0);
  }

}
