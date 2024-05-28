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
package org.gbif.registry.cli.datasetindex;

import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NetworkService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.cli.datasetindex.batchindexer.DatasetBatchIndexer;
import org.gbif.registry.cli.datasetindex.batchindexer.DatasetBatchIndexerConfiguration;
import org.gbif.registry.cli.datasetindex.indexupdater.DatasetIndexUpdaterConfiguration;
import org.gbif.registry.pipelines.issues.GithubApiClient;
import org.gbif.registry.search.dataset.indexing.DatasetJsonConverter;
import org.gbif.registry.search.dataset.indexing.EsDatasetRealtimeIndexer;
import org.gbif.registry.search.dataset.indexing.checklistbank.ChecklistbankPersistenceServiceImpl;
import org.gbif.registry.search.dataset.indexing.es.EsClient;
import org.gbif.registry.search.dataset.indexing.es.EsConfiguration;
import org.gbif.registry.search.dataset.indexing.ws.GbifApiServiceConfig;
import org.gbif.registry.search.dataset.indexing.ws.GbifWsWrapperClient;
import org.gbif.registry.search.dataset.indexing.ws.JacksonObjectMapper;
import org.gbif.registry.ws.client.DatasetClient;
import org.gbif.registry.ws.client.InstallationClient;
import org.gbif.registry.ws.client.NetworkClient;
import org.gbif.registry.ws.client.OrganizationClient;
import org.gbif.ws.client.ClientBuilder;
import org.gbif.ws.json.JacksonJsonObjectMapperProvider;

import java.io.IOException;
import java.util.Date;

import org.springframework.boot.actuate.autoconfigure.elasticsearch.ElasticSearchRestHealthContributorAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceInitializationMode;
import org.springframework.cloud.netflix.archaius.ArchaiusAutoConfiguration;
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
import com.google.common.collect.ImmutableMap;
import com.zaxxer.hikari.HikariDataSource;

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
      DatasetBatchIndexerConfiguration configuration) {
    return commonContext(configuration);
  }

  public static ApplicationContext applicationContext(
      DatasetIndexUpdaterConfiguration configuration) {
    AnnotationConfigApplicationContext ctx = commonContext(configuration);
    ctx.register(EsDatasetRealtimeIndexer.class);
    return ctx;
  }

  private static AnnotationConfigApplicationContext commonContext(
      DatasetIndexConfiguration configuration) {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.registerBean(
        "DatasetIndexConfiguration", DatasetIndexConfiguration.class, () -> configuration);
    ctx.register(DatasetBatchIndexer.class);
    ctx.register(GbifApiServiceConfig.class);
    ctx.register(GbifWsWrapperClient.class);
    ctx.register(JacksonObjectMapper.class);
    ctx.register(EsConfiguration.class);
    ctx.register(EsClient.class);
    ctx.register(DatasetJsonConverter.class);

    if (configuration.isIndexClb()) {
      ctx.register(ChecklistbankPersistenceServiceImpl.class);
    }

    ctx.registerBean(
        "registryEsClientConfig",
        EsClient.EsClientConfiguration.class,
        () -> toEsClientConfiguration(configuration.getDatasetEs()));

    ctx.registerBean(
        "esOccurrenceClientConfig",
        EsClient.EsClientConfiguration.class,
        () -> toEsClientConfiguration(configuration.getOccurrenceEs()));

    ctx.register(ApplicationConfig.class);

    ctx.getEnvironment()
        .getPropertySources()
        .addLast(
            new MapPropertySource(
                "ManagedProperties",
                new ImmutableMap.Builder<String, Object>()
                    .put("api.root.url", configuration.getApiRootUrl())
                    .put(
                        "elasticsearch.occurrence.index",
                        configuration.getOccurrenceEs().getAlias())
                    .put("elasticsearch.registry.index", configuration.getDatasetEs().getAlias())
                    .put(
                        "indexing.datasource.checklistbank.url",
                        "jdbc:postgresql://"
                            + configuration.getClbDb().getServerName()
                            + " /"
                            + configuration.getClbDb().getDatabaseName())
                    .put(
                        "indexing.datasource.checklistbank.username",
                        configuration.getClbDb().getUser())
                    .put(
                        "indexing.datasource.checklistbank.password",
                        configuration.getClbDb().getPassword())
                    .put(
                        "indexing.datasource.checklistbank.hikari.maxPoolSize",
                        configuration.getClbDb().getMaximumPoolSize())
                    .put("indexing.datasource.checklistbank.hikari.minimumIdle", 1)
                    .put(
                        "indexing.datasource.checklistbank.hikari.connectionTimeout",
                        configuration.getClbDb().getConnectionTimeout())
                    .put("indexing.stopAfter", configuration.getStopAfter())
                    .put("indexing.pageSize", configuration.getPageSize())
                    .build()));
    ctx.refresh();
    ctx.start();
    return ctx;
  }

  /** Class to help with the loading and injection of */
  @SpringBootApplication(
      exclude = {
        ElasticsearchRestClientAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        LiquibaseAutoConfiguration.class,
        FreeMarkerAutoConfiguration.class,
        ArchaiusAutoConfiguration.class
      })
  @EnableConfigurationProperties
  static class ApplicationConfig {

    @Bean
    @Primary
    public DataSourceProperties clbDataSourceProperties(DatasetIndexConfiguration configuration) {
      DataSourceProperties dataSourceProperties = new DataSourceProperties();
      dataSourceProperties.setPassword(configuration.getClbDb().getPassword());
      dataSourceProperties.setUsername(configuration.getClbDb().getUser());
      dataSourceProperties.setUrl(
          "jdbc:postgresql://"
              + configuration.getClbDb().getServerName()
              + "/"
              + configuration.getClbDb().getDatabaseName());
      dataSourceProperties.setInitializationMode(DataSourceInitializationMode.ALWAYS);
      dataSourceProperties.setGenerateUniqueName(true);
      return dataSourceProperties;
    }

    @Bean(name = "clb_datasource")
    @Primary
    public HikariDataSource clbDataSource(DatasetIndexConfiguration configuration) {
      HikariDataSource hikariDataSource =
          clbDataSourceProperties(configuration)
              .initializeDataSourceBuilder()
              .type(HikariDataSource.class)
              .build();
      hikariDataSource.setMaximumPoolSize(configuration.getClbDb().getMaximumPoolSize());
      hikariDataSource.setMinimumIdle(1);
      return hikariDataSource;
    }

    @Bean
    public ClientBuilder clientBuilder(DatasetIndexConfiguration configuration) {
      ClientBuilder clientBuilder = new ClientBuilder();

      ObjectMapper objectMapper = JacksonJsonObjectMapperProvider.getObjectMapperWithBuilderSupport();
      SimpleModule simpleModule = new SimpleModule();
      simpleModule.addDeserializer(Date.class, new CustomDateDeserializer());
      objectMapper.registerModule(simpleModule);

      clientBuilder.withObjectMapper(
        objectMapper);
      clientBuilder.withUrl(
          configuration.getRegistryWsUrl() != null
              ? configuration.getRegistryWsUrl()
              : configuration.getApiRootUrl());
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
    public GithubApiClient githubApiClient(ClientBuilder clientBuilder){
      return clientBuilder.build(GithubApiClient.class);
    }
  }

  private static class CustomDateDeserializer extends DateDeserializers.DateDeserializer {

    @Override
    public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      try {
        return super.deserialize(p, ctxt);
      } catch (Exception ex) {
        return null;
      }
    }
  }
}
