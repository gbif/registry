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
package org.gbif.registry.doi.config;

import org.gbif.common.messaging.api.messages.ChangeDoiMessage;

import java.io.IOException;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

@Configuration
public class RabbitSenderConfiguration {

  public static final String QUEUE_REGISTRY_DOI = "registry-doi";
  public static final String QUEUE_DEAD_REGISTRY_DOI = "dead-registry-doi";

  private final ObjectMapper objectMapper;

  public RabbitSenderConfiguration(@Qualifier("registryObjectMapper") ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Bean
  Queue registryDoiQueue() {
    return QueueBuilder.durable(QUEUE_REGISTRY_DOI).build();
  }

  @Bean
  Queue deadLetterQueue() {
    return QueueBuilder.durable(QUEUE_DEAD_REGISTRY_DOI).build();
  }

  @Bean
  public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory) {
    final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(producerJackson2MessageConverter());
    return rabbitTemplate;
  }

  @Bean
  public Jackson2JsonMessageConverter producerJackson2MessageConverter() {
    final SimpleModule changeDoiMessageSerializerModule = new SimpleModule();
    changeDoiMessageSerializerModule.addSerializer(
        ChangeDoiMessage.class, new ChangeDoiMessageSerializer());
    objectMapper.registerModule(changeDoiMessageSerializerModule);

    return new Jackson2JsonMessageConverter(objectMapper);
  }

  public static class ChangeDoiMessageSerializer extends StdSerializer<ChangeDoiMessage> {

    public ChangeDoiMessageSerializer() {
      super(ChangeDoiMessage.class);
    }

    @Override
    public void serialize(ChangeDoiMessage value, JsonGenerator gen, SerializerProvider provider)
        throws IOException {
      gen.writeStartObject();
      gen.writeStringField(ChangeDoiMessage.DOI_FIELD, value.getDoi().getDoiName());
      gen.writeStringField(ChangeDoiMessage.METADATA_FIELD, value.getMetadata());
      gen.writeStringField(ChangeDoiMessage.DOI_STATUS_FIELD, value.getStatus().name());
      gen.writeStringField(ChangeDoiMessage.TARGET_FIELD, value.getTarget().toString());
      gen.writeEndObject();
    }
  }
}
