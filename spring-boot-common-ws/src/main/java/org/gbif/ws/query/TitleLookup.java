package org.gbif.ws.query;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.registry.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

public class TitleLookup {

  private static final Logger LOG = LoggerFactory.getLogger(TitleLookup.class);

  private final String apiRootUrl;
  private final RestTemplate restTemplate;

  public TitleLookup(RestTemplate restTemplate) {
    this.apiRootUrl = "";// TODO: 03/09/2019 get from a property?
    this.restTemplate = restTemplate;
  }

  public TitleLookup(String apiRootUrl) {
    this.apiRootUrl = apiRootUrl;
    this.restTemplate = new RestTemplate();
  }

  public String getDatasetTitle(String datasetKey) {
    try {
      // TODO: 03/09/2019 add mixins
      // TODO: 03/09/2019 use special ObjectMapper (gbif-common-ws JacksonJsonContextResolver)

      String targetUrl = apiRootUrl + "/dataset/{key}";
      final HttpHeaders headers = new HttpHeaders();
      headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
      HttpEntity<Dataset> entity = new HttpEntity<>(headers);

      return restTemplate.exchange(targetUrl, HttpMethod.GET, entity, Dataset.class, datasetKey).getBody()
          .getTitle();
    } catch (RuntimeException var3) {
      LOG.error("Cannot lookup dataset title {}", datasetKey, var3);
      return datasetKey;
    }
  }

  public String getSpeciesName(String usageKey) {
    try {
      String targetUrl = apiRootUrl + "/species/{key}";
      final HttpHeaders headers = new HttpHeaders();
      headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
      HttpEntity<Dataset> entity = new HttpEntity<>(headers);

      return restTemplate.exchange(targetUrl, HttpMethod.GET, entity, NameUsage.class, usageKey).getBody()
          .getScientificName();
    } catch (RuntimeException var3) {
      LOG.error("Cannot lookup species title {}", usageKey, var3);
      return usageKey;
    }
  }
}
