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
package org.gbif.registry.cli.common.spring;

import org.gbif.api.ws.mixin.Mixins;
import org.gbif.common.messaging.ConnectionParameters;
import org.gbif.common.messaging.DefaultMessagePublisher;
import org.gbif.common.messaging.DefaultMessageRegistry;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.config.MessagingConfiguration;
import org.gbif.doi.service.DoiService;
import org.gbif.registry.cli.common.CommonBuilder;
import org.gbif.registry.cli.common.DataCiteConfiguration;
import org.gbif.registry.cli.common.DbConfiguration;
import org.gbif.registry.cli.common.DirectoryConfiguration;
import org.gbif.registry.cli.common.stubs.MessagePublisherStub;
import org.gbif.registry.cli.datasetindex.batchindexer.DatasetBatchIndexer;
import org.gbif.registry.cli.doisynchronizer.DoiSynchronizerConfiguration;
import org.gbif.registry.cli.doiupdater.DoiUpdaterConfiguration;
import org.gbif.registry.cli.vocabularysynchronizer.VocabularySynchronizerConfiguration;
import org.gbif.registry.directory.config.DirectoryClientConfiguration;
import org.gbif.registry.identity.service.BaseIdentityAccessService;
import org.gbif.registry.persistence.config.MyBatisConfiguration;
import org.gbif.registry.ws.resources.OccurrenceDownloadResource;
import org.gbif.registry.ws.resources.scheduled.UpdateDownloadStatsService;
import org.gbif.ws.security.Md5EncodeServiceImpl;
import org.gbif.ws.security.SecretKeySigningService;
import org.gbif.ws.security.SigningService;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.elasticsearch.ElasticSearchRestHealthContributorAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.archaius.ArchaiusAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.env.MapPropertySource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableMap;

/** Utility class to create Spring contexts to be used later in CLI applications. */
public class SpringContextBuilder {

  private String[] basePackages;

  private Class<?>[] componentClasses;

  private DbConfiguration dbConfiguration;

  private DirectoryConfiguration directoryConfiguration;

  private DoiSynchronizerConfiguration doiSynchronizerConfiguration;

  private DataCiteConfiguration dataCiteConfiguration;

  private MessagingConfiguration messagingConfiguration;

  private VocabularySynchronizerConfiguration vocabularySynchronizerConfiguration;

  private String conceptClientApiUrl;

  private SpringContextBuilder() {}

  public static SpringContextBuilder create() {
    return new SpringContextBuilder();
  }

  public SpringContextBuilder withDbConfiguration(DbConfiguration dbConfiguration) {
    this.dbConfiguration = dbConfiguration;
    return this;
  }

  public SpringContextBuilder withDirectoryConfiguration(
      DirectoryConfiguration directoryConfiguration) {
    this.directoryConfiguration = directoryConfiguration;
    return this;
  }

  public SpringContextBuilder withDataCiteConfiguration(
      DataCiteConfiguration dataCiteConfiguration) {
    this.dataCiteConfiguration = dataCiteConfiguration;
    return this;
  }

  public SpringContextBuilder withDoiUpdaterConfiguration(
      DoiUpdaterConfiguration doiUpdaterConfiguration) {
    this.dbConfiguration = doiUpdaterConfiguration.registry;
    this.dataCiteConfiguration = doiUpdaterConfiguration.datacite;
    return this;
  }

  public SpringContextBuilder withDoiSynchronizerConfiguration(
      DoiSynchronizerConfiguration doiSynchronizerConfiguration) {
    this.doiSynchronizerConfiguration = doiSynchronizerConfiguration;
    this.dbConfiguration = doiSynchronizerConfiguration.registry;
    this.dataCiteConfiguration = doiSynchronizerConfiguration.datacite;
    this.messagingConfiguration = doiSynchronizerConfiguration.messaging;
    return this;
  }

  public SpringContextBuilder withVocabularySynchronizerConfiguration(
      VocabularySynchronizerConfiguration vocabularySynchronizerConfiguration) {
    this.vocabularySynchronizerConfiguration = vocabularySynchronizerConfiguration;
    this.conceptClientApiUrl = vocabularySynchronizerConfiguration.apiRootUrl;
    return this;
  }

  public SpringContextBuilder withDatasetUpdaterConfiguration(
      org.gbif.registry.cli.datasetupdater.DatasetUpdaterConfiguration datasetUpdaterConfiguration) {
    this.dbConfiguration = datasetUpdaterConfiguration.db;
    this.conceptClientApiUrl = datasetUpdaterConfiguration.apiRootUrl;
    return this;
  }

  public SpringContextBuilder withComponents(Class<?>... componentClasses) {
    this.componentClasses = componentClasses;
    return this;
  }

  public SpringContextBuilder withScanPackages(String... basePackages) {
    this.basePackages = basePackages;
    return this;
  }

  public SpringContextBuilder withConceptClientApiUrl(String conceptClientApiUrl) {
    this.conceptClientApiUrl = conceptClientApiUrl;
    return this;
  }

