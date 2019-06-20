package org.gbif.registry.doi;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.gbif.common.messaging.api.messages.ChangeDoiMessage;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

// TODO: 2019-06-21 rename
// TODO: 2019-06-21 move to a dedicated package
@Configuration
public class RabbitSenderConfiguration {

  // TODO: 2019-06-24 use property
  public static final String QUEUE_REGISTRY_DOI = "registry-doi";
  public static final String QUEUE_DEAD_REGISTRY_DOI = "dead-registry-doi";

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
    final ObjectMapper objectMapper = new ObjectMapper();
    final SimpleModule changeDoiMessageSerializerModule = new SimpleModule();
    changeDoiMessageSerializerModule.addSerializer(ChangeDoiMessage.class, new ChangeDoiMessageSerializer());
    objectMapper.registerModule(changeDoiMessageSerializerModule);

    return new Jackson2JsonMessageConverter(objectMapper);
  }

  public static class ChangeDoiMessageSerializer extends StdSerializer<ChangeDoiMessage> {

    public ChangeDoiMessageSerializer() {
      super(ChangeDoiMessage.class);
    }

    @Override
    public void serialize(ChangeDoiMessage value, JsonGenerator gen, SerializerProvider provider) throws IOException {
      gen.writeStartObject();
      gen.writeStringField(ChangeDoiMessage.DOI_FIELD, value.getDoi().getDoiName());
      gen.writeStringField(ChangeDoiMessage.METADATA_FIELD, value.getMetadata());
      gen.writeStringField(ChangeDoiMessage.DOI_STATUS_FIELD, value.getStatus().name());
      gen.writeStringField(ChangeDoiMessage.TARGET_FIELD, value.getTarget().toString());
      gen.writeEndObject();
    }
  }
}
