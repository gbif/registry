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
package org.gbif.registry.cli.doiupdater;

import org.gbif.common.messaging.MessageListener;
import org.gbif.doi.service.DoiService;
import org.gbif.registry.cli.common.spring.SpringContextBuilder;
import org.gbif.registry.persistence.mapper.DoiMapper;

import org.springframework.context.ApplicationContext;

import com.google.common.util.concurrent.AbstractIdleService;

/**
 * A CLI service that starts and stops a listener of DoiUpdate messages. Must always be only one
 * thread - multiple will introduce a possible race (e.g. delete before create).
 */
@SuppressWarnings("UnstableApiUsage")
public class DoiUpdaterService extends AbstractIdleService {

  private final DoiUpdaterConfiguration config;

  private MessageListener listener;

  public DoiUpdaterService(DoiUpdaterConfiguration config) {
    this.config = config;
  }

  @Override
  protected void startUp() throws Exception {
    config.ganglia.start();

    ApplicationContext ctx =
        SpringContextBuilder.create().withDoiUpdaterConfiguration(config).build();

    listener = new MessageListener(config.messaging.getConnectionParameters(), 1);
    listener.listen(
        config.queueName,
        1,
        new DoiUpdateListener(
            ctx.getBean(DoiService.class), ctx.getBean(DoiMapper.class), config.timeToRetryInMs));
  }

  @Override
  protected void shutDown() {
    if (listener != null) {
      listener.close();
    }
  }
}
