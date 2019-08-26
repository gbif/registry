package org.gbif.registry.doi.generator;

import org.gbif.common.messaging.api.Message;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.registry.doi.RabbitSenderConfiguration;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;

// TODO: 26/08/2019 implement methods
@Service
public class RegistryMessagePublisher implements MessagePublisher {

  private final RabbitTemplate rabbitTemplate;

  public RegistryMessagePublisher(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  public void send(Message message) throws IOException {
    this.rabbitTemplate.convertAndSend(RabbitSenderConfiguration.QUEUE_REGISTRY_DOI, message);
  }

  @Override
  public void send(Message message, boolean b) throws IOException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void send(Message message, String s) throws IOException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void send(Object o, String s, String s1) throws IOException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void send(Object o, String s, String s1, boolean b) throws IOException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void close() {
    throw new UnsupportedOperationException("not implemented");
  }
}
