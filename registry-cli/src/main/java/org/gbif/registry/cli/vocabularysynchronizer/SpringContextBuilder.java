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

import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NetworkService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.cli.common.DbConfiguration;
import org.gbif.registry.cli.datasetindex.ElasticsearchConfig;
import org.gbif.registry.persistence.config.MyBatisConfiguration;
import org.gbif.registry.service.collections.descriptors.DescriptorVocabularySynchronizer;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import com.zaxxer.hikari.HikariDataSource;
import org.gbif.registry.search.dataset.indexing.DatasetJsonConverter;
import org.gbif.registry.search.dataset.indexing.EsDatasetRealtimeIndexer;
import org.gbif.registry.search.dataset.indexing.es.EsClient;
import org.gbif.registry.search.dataset.indexing.es.EsConfiguration;
import org.gbif.registry.search.dataset.indexing.ws.GbifApiServiceConfig;
import org.gbif.registry.search.dataset.indexing.ws.GbifWsWrapperClient;
import org.gbif.registry.search.dataset.indexing.ws.JacksonObjectMapper;
import org.gbif.registry.service.DatasetCategoryService;
import org.gbif.registry.service.VocabularyConceptService;
import org.gbif.registry.service.WithMyBatis;
import org.gbif.registry.ws.client.DatasetClient;
import org.gbif.registry.ws.client.InstallationClient;
import org.gbif.registry.ws.client.NetworkClient;
import org.gbif.registry.ws.client.OrganizationClient;
import org.gbif.ws.client.ClientBuilder;
import org.gbif.ws.json.JacksonJsonObjectMapperProvider;
import org.gbif.vocabulary.client.ConceptClient;

import java.util.Date;

import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.MapPropertySource;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableMap;

public class SpringContextBuilder {

  private static EsClient.EsClientConfiguration toEsClientConfiguration(
      ElasticsearchConfig elasticsearchConfig) {
    EsClient.EsClientConfiguration esClientConfiguration = new EsClient.EsClientConfiguration();
    esClientConfiguration.setHosts(elasticsearchConfig.getHosts());
    esClientConfiguration.setConnectionRequestTimeOut(
        elasticsearchConfig.getConnectionRequestTimeOut());
    esClientConfiguration.setSocketTimeOut(elasticsearchConfig.getSocketTimeOut());
    esClientConfiguration.setConnectionTimeOut(elasticsearchConfig.getConnectionTimeOut());
    return esClientConfiguration;
  }

  public static ApplicationContext applicationContext(
      VocabularySynchronizerConfiguration configuration) {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();

    // Register configuration bean
    ctx.registerBean(
        "VocabularySynchronizerConfiguration", VocabularySynchronizerConfiguration.class, () -> configuration);

    // Register core components
    ctx.register(ApplicationConfig.class);
    ctx.register(MyBatisConfiguration.class);
    ctx.register(MybatisAutoConfiguration.class);
    ctx.register(EsConfiguration.class);
    ctx.register(EsClient.class);
    ctx.register(DatasetJsonConverter.class);
    ctx.register(EsDatasetRealtimeIndexer.class);
    ctx.register(JacksonObjectMapper.class);
    ctx.register(GbifApiServiceConfig.class);
    ctx.register(GbifWsWrapperClient.class);
    ctx.register(VocabularyConceptService.class);
    ctx.register(DescriptorVocabularySynchronizer.class);
    ctx.register(DatasetCategoryService.class);
    ctx.register(WithMyBatis.class);

    // Register database configuration
    ctx.registerBean(DbConfiguration.class, configuration::getDbConfig);

    // Register Elasticsearch client configurations
    ctx.registerBean(
        "registryEsClientConfig",
        EsClient.EsClientConfiguration.class,
        () -> toEsClientConfiguration(configuration.getElasticsearch()));

    ctx.registerBean(
        "esOccurrenceClientConfig",
        EsClient.EsClientConfiguration.class,
        () -> toEsClientConfiguration(configuration.getElasticsearch()));

    // Register ConceptClient bean
    ctx.registerBean("conceptClient", ConceptClient.class, () ->
        new ClientBuilder()
            .withObjectMapper(JacksonJsonObjectMapperProvider.getObjectMapperWithBuilderSupport()
                .registerModule(new JavaTimeModule()))
            .withUrl(configuration.getApiRootUrl())
            .build(ConceptClient.class));

    // Add properties to environment
    ctx.getEnvironment()
        .getPropertySources()
        .addLast(
            new MapPropertySource(
                "VocabularySynchronizerProperties",
                new ImmutableMap.Builder<String, Object>()
                    .put("api.root.url", configuration.getApiRootUrl())
                    .put("elasticsearch.registry.index", configuration.getElasticsearch().getIndex())
                    .put("elasticsearch.occurrence.index", "occurrence")
                    .put("elasticsearch.registry.enabled", "true")
                    .put("elasticsearch.occurrence.enabled", "false")
                    .put("spring.cloud.compatibility-verifier.enabled", "false")
                    .build()));

    ctx.refresh();
    ctx.start();
    return ctx;
  }

