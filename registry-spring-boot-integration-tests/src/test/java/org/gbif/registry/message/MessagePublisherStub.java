package org.gbif.registry.message;

import org.gbif.common.messaging.api.Message;
import org.gbif.common.messaging.api.MessagePublisher;

import java.io.IOException;

public class MessagePublisherStub implements MessagePublisher {

  @Override
  public void send(Message message) throws IOException {
  }

  @Override
  public void send(Message message, boolean persistent) throws IOException {
  }

  @Override
  public void send(Message message, String exchange) throws IOException {
  }

  @Override
  public void send(Object message, String exchange, String routingKey) throws IOException {
  }

  @Override
  public void send(Object message, String exchange, String routingKey, boolean persistent) throws IOException {
  }

  @Override
  public void close() {
  }
}
