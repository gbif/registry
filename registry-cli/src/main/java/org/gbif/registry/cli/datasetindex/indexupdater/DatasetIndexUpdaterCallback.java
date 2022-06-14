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
package org.gbif.registry.cli.datasetindex.indexupdater;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.common.messaging.AbstractMessageCallback;
import org.gbif.common.messaging.api.messages.PipelinesIndexedMessage;
import org.gbif.registry.search.dataset.indexing.DatasetRealtimeIndexer;

import java.util.Objects;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

/** Callback which is called when the {@link PipelinesIndexedMessage} is received. */
@Slf4j
public class DatasetIndexUpdaterCallback extends AbstractMessageCallback<PipelinesIndexedMessage> {

  private final DatasetRealtimeIndexer datasetRealtimeIndexer;
  private final DatasetService datasetService;

  public DatasetIndexUpdaterCallback(
      DatasetRealtimeIndexer datasetRealtimeIndexer, DatasetService datasetService) {
    this.datasetRealtimeIndexer = datasetRealtimeIndexer;
    this.datasetService = datasetService;
  }

  @Override
  public void handleMessage(PipelinesIndexedMessage pipelinesIndexedMessage) {
    UUID datasetKey = Objects.requireNonNull(pipelinesIndexedMessage.getDatasetUuid());

    Dataset dataset = datasetService.get(datasetKey);

    if (dataset == null) {
      log.error("Dataset not found for key {}", datasetKey);
      return;
    }

    log.info("Indexing dataset {}", datasetKey);
    datasetRealtimeIndexer.index(dataset);
  }
}
