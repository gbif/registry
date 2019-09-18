package org.gbif.query;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.registry.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class TitleLookupServiceImpl implements TitleLookupService {

  private static final Logger LOG = LoggerFactory.getLogger(TitleLookupServiceImpl.class);

  private final String apiRootUrl;
  private final RestTemplate restTemplate;

  public TitleLookupServiceImpl(@Value("${api.root.url}") String apiRootUrl,
                                @Qualifier("titleLookupRestTemplate") RestTemplate restTemplate) {
    this.apiRootUrl = apiRootUrl;
    this.restTemplate = restTemplate;
  }

  @Override
  public String getDatasetTitle(String datasetKey) {
    try {
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

  @Override
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
