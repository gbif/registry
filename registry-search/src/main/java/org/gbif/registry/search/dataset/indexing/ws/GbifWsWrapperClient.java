/*
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import org.gbif.api.service.registry.NetworkService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.metrics.ws.client.CubeWsClient;
import org.gbif.occurrence.ws.client.OccurrenceWsSearchClient;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;


import org.gbif.registry.search.dataset.indexing.ws.taxon.TaxonApiClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;

/** Retrofit {@link GbifApiService} client. */
@Component
@Lazy
public class GbifWsWrapperClient implements GbifWsClient {

  private static final Logger LOG = LoggerFactory.getLogger(GbifWsWrapperClient.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

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

  private final InstallationService installationService;
  private final OrganizationService organizationService;
  private final DatasetService datasetService;
  private final NetworkService networkService;
  private final OccurrenceWsSearchClient occurrenceWsSearchClient;
  private final TaxonApiClient taxonApiClient;
  private final CubeWsClient cubeWsClient;


  /**
   * Factory method, only need the api base url.
   *
   * @param apiBaseUrl GBIF Api base url, for example: https://api.gbif-dev.orf/v1/ .
   */
  @Autowired
  public GbifWsWrapperClient(
      InstallationService installationService,
      OrganizationService organizationService,
      DatasetService datasetService,
      NetworkService networkService,
      OccurrenceWsSearchClient occurrenceWsSearchClient,
      CubeWsClient cubeWsClient,
      TaxonApiClient taxonApiClient) {
    this.installationService = installationService;
    this.organizationService = organizationService;
    this.datasetService = datasetService;
    this.networkService = networkService;
    this.occurrenceWsSearchClient = occurrenceWsSearchClient;
    this.taxonApiClient = taxonApiClient;
    this.cubeWsClient = cubeWsClient;
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
  public PagingResponse<Dataset> getNetworkDatasets(
      String networkKey, PagingRequest pagingRequest) {
    return networkService.listConstituents(UUID.fromString(networkKey), pagingRequest);
  }

  @Override
  public InputStream getMetadataDocument(UUID datasetKey) {
    return datasetService.getMetadataDocument(datasetKey);
  }

  @Override
  public Long getDatasetRecordCount(String datasetKey) {
    LinkedMultiValueMap<String,String> params = new LinkedMultiValueMap<>();
    params.add("datasetKey", datasetKey);
    return cubeWsClient.get(params);
  }

  @Override
  public Long getOccurrenceRecordCount() {
    return cubeWsClient.get(new LinkedMultiValueMap<>());
  }

  @Override
  public Long getChecklistMetricsNameCount(String datasetKey) {
    try {
      JsonNode response = taxonApiClient.getMetrics(UUID.fromString(datasetKey));
      if (response != null) {
        JsonNode nameCountNode = response.get("nameCount");
        return nameCountNode != null ? nameCountNode.asLong() : null;
      }
    } catch (Exception e) {
      LOG.warn("Failed to get checklist metrics for dataset {}", datasetKey, e);
    }
    return null;
  }

  @Override
  public Long taxonSearchCount() {
    try {
      JsonNode response = taxonApiClient.search(MAPPER.createObjectNode());
      if (response != null) {
        JsonNode countNode = response.get("count");
        return countNode != null ? countNode.asLong() : null;
      }
    } catch (Exception e) {
      LOG.warn("Failed to get taxon search count", e);
    }
    return null;
  }

  @Override
  public SearchResponse<Occurrence, OccurrenceSearchParameter> occurrenceSearch(
      OccurrenceSearchRequest searchRequest) {
    return occurrenceWsSearchClient.search(searchRequest);
  }

  @Override
  public List<Network> getNetworks(UUID datasetKey) {
    return datasetService.listNetworks(datasetKey);
  }
}
