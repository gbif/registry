package org.gbif.registry.test;

import org.gbif.api.model.checklistbank.DatasetMetrics;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.search.NameUsageSearchParameter;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.model.occurrence.Occurrence;
import org.gbif.api.model.occurrence.search.OccurrenceSearchParameter;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.events.VarnishPurgeListener;
import org.gbif.registry.search.dataset.indexing.ws.GbifApiService;
import org.gbif.registry.search.dataset.indexing.ws.GbifWsClient;
import org.gbif.registry.search.dataset.indexing.ws.GbifWsRetrofitClient;

import java.util.Map;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import retrofit2.Call;
import retrofit2.mock.Calls;

@Configuration(proxyBeanMethods = false)
public class TestConfiguration {

  @Bean
  @Primary
  public GbifWsClient gbifWsClient(
    InstallationService installationService,
    OrganizationService organizationService,
    DatasetService datasetService) {
    return new GbifWsRetrofitClient(gbifApiService(), installationService, organizationService, datasetService);
  }


  public GbifApiService gbifApiService() {
    return new GbifApiService() {
      @Override
      public Call<Long> getDatasetRecordCount(String datasetKey) {
        return Calls.response(1L);
      }

      @Override
      public Call<Long> getOccurrenceRecordCount() {
        return Calls.response(1L);
      }

      @Override
      public Call<DatasetMetrics> getDatasetSpeciesMetrics(String datasetKey) {
        return Calls.response(new DatasetMetrics());
      }

      @Override
      public Call<SearchResponse<NameUsage, NameUsageSearchParameter>> speciesSearch(
        Map<String, Object> options
      ) {
        return Calls.response(new SearchResponse<>());
      }

      @Override
      public Call<SearchResponse<Occurrence, OccurrenceSearchParameter>> occurrenceSearch(
        Map<String, Object> options
      ) {
        return Calls.response(new SearchResponse<>());
      }
    };
  }

  @Bean
  @Primary
  VarnishPurgeListener varnishPurgeListener() {
    return Mockito.mock(VarnishPurgeListener.class);
  }
}
