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

import org.gbif.api.model.checklistbank.DatasetMetrics;
import org.gbif.api.model.checklistbank.search.NameUsageSearchParameter;
import org.gbif.api.model.checklistbank.search.NameUsageSearchRequest;
import org.gbif.api.model.checklistbank.search.NameUsageSearchResult;
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

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public interface GbifWsClient {

  void purge(Installation installation);

  void purge(Organization organization);

  PagingResponse<Dataset> listDatasets(PagingRequest pagingRequest);

  Installation getInstallation(String installationKey);

  PagingResponse<Dataset> getInstallationDatasets(
      String installationKey, PagingRequest pagingRequest);

  Organization getOrganization(String organizationKey);

  PagingResponse<Dataset> getOrganizationHostedDatasets(
      String organizationKey, PagingRequest pagingRequest);

  PagingResponse<Dataset> getOrganizationPublishedDataset(
      String organizationKey, PagingRequest pagingRequest);

  PagingResponse<Dataset> getNetworkDatasets(String networkKey, PagingRequest pagingRequest);

  InputStream getMetadataDocument(UUID datasetKey);

  Long getDatasetRecordCount(String datasetKey);

  Long getOccurrenceRecordCount();

  DatasetMetrics getDatasetSpeciesMetrics(String datasetKey);

  SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> speciesSearch(
      NameUsageSearchRequest searchRequest);

  SearchResponse<Occurrence, OccurrenceSearchParameter> occurrenceSearch(
      OccurrenceSearchRequest searchRequest);

  List<Network> getNetworks(UUID datasetKey);
}
