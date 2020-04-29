package org.gbif.registry.search.test;

import org.gbif.registry.search.dataset.indexing.es.EsClient;
import org.gbif.registry.ws.it.DatasetIT;

import java.nio.file.Paths;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DatasetElasticsearchConfiguration {

  @Bean("datasetElasticCluster")
  public EsManageServer esManageServer() {
    try {
      return new EsManageServer(Paths.get(DatasetIT.class.getClassLoader().getResource("dataset-es-mapping.json").getPath()),
                                "dataset",
                                "dataset");
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  @Bean(name = "esOccurrenceClientConfig")
  public EsClient.EsClientConfiguration occurrenceRestHighLevelClient(EsManageServer esManageServer) {
    EsClient.EsClientConfiguration esClientConfiguration = new EsClient.EsClientConfiguration();
    esClientConfiguration.setMaxRetryTimeOut(6000);
    esClientConfiguration.setSocketTimeOut(6000);
    esClientConfiguration.setConnectionRequestTimeOut(6000);
    esClientConfiguration.setConnectionRequestTimeOut(6000);
    esClientConfiguration.setHosts(esManageServer.getServerAddress());
    return esClientConfiguration;
  }

  @Bean(name = "registryEsClientConfig")
  @Primary
  public EsClient.EsClientConfiguration registryRestHighLevelClient(EsManageServer esManageServer) {
    return occurrenceRestHighLevelClient(esManageServer);
  }
}
