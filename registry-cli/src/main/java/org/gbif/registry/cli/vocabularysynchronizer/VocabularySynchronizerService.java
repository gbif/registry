package org.gbif.registry.cli.vocabularysynchronizer;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.gbif.common.messaging.DefaultMessageRegistry;
import org.gbif.common.messaging.MessageListener;
import org.gbif.registry.cli.common.spring.SpringContextBuilder;
import org.gbif.registry.service.VocabularyPostProcessor;
import org.gbif.registry.service.WithMyBatis;

import org.springframework.context.ApplicationContext;

import com.google.common.util.concurrent.AbstractIdleService;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class VocabularySynchronizerService extends AbstractIdleService {

  private final VocabularySynchronizerConfiguration config;
  private MessageListener listener;

  public VocabularySynchronizerService(VocabularySynchronizerConfiguration config) {
    this.config = config;
  }

  @Override
  protected void startUp() throws Exception {
    ApplicationContext ctx;
    log.info("Starting VocabularySynchronizerService with parameters: {}", config);

    if (config.vocabulariesToProcess == null || config.vocabulariesToProcess.isEmpty()) {
      throw new IllegalArgumentException("vocabulariesToProcess cannot be null or empty");
    }
    if (config.apiRootUrl == null || config.apiRootUrl.trim().isEmpty()) {
      throw new IllegalArgumentException("apiRootUrl cannot be null or empty");
    }
    if (config.poolSize <= 0) {
      throw new IllegalArgumentException("poolSize must be positive");
    }
    if (config.queueName == null || config.queueName.trim().isEmpty()) {
      throw new IllegalArgumentException("queueName cannot be null or empty");
    }

    // Build the Spring context with database and concept client configuration
    ctx = SpringContextBuilder.create()
        .withDbConfiguration(config.getDbConfig())
        .withVocabularySynchronizerConfiguration(config)
        .withComponents(
            VocabularyPostProcessor.class,
            WithMyBatis.class)
        .build();

    // Get vocabularies to process
    Set<String> vocabulariesToProcess = config.vocabulariesToProcess;
    log.info("Configured to process vocabularies: {}", vocabulariesToProcess);

    // Get all VocabularyPostProcessor beans
    List<VocabularyPostProcessor> vocabularyPostProcessors = ctx.getBeansOfType(VocabularyPostProcessor.class).values().stream().collect(Collectors.toList());
    log.info("Found {} vocabulary post-processors: {}", vocabularyPostProcessors.size(),
             vocabularyPostProcessors.stream().map(p -> p.getClass().getSimpleName()).collect(Collectors.toList()));

    // Create the callback for handling vocabulary released messages
    VocabularySynchronizerCallback callback = new VocabularySynchronizerCallback(
        vocabularyPostProcessors,
        vocabulariesToProcess);

    // Set up the message listener
    listener = new MessageListener(
        config.messaging.getConnectionParameters(),
        new DefaultMessageRegistry(),
        ctx.getBean(ObjectMapper.class),
        config.poolSize);

    // Listen for VocabularyReleasedMessage
    listener.listen(config.queueName, config.poolSize, callback);

    log.info("VocabularySynchronizerService started successfully.");
  }

  @Override
  protected void shutDown() throws Exception {
    listener.close();
    log.info("VocabularySynchronizerService stopped.");
  }
}
