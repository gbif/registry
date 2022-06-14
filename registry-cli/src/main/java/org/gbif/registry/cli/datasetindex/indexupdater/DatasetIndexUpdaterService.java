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

import org.gbif.api.service.registry.DatasetService;
import org.gbif.common.messaging.DefaultMessageRegistry;
import org.gbif.common.messaging.MessageListener;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.PipelinesIndexedMessage;
import org.gbif.common.messaging.api.messages.PipelinesInterpretedMessage;
import org.gbif.registry.cli.datasetindex.SpringContextBuilder;
import org.gbif.registry.search.dataset.indexing.DatasetRealtimeIndexer;

import org.springframework.context.ApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.AbstractIdleService;

import lombok.extern.slf4j.Slf4j;

/** A service which listens to the {@link PipelinesInterpretedMessage } */
@Slf4j
public class DatasetIndexUpdaterService extends AbstractIdleService {

  private final DatasetIndexUpdaterConfiguration config;
  private MessageListener listener;
  private MessagePublisher publisher;
  private ApplicationContext ctx;

  public DatasetIndexUpdaterService(DatasetIndexUpdaterConfiguration config) {
    this.config = config;
    ctx = SpringContextBuilder.applicationContext(config);
  }

  @Override
  protected void startUp() throws Exception {
    log.info("Started dataset-index-updater service with parameters : {}", config);

    DatasetIndexUpdaterCallback callback =
        new DatasetIndexUpdaterCallback(
            ctx.getBean(DatasetRealtimeIndexer.class), ctx.getBean(DatasetService.class));

    PipelinesIndexedMessage em = new PipelinesIndexedMessage();
    // we run all the events pipelines in distributed mode
    String routingKey = em.getRoutingKey() + ".*";

    listener =
        new MessageListener(
            config.messaging.getConnectionParameters(),
            new DefaultMessageRegistry(),
            ctx.getBean(ObjectMapper.class),
            1);
    listener.listen(config.queueName, routingKey, config.poolSize, callback);
  }

  @Override
  protected void shutDown() {
    listener.close();
    publisher.close();
    log.info("Stopping dataset-index-updater service");
  }
}
