/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.registry.doi.generator;

import org.gbif.common.messaging.api.Message;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.registry.doi.config.RabbitSenderConfiguration;

import java.io.IOException;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

// TODO: 26/08/2019 implement methods
@Service
public class RegistryMessagePublisher implements MessagePublisher {

  private final RabbitTemplate rabbitTemplate;

  public RegistryMessagePublisher(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  @Override
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
