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
package org.gbif.registry.cli.vocabularysynchronizer;

import org.gbif.common.messaging.DefaultMessageRegistry;
import org.gbif.common.messaging.MessageListener;
import org.gbif.registry.cli.common.spring.SpringContextBuilder;
import org.gbif.registry.cli.common.stubs.DatasetServiceStub;
import org.gbif.registry.cli.common.stubs.RegistryDatasetServiceStub;
import org.gbif.registry.cli.common.stubs.RegistryDerivedDatasetServiceStub;
import org.gbif.registry.service.VocabularyPostProcessor;
import org.gbif.registry.service.VocabularyConceptService;
import org.gbif.registry.service.DatasetCategoryService;
import org.gbif.registry.service.WithMyBatis;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.AbstractIdleService;

import lombok.extern.slf4j.Slf4j;

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
            VocabularyConceptService.class,
            DatasetCategoryService.class,
            RegistryDatasetServiceStub.class,
            RegistryDerivedDatasetServiceStub.class,
            DatasetServiceStub.class,
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
