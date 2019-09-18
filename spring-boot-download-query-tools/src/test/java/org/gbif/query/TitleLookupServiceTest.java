package org.gbif.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.registry.Dataset;
import org.gbif.ws.mixin.LicenseMixin;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TitleLookupServiceTest {

  private static final String GBIF_API_ROOT_URL = "http://api.gbif.org/v1/";

  @Test
  public void testLookup() {
    RestTemplate rt = mock(RestTemplate.class);

    NameUsage abies = new NameUsage();
    abies.setScientificName("Abies alba Mill.");
    Dataset pontaurus = new Dataset();
    pontaurus.setTitle("PonTaurus");

    when(rt.exchange(any(String.class), any(HttpMethod.class), any(HttpEntity.class), eq(NameUsage.class), any(String.class)))
        .thenReturn(ResponseEntity.ok(abies));
    when(rt.exchange(any(String.class), any(HttpMethod.class), any(HttpEntity.class), eq(Dataset.class), any(String.class)))
        .thenReturn(ResponseEntity.ok(pontaurus));

    TitleLookupService tl = new TitleLookupServiceImpl(GBIF_API_ROOT_URL, rt);

    assertEquals("Abies alba Mill.", tl.getSpeciesName("4231"));
    assertEquals("PonTaurus", tl.getDatasetTitle(UUID.randomUUID().toString()));
  }

  @Test
  public void integrationTestLookup() {
    TitleLookupService tl = new TitleLookupServiceImpl(GBIF_API_ROOT_URL, titleLookupRestTemplate());
    assertEquals("Aves", tl.getSpeciesName("212"));
    assertEquals("GBIF Backbone Taxonomy", tl.getDatasetTitle(Constants.NUB_DATASET_KEY.toString()));
    assertEquals("EOD - eBird Observation Dataset", tl.getDatasetTitle("4fa7b334-ce0d-4e88-aaae-2e0c138d049e"));
  }

  private ObjectMapper titleLookupObjectMapper() {
    final ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    objectMapper.addMixIn(Dataset.class, LicenseMixin.class);

    return objectMapper;
  }

  private RestTemplate titleLookupRestTemplate() {
    final RestTemplate restTemplate = new RestTemplate();
    MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
    converter.setObjectMapper(titleLookupObjectMapper());
    restTemplate.getMessageConverters().add(0, converter);

    return restTemplate;
  }

}