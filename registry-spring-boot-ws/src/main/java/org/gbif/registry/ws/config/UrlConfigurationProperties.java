package org.gbif.registry.ws.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "url")
public class UrlConfigurationProperties {

  private String api;

  private String portal;

  private String occurrenceApi;

  private String directoryApi;

  private String grscicollPortal;

  public String getApi() {
    return api;
  }

  public void setApi(String api) {
    this.api = api;
  }

  public String getPortal() {
    return portal;
  }

  public void setPortal(String portal) {
    this.portal = portal;
  }

  public String getOccurrenceApi() {
    return occurrenceApi;
  }

  public void setOccurrenceApi(String occurrenceApi) {
    this.occurrenceApi = occurrenceApi;
  }

  public String getDirectoryApi() {
    return directoryApi;
  }

  public void setDirectoryApi(String directoryApi) {
    this.directoryApi = directoryApi;
  }

  public String getGrscicollPortal() {
    return grscicollPortal;
  }

  public void setGrscicollPortal(String grscicollPortal) {
    this.grscicollPortal = grscicollPortal;
  }
}
