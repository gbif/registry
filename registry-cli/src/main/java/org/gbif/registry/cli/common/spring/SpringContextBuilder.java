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
package org.gbif.registry.cli.common.spring;

import org.gbif.api.ws.mixin.Mixins;
import org.gbif.cli.indexing.dataset.DatasetBatchIndexBuilder;
import org.gbif.common.messaging.config.MessagingConfiguration;
import org.gbif.doi.service.DoiService;
import org.gbif.registry.cli.common.CommonBuilder;
import org.gbif.registry.cli.common.DataCiteConfiguration;
import org.gbif.registry.cli.common.DbConfiguration;
import org.gbif.registry.cli.common.DirectoryConfiguration;
import org.gbif.registry.cli.doisynchronizer.DoiSynchronizerConfiguration;
import org.gbif.registry.cli.doiupdater.DoiUpdaterConfiguration;
import org.gbif.registry.directory.client.config.DirectoryClientConfiguration;
import org.gbif.registry.identity.service.BaseIdentityAccessService;
import org.gbif.registry.messaging.RegistryRabbitConfiguration;
import org.gbif.registry.ws.config.MyBatisConfiguration;
import org.gbif.registry.ws.resources.OccurrenceDownloadResource;
import org.gbif.ws.security.Md5EncodeServiceImpl;
import org.gbif.ws.security.SecretKeySigningService;
import org.gbif.ws.security.SigningService;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.elasticsearch.ElasticSearchRestHealthContributorAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchAutoConfiguration;
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

  public SpringContextBuilder withComponents(Class<?>... componentClasses) {
    this.componentClasses = componentClasses;
    return this;
  }

  public SpringContextBuilder withScanPackages(String... basePackages) {
    this.basePackages = basePackages;
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
      ctx.getEnvironment()
          .getPropertySources()
          .addLast(
              new MapPropertySource(
                  "rabbitConfigProperties",
                  ImmutableMap.of(
                      "spring.rabbitmq.host", messagingConfiguration.host,
                      "spring.rabbitmq.port", messagingConfiguration.port,
                      "spring.rabbitmq.username", messagingConfiguration.username,
                      "spring.rabbitmq.password", messagingConfiguration.password,
                      "spring.rabbitmq.virtualHost", messagingConfiguration.virtualHost)));

      ctx.getEnvironment()
          .getPropertySources()
          .addLast(
              new MapPropertySource(
                  "messagingConfigProperties", ImmutableMap.of("message.enabled", true)));
      ctx.registerBean(RegistryRabbitConfiguration.class);
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

      SigningService signingService = new SecretKeySigningService();

      ctx.registerBean("secretKeySigningService", SigningService.class, () -> signingService);
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
    }

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
        ElasticsearchAutoConfiguration.class,
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
            classes = DatasetBatchIndexBuilder.class)
      })
  static class ApplicationConfig {}
}
