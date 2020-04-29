/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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

  // how often to poll and wait for SOLR to update
  private static final int UPDATE_TIMEOUT_SECS = 10;
  private static final int UPDATE_POLL_MSECS = 10;
  private static final Logger LOG = LoggerFactory.getLogger(DatasetSearchUpdateUtils.class);

  /** Waits for ElasticSearch update threads to finish. */
  public static void awaitUpdates(DatasetRealtimeIndexer indexService, EsManageServer esServer) {
    Preconditions.checkNotNull(indexService, "Index service is required");
    Preconditions.checkNotNull(esServer, "EsServer is required");
    try {
      Stopwatch stopWatch = Stopwatch.createStarted();
      while (indexService.getPendingUpdates() > 0) {
        Thread.sleep(UPDATE_POLL_MSECS);
        if (stopWatch.elapsed(TimeUnit.SECONDS) > UPDATE_TIMEOUT_SECS) {
          throw new IllegalStateException(
              "Failing test due to unreasonable timeout on SOLR update");
        }
      }
      esServer.refresh();
      LOG.info(
          "Waited {} msecs for Elasticsearch update backlog to clear successfully",
          stopWatch.elapsed(TimeUnit.MILLISECONDS));

    } catch (InterruptedException e) {
      throw Throwables.propagate(e);
    }
  }
}
