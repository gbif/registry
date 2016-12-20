package org.gbif.registry.search.dataset;

import org.gbif.registry.search.dataset.checklist.DatasetIndexChecklistUpdater;
import org.gbif.registry.search.dataset.occurrence.DatasetIndexOccurrenceUpdater;

/**
 *
 */
public class DatasetIndexApp {

  public static void main(String[] args) throws Exception {
    // run dataset index builder just as in oozie
    DatasetIndexBuilder.main(args);

    // run checklist updaters just as in oozie
    DatasetIndexChecklistUpdater.main(args);

    // run occurrence updaters just as in oozie
    DatasetIndexOccurrenceUpdater.main(args);

    // success
    System.exit(0);
  }

}
