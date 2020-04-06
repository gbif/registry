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
package org.gbif.registry.doi.handler;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.OccurrenceDownloadService;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.metadata.datacite.RelatedIdentifierType;
import org.gbif.doi.metadata.datacite.RelationType;
import org.gbif.doi.service.InvalidMetadataException;
import org.gbif.occurrence.query.TitleLookupService;
import org.gbif.registry.doi.config.DoiConfigurationProperties;
import org.gbif.registry.doi.converter.DatasetConverter;
import org.gbif.registry.doi.converter.DownloadConverter;
import org.gbif.registry.doi.generator.DoiGenerator;
import org.gbif.registry.persistence.mapper.OrganizationMapper;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/** GBIF Business logic for DOI handling with DataCite in the Registry. */
@Service
public class GbifDataCiteDoiHandlerStrategy implements DataCiteDoiHandlerStrategy {

  // DOI logging marker
  private static final Logger LOG = LoggerFactory.getLogger(GbifDataCiteDoiHandlerStrategy.class);
  private static final Marker DOI_SMTP = MarkerFactory.getMarker("DOI_SMTP");

  // Page size to iterate over dataset usages
  private static final int USAGES_PAGE_SIZE = 400;

  private static final EnumSet<Download.Status> FAILED_STATES =
      EnumSet.of(Download.Status.KILLED, Download.Status.CANCELLED, Download.Status.FAILED);

  private final DoiGenerator doiGenerator;
  private final OrganizationMapper organizationMapper;
  private final OccurrenceDownloadService occurrenceDownloadService;
  private final TitleLookupService titleLookupService;

  // Used to exclude constituents of selected datasets (e.g. GBIF Backbone Taxonomy)
  private final List<UUID> parentDatasetExcludeList;

  public GbifDataCiteDoiHandlerStrategy(
      DoiGenerator doiGenerator,
      OrganizationMapper organizationMapper,
      @Qualifier("occurrenceDownloadResource") OccurrenceDownloadService occurrenceDownloadService,
      TitleLookupService titleLookupService,
      DoiConfigurationProperties doiConfigProperties) {
    this.doiGenerator = doiGenerator;
    this.organizationMapper = organizationMapper;
    this.occurrenceDownloadService = occurrenceDownloadService;
    this.titleLookupService = titleLookupService;
    this.parentDatasetExcludeList = doiConfigProperties.getDatasetParentExcludeList();
  }

  @Override
  public boolean isUsingMyPrefix(DOI doi) {
    return doiGenerator.isGbif(doi);
  }

  @Override
  public DataCiteMetadata buildMetadata(Download download, GbifUser user) {
    List<DatasetOccurrenceDownloadUsage> response = null;
    List<DatasetOccurrenceDownloadUsage> usages = Lists.newArrayList();
    PagingRequest pagingRequest = new PagingRequest(0, USAGES_PAGE_SIZE);

    while (response == null || !response.isEmpty()) {
      response =
          occurrenceDownloadService
              .listDatasetUsages(download.getKey(), pagingRequest)
              .getResults();
      usages.addAll(response);
      pagingRequest.nextPage();
    }

    return DownloadConverter.convert(download, user, usages, titleLookupService);
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

  @Override
  public void datasetChanged(Dataset dataset, @Nullable DOI previousDoi) {
    // When configured, we can skip the DOI logic for some dataset when the getParentDatasetKey is
    // in the
    // parentDatasetExcludeList
    if (dataset.getParentDatasetKey() != null
        && parentDatasetExcludeList.contains(dataset.getParentDatasetKey())) {
      LOG.info(
          "Dataset {} parentDatasetKey is part of the ignore list: ignoring DOI related action(s). ",
          dataset.getKey());
      return;
    }

    // if the old doi was a GBIF one and the new one is different, update its metadata with a
    // version relationship
    if (previousDoi != null
        && doiGenerator.isGbif(previousDoi)
        && !dataset.getDoi().equals(previousDoi)) {
      scheduleDatasetRegistration(
          previousDoi,
          buildMetadata(dataset, dataset.getDoi(), RelationType.IS_PREVIOUS_VERSION_OF),
          dataset.getKey());
    }
    // if the current doi was a GBIF DOI finally schedule a metadata update in datacite
    if (doiGenerator.isGbif(dataset.getDoi())) {
      // if DOIs changed establish relationship
      DataCiteMetadata metadata;
      // to get the latest timestamps we need to read a new copy of the dataset
      if (previousDoi == null || dataset.getDoi().equals(previousDoi)) {
        metadata = buildMetadata(dataset);
      } else {
        metadata = buildMetadata(dataset, previousDoi, RelationType.IS_NEW_VERSION_OF);
      }
      scheduleDatasetRegistration(dataset.getDoi(), metadata, dataset.getKey());
    }
  }

  /**
   * Updates the download DOI according to the download status. If the download succeeded its DOI is
   * registered; if the download status is one the FAILED_STATES the DOI is removed, otherwise does
   * nothing.
   */
  @Override
  public void downloadChanged(Download download, Download previousDownload, GbifUser user) {
    Preconditions.checkNotNull(download, "download can not be null");

    if (download.isAvailable()
        && (previousDownload == null
            || (previousDownload.getStatus() != Download.Status.SUCCEEDED
                && previousDownload.getStatus() != Download.Status.FILE_ERASED))) {
      try {
        doiGenerator.registerDownload(
            download.getDoi(), buildMetadata(download, user), download.getKey());
      } catch (Exception error) {
        LOG.error(
            DOI_SMTP,
            "Invalid metadata for download {} with doi {} ",
            download.getKey(),
            download.getDoi(),
            error);
      }
    } else if (FAILED_STATES.contains(download.getStatus())) {
      doiGenerator.delete(download.getDoi());
    }
  }

  @Override
  public void scheduleDatasetRegistration(DOI doi, DataCiteMetadata metadata, UUID datasetKey) {
    try {
      doiGenerator.registerDataset(doi, metadata, datasetKey);
    } catch (InvalidMetadataException e) {
      LOG.error(DOI_SMTP, "Failed to schedule DOI update for {}, dataset {}", doi, datasetKey, e);
      doiGenerator.failed(doi, e);
    }
  }
}
