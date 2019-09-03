package org.gbif.ws.query;

import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.registry.Dataset;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TitleLookupTest {

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

    TitleLookup tl = new TitleLookup(rt);

    assertEquals("Abies alba Mill.", tl.getSpeciesName("4231"));
    assertEquals("PonTaurus", tl.getDatasetTitle(UUID.randomUUID().toString()));
  }

  @Test
  public void integrationTestLookup() {
    TitleLookup tl = new TitleLookup("http://api.gbif.org/v1/");
    assertEquals("Aves", tl.getSpeciesName("212"));
    assertEquals("GBIF Backbone Taxonomy", tl.getDatasetTitle(Constants.NUB_DATASET_KEY.toString()));
    assertEquals("EOD - eBird Observation Dataset", tl.getDatasetTitle("4fa7b334-ce0d-4e88-aaae-2e0c138d049e"));
  }

}