  public AnnotationConfigApplicationContext build() {

    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();

    ctx.registerBean("registryObjectMapper", ObjectMapper.class, this::registryObjectMapper);

    Set<String> packages =
        basePackages == null ? new HashSet<>() : new HashSet<>(Arrays.asList(basePackages));

    if (dbConfiguration != null) {
      ctx.register(ApplicationConfig.class);
      ctx.register(MyBatisConfiguration.class);
      ctx.registerBean(DbConfiguration.class, () -> dbConfiguration);
      ctx.register(MybatisAutoConfiguration.class);
      packages.add("org.gbif.registry.persistence");
    }

    if (messagingConfiguration != null) {
      if (messagingConfiguration.host != null) {
        ctx.registerBean(
            "messagePublisher",
            MessagePublisher.class,
            () -> {
              try {
                return new DefaultMessagePublisher(
                    new ConnectionParameters(
                        messagingConfiguration.host,
                        messagingConfiguration.port,
                        messagingConfiguration.username,
                        messagingConfiguration.password,
                        messagingConfiguration.virtualHost),
                    new DefaultMessageRegistry(),
                    registryObjectMapper());
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
      } else {
        ctx.registerBean("messagePublisher", MessagePublisher.class, MessagePublisherStub::new);
      }
    }

    if (!packages.isEmpty()) {
      ctx.scan(packages.toArray(new String[] {}));
    }

    if (directoryConfiguration != null) {
      ctx.getEnvironment()
          .getPropertySources()
          .addLast(
              new MapPropertySource(
                  "directoryConfigProperties",
                  ImmutableMap.of(
                      "directory.ws.url", directoryConfiguration.wsUrl,
                      "directory.app.key", directoryConfiguration.appKey,
                      "directory.app.secret", directoryConfiguration.appSecret)));

      ctx.registerBean(
          "secretKeySigningService", SigningService.class, SecretKeySigningService::new);
      ctx.register(Md5EncodeServiceImpl.class);
      ctx.register(DirectoryClientConfiguration.class);
    }

    if (dataCiteConfiguration != null) {
      ctx.registerBean(
          "doiService",
          DoiService.class,
          () -> CommonBuilder.createRestJsonApiDataCiteService(dataCiteConfiguration));
    }

    if (doiSynchronizerConfiguration != null) {
      ctx.getEnvironment()
          .getPropertySources()
          .addLast(
              new MapPropertySource(
                  "doiSynchronizerConfigProperties",
                  ImmutableMap.of(
                      "api.root.url", doiSynchronizerConfiguration.apiRoot,
                      "portal.url", doiSynchronizerConfiguration.portalurl,
                      "doi.prefix", doiSynchronizerConfiguration.doiPrefix)));

      ctx.register(BaseIdentityAccessService.class);
      ctx.register(OccurrenceDownloadResource.class);
      ctx.register(UpdateDownloadStatsService.class);
    }

    if (vocabularySynchronizerConfiguration != null) {
      ctx.getEnvironment()
          .getPropertySources()
          .addLast(
              new MapPropertySource(
                  "vocabularySynchronizerConfigProperties",
                  ImmutableMap.of(
                      "api.root.url", vocabularySynchronizerConfiguration.apiRootUrl)));
      ctx.registerBean(VocabularySynchronizerConfiguration.class, () -> vocabularySynchronizerConfiguration);
    }

    if (conceptClientApiUrl != null) {
      ctx.registerBean("conceptClient", org.gbif.vocabulary.client.ConceptClient.class, () ->
          new org.gbif.ws.client.ClientBuilder()
              .withObjectMapper(org.gbif.ws.json.JacksonJsonObjectMapperProvider.getObjectMapperWithBuilderSupport()
                  .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule()))
              .withUrl(conceptClientApiUrl)
              .build(org.gbif.vocabulary.client.ConceptClient.class));
    }

    ctx.getEnvironment()
        .getPropertySources()
        .addLast(
            new MapPropertySource(
                "ManagedProperties",
                new ImmutableMap.Builder<String, Object>()
                    .put("spring.cloud.compatibility-verifier.enabled", "false")
                    .build()));

    if (componentClasses != null) {
      for (Class<?> c : componentClasses) {
        ctx.register(c);
      }
    }

    ctx.refresh();
    ctx.start();
    return ctx;
  }

  public ObjectMapper registryObjectMapper() {
    final ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    // determines whether encountering of unknown properties (ones that do not map to a property,
    // and there is no
    // "any setter" or handler that can handle it) should result in a failure (throwing a
    // JsonMappingException) or not.
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    // Enforce use of ISO-8601 format dates (http://wiki.fasterxml.com/JacksonFAQDateHandling)
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    Mixins.getPredefinedMixins().forEach(objectMapper::addMixIn);

    return objectMapper;
  }

  /** Class to help with the loading and injection of */
  @SpringBootApplication(
      exclude = {
        ElasticSearchRestHealthContributorAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        LiquibaseAutoConfiguration.class,
        FreeMarkerAutoConfiguration.class,
        ArchaiusAutoConfiguration.class
      })
  @MapperScan("org.gbif.registry.persistence.mapper")
  @EnableConfigurationProperties
  @ComponentScan(
      excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = DatasetBatchIndexer.class)
      })
  static class ApplicationConfig {}
}
