package org.gbif.registry.cli.doiupdater;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiStatus;
import org.gbif.common.messaging.api.messages.ChangeDoiMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;

import java.io.IOException;
import java.net.URI;

@Configuration
public class RabbitListenerConfiguration implements RabbitListenerConfigurer {

  // TODO: 2019-06-24 use property
  public static final String QUEUE_REGISTRY_DOI = "registry-doi";

  @Override
  public void configureRabbitListeners(RabbitListenerEndpointRegistrar registrar) {
    registrar.setMessageHandlerMethodFactory(messageHandlerMethodFactory());
  }

  @Bean
  MessageHandlerMethodFactory messageHandlerMethodFactory() {
    final DefaultMessageHandlerMethodFactory messageHandlerMethodFactory = new DefaultMessageHandlerMethodFactory();
    messageHandlerMethodFactory.setMessageConverter(consumerJackson2MessageConverter());
    return messageHandlerMethodFactory;
  }

  @Bean
  public MappingJackson2MessageConverter consumerJackson2MessageConverter() {
    final MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
    final ObjectMapper objectMapper = new ObjectMapper();
    final SimpleModule module = new SimpleModule();
    module.addDeserializer(ChangeDoiMessage.class, new ChangeDoiMessageDeserializer());
    objectMapper.registerModule(module);
    converter.setObjectMapper(objectMapper);

    return converter;
  }

  public static class ChangeDoiMessageDeserializer extends StdDeserializer<ChangeDoiMessage> {

    public ChangeDoiMessageDeserializer() {
      super(ChangeDoiMessage.class);
    }

    @Override
    public ChangeDoiMessage deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      final JsonNode node = p.getCodec().readTree(p);

      final String status = node.get(ChangeDoiMessage.DOI_STATUS_FIELD).asText();
      final String target = node.get(ChangeDoiMessage.TARGET_FIELD).asText();
      final String metadata = node.get(ChangeDoiMessage.METADATA_FIELD).asText();
      final String doi = node.get(ChangeDoiMessage.DOI_FIELD).asText();

      return new ChangeDoiMessage(DoiStatus.valueOf(status), new DOI(doi), metadata, URI.create(target));
    }
  }
}
