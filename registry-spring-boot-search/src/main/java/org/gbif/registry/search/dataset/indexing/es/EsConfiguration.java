package org.gbif.registry.search.dataset.indexing.es;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EsConfiguration {

  @Bean
  public RestHighLevelClient restHighLevelClient(@Value("${elasticsearch.hosts}") String hosts,
                                                 @Value("${elasticsearch.connectionTimeOut}") int connectionTimeOut,
                                                 @Value("${elasticsearch.socketTimeOut}") int socketTimeOut,
                                                 @Value("${elasticsearch.connectionRequestTimeOut}") int connectionRequestTimeOut,
                                                 @Value("${elasticsearch.maxRetryTimeOut}") int maxRetryTimeOut) {
    return EsClient.provideEsClient(hosts.split(","), connectionTimeOut, socketTimeOut, connectionRequestTimeOut, maxRetryTimeOut);
  }
}
