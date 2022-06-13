package org.gbif.registry.cli.datasetindex.indexupdater;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.common.messaging.AbstractMessageCallback;
import org.gbif.common.messaging.api.messages.PipelinesEventsMessage;
import org.gbif.common.messaging.api.messages.PipelinesIndexedMessage;
import org.gbif.registry.search.dataset.indexing.DatasetRealtimeIndexer;

import java.util.Objects;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

/** Callback which is called when the {@link PipelinesEventsMessage} is received. */
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
