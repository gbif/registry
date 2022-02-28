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
import org.gbif.api.model.common.DoiData;
import org.gbif.api.model.common.DoiStatus;
import org.gbif.api.model.registry.Dataset;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.metadata.datacite.RelationType;
import org.gbif.doi.service.InvalidMetadataException;
import org.gbif.registry.doi.config.DoiConfigurationProperties;
import org.gbif.registry.persistence.mapper.DoiMapper;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.stereotype.Service;

@Service
public class DatasetDoiDataCiteHandlingServiceImpl implements DatasetDoiDataCiteHandlingService {

  // DOI logging marker
  private static final Logger LOG =
      LoggerFactory.getLogger(DatasetDoiDataCiteHandlingServiceImpl.class);
  private static final Marker DOI_SMTP = MarkerFactory.getMarker("DOI_SMTP");

  private final DoiMapper doiMapper;
  private final DoiMessageManagingService doiMessageManagingService;
  private final DoiIssuingService doiIssuingService;
  private final DataCiteMetadataBuilderService metadataBuilderService;

  // Used to exclude constituents of selected datasets (e.g. GBIF Backbone Taxonomy)
  private final List<UUID> parentDatasetExcludeList;

  public DatasetDoiDataCiteHandlingServiceImpl(
      DoiMapper doiMapper,
      DoiMessageManagingService doiMessageManagingService,
      DoiIssuingService doiIssuingService,
      DataCiteMetadataBuilderService metadataBuilderService,
      DoiConfigurationProperties doiConfigProperties) {
    this.doiMapper = doiMapper;
    this.doiMessageManagingService = doiMessageManagingService;
    this.doiIssuingService = doiIssuingService;
    this.metadataBuilderService = metadataBuilderService;
    this.parentDatasetExcludeList = doiConfigProperties.getDatasetParentExcludeList();
  }

  @Override
  public void datasetChanged(Dataset dataset, @Nullable DOI previousDoi) {
    // When configured, we can skip the DOI logic for some dataset when the getParentDatasetKey is
    // in the parentDatasetExcludeList
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
        && doiIssuingService.isGbif(previousDoi)
        && !dataset.getDoi().equals(previousDoi)) {
      scheduleDatasetRegistration(
          previousDoi,
          metadataBuilderService.buildMetadata(
              dataset, dataset.getDoi(), RelationType.IS_PREVIOUS_VERSION_OF),
          dataset.getKey());
    }
    // if the current doi was a GBIF DOI finally schedule a metadata update in datacite
    if (doiIssuingService.isGbif(dataset.getDoi())) {
      // if DOIs changed establish relationship
      DataCiteMetadata metadata;
      // to get the latest timestamps we need to read a new copy of the dataset
      if (previousDoi == null || dataset.getDoi().equals(previousDoi)) {
        metadata = metadataBuilderService.buildMetadata(dataset);
      } else {
        metadata =
            metadataBuilderService.buildMetadata(
                dataset, previousDoi, RelationType.IS_NEW_VERSION_OF);
      }
      scheduleDatasetRegistration(dataset.getDoi(), metadata, dataset.getKey());
    }
  }

  @Override
  public void datasetDeleted(DOI doi) {
    doiMessageManagingService.delete(doi);
  }

  @Override
  public void scheduleDatasetRegistration(DOI doi, DataCiteMetadata metadata, UUID datasetKey) {
    try {
      doiMessageManagingService.registerDataset(doi, metadata, datasetKey);
    } catch (InvalidMetadataException e) {
      LOG.error(DOI_SMTP, "Failed to schedule DOI update for {}, dataset {}", doi, datasetKey, e);
      failed(doi, e);
    }
  }

  @Override
  public void scheduleDerivedDatasetRegistration(
      DOI doi, DataCiteMetadata metadata, URI target, Date registrationDate) {
    try {
      doiMessageManagingService.registerDerivedDataset(doi, metadata, target, registrationDate);
    } catch (InvalidMetadataException e) {
      LOG.error(DOI_SMTP, "Failed to schedule DOI update for {}", doi, e);
      failed(doi, e);
    }
  }

  private void failed(DOI doi, InvalidMetadataException e) {
    // Updates the doi table to FAILED status and uses the error stacktrace as the xml for
    // debugging.
    doiMapper.update(doi, new DoiData(DoiStatus.FAILED, null), ExceptionUtils.getStackTrace(e));
  }
}
