package org.gbif.registry.cli.vocabularyfacetupdater;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.gbif.common.messaging.DefaultMessageRegistry;
import org.gbif.common.messaging.MessageListener;
import org.gbif.registry.cli.common.spring.SpringContextBuilder;

import org.gbif.registry.service.WithMyBatis;
import org.gbif.registry.service.VocabularyConceptService;


import org.springframework.context.ApplicationContext;

import com.google.common.util.concurrent.AbstractIdleService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VocabularyFacetUpdaterService extends AbstractIdleService {

  private final VocabularyFacetUpdaterConfiguration config;
  private MessageListener listener;


  public VocabularyFacetUpdaterService(VocabularyFacetUpdaterConfiguration config) {
    this.config = config;
  }

  @Override
  protected void startUp() throws Exception {
    ApplicationContext ctx;
    log.info("Starting VocabularyFacetUpdaterService with parameters: {}", config);

    // Build the Spring context with database and concept client configuration
    ctx = SpringContextBuilder.create()
        .withVocabularyFacetUpdaterConfiguration(config)
        .withComponents(
            VocabularyConceptService.class,
            WithMyBatis.class)
        .build();

    // Create the callback for handling vocabulary released messages
    VocabularyFacetUpdaterCallback callback = new VocabularyFacetUpdaterCallback(
        ctx.getBean(VocabularyConceptService.class),
        config.getVocabulariesToProcess());

    // Set up the message listener
    listener = new MessageListener(
        config.messaging.getConnectionParameters(),
        new DefaultMessageRegistry(),
        ctx.getBean(ObjectMapper.class),
        config.poolSize);

    // Listen for VocabularyReleasedMessage
    listener.listen(config.queueName, config.poolSize, callback);

    log.info("VocabularyFacetUpdaterService started successfully.");
  }

  @Override
  protected void shutDown() throws Exception {
    listener.close();
    log.info("VocabularyFacetUpdaterService stopped.");
  }
}
