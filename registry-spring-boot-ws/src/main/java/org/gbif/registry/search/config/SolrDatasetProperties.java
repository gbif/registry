package org.gbif.registry.search.config;

import org.gbif.common.search.solr.SolrServerType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "solr.dataset")
public class SolrDatasetProperties {

  private SolrServerType serverType;
  private String home;
  private String collection;
  private Boolean delete;

  public SolrServerType getServerType() {
    return serverType;
  }

  public void setServerType(SolrServerType serverType) {
    this.serverType = serverType;
  }

  public String getHome() {
    return home;
  }

  public void setHome(String home) {
    this.home = home;
  }

  public String getCollection() {
    return collection;
  }

  public void setCollection(String collection) {
    this.collection = collection;
  }

  public Boolean getDelete() {
    return delete;
  }

  public void setDelete(Boolean delete) {
    this.delete = delete;
  }
}
