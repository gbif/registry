package org.gbif.registry.guice;

import org.gbif.common.messaging.api.MessagePublisher;

import com.google.inject.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.mock;

/**
 * Provides a mock doi service
 */
public class RabbitMockModule extends AbstractModule {
  private static final Logger LOG = LoggerFactory.getLogger(RabbitMockModule.class);

  @Override
  protected void configure() {
    MessagePublisher mp = mock(MessagePublisher.class);
    bind(MessagePublisher.class).toInstance(mp);
    LOG.info("Using a mocked Message Publisher");
  }
}
