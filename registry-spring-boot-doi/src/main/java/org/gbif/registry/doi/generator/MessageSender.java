package org.gbif.registry.doi.generator;

import org.gbif.common.messaging.api.Message;
import org.gbif.registry.doi.RabbitSenderConfiguration;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;

// TODO: 2019-06-22 change location (package/project)?
// TODO: 2019-06-22 change name
@Service
public class MessageSender {

  private final RabbitTemplate rabbitTemplate;

  public MessageSender(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  // TODO: 2019-06-22 throws IOException because of messagePublisher throws...
  public void send(Message message) throws IOException {
    this.rabbitTemplate.convertAndSend(RabbitSenderConfiguration.QUEUE_REGISTRY_DOI, message);
  }
}
