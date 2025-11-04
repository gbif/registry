/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.search.test;

import org.gbif.registry.search.dataset.indexing.DatasetRealtimeIndexer;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;

public class DatasetSearchUpdateUtils {

  // how often to poll and wait for ES to update
  private static final int UPDATE_TIMEOUT_SECS = 10;
  private static final int UPDATE_POLL_MSECS = 20;
  private static final int INITIAL_WAIT_MSECS = 100; // Wait for async events to reach indexer
  private static final int STABILITY_CHECK_MSECS = 100; // Wait to ensure no more updates coming
  private static final Logger LOG = LoggerFactory.getLogger(DatasetSearchUpdateUtils.class);

  /** Waits for ElasticSearch update threads to finish. */
  public static void awaitUpdates(DatasetRealtimeIndexer indexService, ElasticsearchTestContainerConfiguration elasticsearchTestContainer) {
    Preconditions.checkNotNull(indexService, "Index service is required");
    Preconditions.checkNotNull(elasticsearchTestContainer, "ElasticsearchTestContainerConfiguration is required");
    try {
      Stopwatch stopWatch = Stopwatch.createStarted();

      // Initial wait for async events (like organization updates) to trigger dataset reindexing
      Thread.sleep(INITIAL_WAIT_MSECS);

      // Wait for all pending updates to complete
      while (indexService.getPendingUpdates() > 0) {
        Thread.sleep(UPDATE_POLL_MSECS);
        if (stopWatch.elapsed(TimeUnit.SECONDS) > UPDATE_TIMEOUT_SECS) {
          throw new IllegalStateException(
              "Failing test due to unreasonable timeout on ElasticSearch update");
        }
      }

      // Wait a bit more to ensure no new updates are triggered
      Thread.sleep(STABILITY_CHECK_MSECS);

      // Final check - if new updates appeared, wait for them too
      if (indexService.getPendingUpdates() > 0) {
        while (indexService.getPendingUpdates() > 0) {
          Thread.sleep(UPDATE_POLL_MSECS);
          if (stopWatch.elapsed(TimeUnit.SECONDS) > UPDATE_TIMEOUT_SECS) {
            throw new IllegalStateException(
                "Failing test due to unreasonable timeout on ElasticSearch update");
          }
        }
      }

      elasticsearchTestContainer.refreshIndex();
      LOG.info(
          "Waited {} msecs for Elasticsearch update backlog to clear successfully",
          stopWatch.elapsed(TimeUnit.MILLISECONDS));

    } catch (InterruptedException e) {
      throw Throwables.propagate(e);
    }
  }
}
