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
package org.gbif.registry.doi;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.model.registry.Organization;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.metadata.datacite.RelatedIdentifierType;
import org.gbif.doi.metadata.datacite.RelationType;
import org.gbif.occurrence.query.TitleLookupService;
import org.gbif.registry.doi.converter.DatasetConverter;
import org.gbif.registry.doi.converter.DerivedDatasetConverter;
import org.gbif.registry.doi.converter.DownloadConverter;
import org.gbif.registry.domain.ws.DerivedDataset;
import org.gbif.registry.domain.ws.DerivedDatasetUsage;
import org.gbif.registry.persistence.mapper.DatasetOccurrenceDownloadMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;

import java.util.List;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

@Service
public class DataCiteMetadataBuilderServiceImpl implements DataCiteMetadataBuilderService {

  // Page size to iterate over dataset usages
  private static final int USAGES_PAGE_SIZE = 400;

  private final String apiRoot;
  private final OrganizationMapper organizationMapper;
  private final DatasetOccurrenceDownloadMapper datasetOccurrenceDownloadMapper;
  private final TitleLookupService titleLookupService;

  public DataCiteMetadataBuilderServiceImpl(
      @Value("${api.root.url}") String apiRoot,
      OrganizationMapper organizationMapper,
      DatasetOccurrenceDownloadMapper datasetOccurrenceDownloadMapper,
      TitleLookupService titleLookupService) {
    this.apiRoot = apiRoot;
    this.organizationMapper = organizationMapper;
    this.datasetOccurrenceDownloadMapper = datasetOccurrenceDownloadMapper;
    this.titleLookupService = titleLookupService;
  }

  @Override
  public DataCiteMetadata buildMetadata(
      DerivedDataset derivedDataset, List<DerivedDatasetUsage> derivedDatasetUsages) {
    return DerivedDatasetConverter.convert(derivedDataset, derivedDatasetUsages);
  }

  @Override
  public DataCiteMetadata buildMetadata(Download download, GbifUser user) {
    List<DatasetOccurrenceDownloadUsage> response = null;
    List<DatasetOccurrenceDownloadUsage> usages = Lists.newArrayList();
    PagingRequest pagingRequest = new PagingRequest(0, USAGES_PAGE_SIZE);

    while (response == null || !response.isEmpty()) {
      response =
          datasetOccurrenceDownloadMapper.listByDownload(
              download.getKey(), null, null, null, pagingRequest);
      usages.addAll(response);
      pagingRequest.nextPage();
    }

    return DownloadConverter.convert(download, user, usages, titleLookupService, apiRoot);
  }

  @Override
  public DataCiteMetadata buildMetadata(Dataset dataset) {
    return buildMetadata(dataset, null, null);
  }

  @Override
  public DataCiteMetadata buildMetadata(
      Dataset dataset, @Nullable DOI related, @Nullable RelationType relationType) {
    Organization publisher = organizationMapper.get(dataset.getPublishingOrganizationKey());
    DataCiteMetadata m = DatasetConverter.convert(dataset, publisher);
    // add previous relationship
    if (related != null) {
      m.getRelatedIdentifiers()
          .getRelatedIdentifier()
          .add(
              DataCiteMetadata.RelatedIdentifiers.RelatedIdentifier.builder()
                  .withRelationType(relationType)
                  .withValue(related.getDoiName())
                  .withRelatedIdentifierType(RelatedIdentifierType.DOI)
                  .build());
    }
    return m;
  }
}
