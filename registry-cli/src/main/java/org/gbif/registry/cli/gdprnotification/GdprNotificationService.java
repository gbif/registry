package org.gbif.registry.cli.gdprnotification;

import org.gbif.common.messaging.MessageListener;

import com.google.common.util.concurrent.AbstractIdleService;

public class GdprNotificationService extends AbstractIdleService {

  // we use a pool size of 1 in order to avoid race conditions when consulting the DB.
  private static final int POOL_SIZE = 1;

  private final GdprNotificationConfiguration configuration;
  private MessageListener listener;

  public GdprNotificationService(GdprNotificationConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  protected void startUp() throws Exception {
    listener = new MessageListener(configuration.messaging.getConnectionParameters());
    listener.listen(configuration.queueName, POOL_SIZE, GdprNotificationListener.newInstance(configuration));
  }

  @Override
  protected void shutDown() throws Exception {
    if (listener != null) {
      listener.close();
    }
  }
}
