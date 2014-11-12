package org.gbif.registry.search;

import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DatasetSearchUpdateUtils {

  // how often to poll and wait for SOLR to update
  private static final int SOLR_UPDATE_TIMEOUT_SECS = 10;
  private static final int SOLR_UPDATE_POLL_MSECS = 10;
  private static final Logger LOG = LoggerFactory.getLogger(DatasetSearchUpdateUtils.class);

  /**
   * Waits for SOLR update threads to finish.
   */
  public static void awaitUpdates(DatasetIndexUpdateListener datasetIndexUpdater) {
    Preconditions.checkNotNull(datasetIndexUpdater, "Index updater is required");
    try {
      Stopwatch stopWatch = Stopwatch.createStarted();
      while (datasetIndexUpdater.queuedUpdates() > 0) {
        Thread.sleep(SOLR_UPDATE_POLL_MSECS);
        if (stopWatch.elapsed(TimeUnit.SECONDS) > SOLR_UPDATE_TIMEOUT_SECS) {
          throw new IllegalStateException("Failing test due to unreasonable timeout on SOLR update");
        }
      }
      LOG.info("Waited {} msecs for SOLR update backlog to clear successfully",
        stopWatch.elapsed(TimeUnit.MILLISECONDS));

    } catch (InterruptedException e) {
      throw Throwables.propagate(e);
    }
  }
}
