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
package org.gbif.registry.messaging;

import org.gbif.common.messaging.ConnectionParameters;
import org.gbif.common.messaging.DefaultMessagePublisher;
import org.gbif.common.messaging.DefaultMessageRegistry;
import org.gbif.common.messaging.api.MessagePublisher;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class RegistryRabbitConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(RegistryRabbitConfiguration.class);

  public static final String QUEUE_REGISTRY_DOI = "registry-doi";
  public static final String QUEUE_DEAD_REGISTRY_DOI = "dead-registry-doi";

  private final ObjectMapper objectMapper;
  private final RabbitProperties rabbitProperties;

  public RegistryRabbitConfiguration(
      @Qualifier("registryObjectMapper") ObjectMapper objectMapper,
      RabbitProperties rabbitProperties) {
    this.objectMapper = objectMapper;
    this.rabbitProperties = rabbitProperties;
  }

  @Bean
  @ConditionalOnProperty(value = "message.enabled", havingValue = "true")
  public MessagePublisher messagePublisher() throws IOException {
    LOG.info("DefaultMessagePublisher activated");
    return new DefaultMessagePublisher(
        new ConnectionParameters(
            rabbitProperties.getHost(),
            rabbitProperties.getPort(),
            rabbitProperties.getUsername(),
            rabbitProperties.getPassword(),
            rabbitProperties.getVirtualHost()),
        new DefaultMessageRegistry(),
        objectMapper);
  }

  @Bean
  Queue registryDoiQueue() {
    return QueueBuilder.durable(QUEUE_REGISTRY_DOI).build();
  }

  @Bean
  Queue deadLetterQueue() {
    return QueueBuilder.durable(QUEUE_DEAD_REGISTRY_DOI).build();
  }
}
