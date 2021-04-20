/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.search.dataset.indexing.ws;

import org.gbif.api.model.checklistbank.DatasetMetrics;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.search.NameUsageSearchParameter;
import org.gbif.api.model.checklistbank.search.NameUsageSearchRequest;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.model.occurrence.Occurrence;
import org.gbif.api.model.occurrence.search.OccurrenceSearchParameter;
import org.gbif.api.model.occurrence.search.OccurrenceSearchRequest;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Network;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.OrganizationService;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import lombok.SneakyThrows;
import retrofit2.Call;
import retrofit2.Response;

import static org.gbif.registry.search.dataset.indexing.ws.SearchParameterProvider.getParameterFromFacetedRequest;

/** Retrofit {@link GbifApiService} client. */
@Component
@Lazy
public class GbifWsRetrofitClient implements GbifWsClient {

  // Uses a cache for installations to avoid too many external calls
  Cache<String, Installation> installationCache =
      Cache2kBuilder.of(String.class, Installation.class)
          .eternal(true)
          .disableStatistics(true)
          .permitNullValues(true)
          .loader(this::loadInstallation)
          .build();

  // Uses a cache for organizations to avoid too many external calls
  Cache<String, Organization> organizationCache =
      Cache2kBuilder.of(String.class, Organization.class)
          .eternal(true)
          .disableStatistics(true)
          .permitNullValues(true)
          .loader(this::loadOrganization)
          .build();

  private final GbifApiService gbifApiService;

  private final InstallationService installationService;
  private final OrganizationService organizationService;
  private final DatasetService datasetService;

  /**
   * Factory method, only need the api base url.
   *
   * @param apiBaseUrl GBIF Api base url, for example: https://api.gbif-dev.orf/v1/ .
   */
  @Autowired
  public GbifWsRetrofitClient(
      GbifApiService gbifApiService,
      InstallationService installationService,
      OrganizationService organizationService,
      DatasetService datasetService) {
    this.gbifApiService = gbifApiService;
    this.installationService = installationService;
    this.organizationService = organizationService;
    this.datasetService = datasetService;
  }

  private Map<String, String> toQueryMap(PagingRequest pagingRequest) {
    Map<String, String> params = new HashMap<>();
    params.put("offset", Long.toString(pagingRequest.getOffset()));
    params.put("limit", Long.toString(pagingRequest.getLimit()));
    return params;
  }

  @Override
  public void purge(Installation installation) {
    installationCache.remove(installation.getKey().toString());
  }

  @Override
  public void purge(Organization organization) {
    organizationCache.remove(organization.getKey().toString());
  }

  @Override
  public PagingResponse<Dataset> listDatasets(PagingRequest pagingRequest) {
    return datasetService.list(pagingRequest);
  }


  @Override
  public Installation getInstallation(String installationKey) {
    return installationCache.get(installationKey);
  }

  private Installation loadInstallation(String installationKey) {
    return installationService.get(UUID.fromString(installationKey));
  }

  @Override
  public PagingResponse<Dataset> getInstallationDatasets(
      String installationKey, PagingRequest pagingRequest) {
    return installationService.getHostedDatasets(UUID.fromString(installationKey), pagingRequest);
  }

  @Override
  public Organization getOrganization(String organizationKey) {
    return organizationCache.get(organizationKey);
  }

  private Organization loadOrganization(String organizationKey) {
    return organizationService.get(UUID.fromString(organizationKey));
  }

  @Override
  public PagingResponse<Dataset> getOrganizationHostedDatasets(
      String organizationKey, PagingRequest pagingRequest) {
    return organizationService.hostedDatasets(UUID.fromString(organizationKey), pagingRequest);
  }

  @Override
  public PagingResponse<Dataset> getOrganizationPublishedDataset(
      String organizationKey, PagingRequest pagingRequest) {
    return organizationService.publishedDatasets(UUID.fromString(organizationKey), pagingRequest);
  }

  @Override
  public InputStream getMetadataDocument(UUID datasetKey) {
    return datasetService.getMetadataDocument(datasetKey);
  }

  @Override
  public Long getDatasetRecordCount(String datasetKey) {
    return syncCallWithResponse(gbifApiService.getDatasetRecordCount(datasetKey)).body();
  }

  @Override
  public Long getOccurrenceRecordCount() {
    return syncCallWithResponse(gbifApiService.getOccurrenceRecordCount()).body();
  }

  @Override
  public DatasetMetrics getDatasetSpeciesMetrics(String datasetKey) {
    return syncCallWithResponse(gbifApiService.getDatasetSpeciesMetrics(datasetKey)).body();
  }

  @Override
  public SearchResponse<NameUsage, NameUsageSearchParameter> speciesSearch(
      NameUsageSearchRequest searchRequest) {
    return syncCallWithResponse(
            gbifApiService.speciesSearch(getParameterFromFacetedRequest(searchRequest)))
        .body();
  }

  @Override
  public SearchResponse<Occurrence, OccurrenceSearchParameter> occurrenceSearch(
      OccurrenceSearchRequest searchRequest) {
    return syncCallWithResponse(
            gbifApiService.occurrenceSearch(getParameterFromFacetedRequest(searchRequest)))
        .body();
  }

  @Override
  public List<Network> getNetworks(UUID datasetKey) {
    return datasetService.listNetworks(datasetKey);
  }

  /**
   * Performs a synchronous call to {@link Call} instance.
   *
   * @param call to be executed
   * @param <T> content of the response object
   * @return {@link Response} with content, throws a {@link RuntimeException} when IOException was
   *     thrown from execute method
   */
  @SneakyThrows
  private static <T> Response<T> syncCallWithResponse(Call<T> call) {
    return call.execute();
  }
}