  /** Class to help with the loading and injection of */
  @SpringBootApplication(
      exclude = {
        DataSourceAutoConfiguration.class,
        LiquibaseAutoConfiguration.class,
        FreeMarkerAutoConfiguration.class
      })
  @EnableConfigurationProperties
  @MapperScan("org.gbif.registry.persistence.mapper")
  static class ApplicationConfig {

        @Bean
    public ClientBuilder clientBuilder(VocabularySynchronizerConfiguration configuration) {
      ClientBuilder clientBuilder = new ClientBuilder();

      ObjectMapper objectMapper =
          JacksonJsonObjectMapperProvider.getObjectMapperWithBuilderSupport();
      SimpleModule simpleModule = new SimpleModule();
      simpleModule.addDeserializer(Date.class, new CustomDateDeserializer());
      objectMapper.registerModule(simpleModule);

      clientBuilder.withObjectMapper(objectMapper);
      clientBuilder.withUrl(configuration.getApiRootUrl());
      return clientBuilder;
    }

    @Bean
    public InstallationService installationService(ClientBuilder clientBuilder) {
      return clientBuilder.build(InstallationClient.class);
    }

    @Bean
    public OrganizationService organizationService(ClientBuilder clientBuilder) {
      return clientBuilder.build(OrganizationClient.class);
    }

    @Bean
    public DatasetService datasetService(ClientBuilder clientBuilder) {
      return clientBuilder.build(DatasetClient.class);
    }

    @Bean
    public NetworkService networkService(ClientBuilder clientBuilder) {
      return clientBuilder.build(NetworkClient.class);
    }

    @Bean
    @Primary
    public DataSourceProperties dataSourceProperties(VocabularySynchronizerConfiguration configuration) {
      DataSourceProperties dataSourceProperties = new DataSourceProperties();
      dataSourceProperties.setPassword(configuration.getDbConfig().getPassword());
      dataSourceProperties.setUsername(configuration.getDbConfig().getUser());
      dataSourceProperties.setUrl(
          "jdbc:postgresql://"
              + configuration.getDbConfig().getServerName()
              + "/"
              + configuration.getDbConfig().getDatabaseName());
      dataSourceProperties.setGenerateUniqueName(true);
      return dataSourceProperties;
    }

    @Bean(name = "dataSource")
    @Primary
    public HikariDataSource dataSource(VocabularySynchronizerConfiguration configuration) {
      HikariDataSource hikariDataSource =
          dataSourceProperties(configuration)
              .initializeDataSourceBuilder()
              .type(HikariDataSource.class)
              .build();
      hikariDataSource.setMaximumPoolSize(configuration.getDbConfig().getMaximumPoolSize());
      hikariDataSource.setMinimumIdle(1);
      return hikariDataSource;
    }


    private static class CustomDateDeserializer extends DateDeserializers.DateDeserializer {
      @Override
      public Date deserialize(JsonParser p, DeserializationContext ctxt) throws java.io.IOException {
        return super.deserialize(p, ctxt);
      }
    }
  }
}
