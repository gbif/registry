package org.gbif.registry.cli;

import org.gbif.common.messaging.MessageListener;
import org.gbif.doi.service.DoiService;
import org.gbif.doi.service.ServiceConfig;
import org.gbif.doi.service.datacite.DataCiteService;

import com.google.common.util.concurrent.AbstractIdleService;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * A CLI service that starts and stops a listener of DoiUpdate messages.
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

    // TODO: does httpclient need more config?
    DoiService doiService = new DataCiteService(HttpClientBuilder.create().build(),
      new ServiceConfig(config.doiUsername, config.doiPassword));

    // TODO: add db setup and include in listener constructor

    listener = new MessageListener(config.messaging.getConnectionParameters());
    listener.listen(config.queueName, config.msgPoolSize, new DoiUpdateListener(doiService, null));
  }

  @Override
  protected void shutDown() throws Exception {
    if (listener != null) {
      listener.close();
    }
  }
}
