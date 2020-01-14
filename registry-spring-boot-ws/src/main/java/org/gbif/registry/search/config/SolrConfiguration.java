package org.gbif.registry.search.config;

import org.apache.solr.client.solrj.SolrClient;
import org.gbif.common.search.solr.SolrConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SolrConfiguration {

  private final SolrConfig cfg;

  public SolrConfiguration(SolrDatasetProperties solrProperties) {
    cfg = new SolrConfig();
    cfg.setServerType(solrProperties.getServerType());
    cfg.setServerHome(solrProperties.getHome());
    cfg.setCollection(solrProperties.getCollection());
    // TODO: 10/01/2020 idField was not found in configuration files
//    cfg.setIdField();
    cfg.setDeleteOnExit(solrProperties.getDelete());
  }

  @Qualifier("datasetSolr")
  @Bean
  public SolrClient datasetSolr() {
    return cfg.buildSolr();
  }
}
