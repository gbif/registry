package org.gbif.registry.cli;

import org.gbif.common.messaging.MessageListener;
import org.gbif.registry.cli.configuration.DoiUpdaterConfiguration;
import org.gbif.registry.persistence.mapper.DoiMapper;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * A CLI service that starts and stops a listener of DoiUpdate messages. Must always be only one thread - multiple
 * will introduce a possible race (e.g. delete before create).
 */
public class DoiUpdaterService extends AbstractIdleService {

  private final DoiUpdaterConfiguration config;

  private MessageListener listener;

  public DoiUpdaterService(DoiUpdaterConfiguration config) {
    this.config = config;
  }

  @Override
  protected void startUp() throws Exception {
    config.ganglia.start();

    Injector inj = Guice.createInjector(config.db.createMyBatisModule());
    listener = new MessageListener(config.messaging.getConnectionParameters());
    listener.listen(config.queueName, 1,
      new DoiUpdateListener(config.datacite.createService(), inj.getInstance(DoiMapper.class), config.timeToRetryInMs));
  }

  @Override
  protected void shutDown() throws Exception {
    if (listener != null) {
      listener.close();
    }
  }
}
