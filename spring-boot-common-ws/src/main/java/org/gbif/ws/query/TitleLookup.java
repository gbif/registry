package org.gbif.ws.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.registry.Dataset;
import org.gbif.ws.mixin.LicenseMixin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

public class TitleLookup {

  private static final Logger LOG = LoggerFactory.getLogger(TitleLookup.class);

  private final String apiRootUrl;
  private final RestTemplate restTemplate;

  public TitleLookup(String apiRootUrl, RestTemplate restTemplate) {
    this.apiRootUrl = apiRootUrl;
    this.restTemplate = restTemplate;

    final ObjectMapper objectMapper = configureObjectMapper();

    MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
    converter.setObjectMapper(objectMapper);
    this.restTemplate.getMessageConverters().add(0, converter);
  }

  public TitleLookup(String apiRootUrl) {
    this.apiRootUrl = apiRootUrl;
    this.restTemplate = new RestTemplate();

    final ObjectMapper objectMapper = configureObjectMapper();

    MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
    converter.setObjectMapper(objectMapper);
    restTemplate.getMessageConverters().add(0, converter);
  }

  private ObjectMapper configureObjectMapper() {
    final ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    objectMapper.addMixIn(Dataset.class, LicenseMixin.class);

    return objectMapper;
  }

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
