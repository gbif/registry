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
package org.gbif.registry.service;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.registry.doi.DataCiteMetadataBuilderService;
import org.gbif.registry.doi.DatasetDoiDataCiteHandlingService;
import org.gbif.registry.doi.DoiIssuingService;
import org.gbif.registry.doi.util.RegistryDoiUtils;
import org.gbif.registry.domain.ws.Citation;
import org.gbif.registry.domain.ws.CitationDatasetUsage;
import org.gbif.registry.persistence.mapper.CitationMapper;
import org.gbif.registry.persistence.mapper.DatasetMapper;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.common.collect.Iterators;

import static org.gbif.registry.service.util.ServiceUtils.pagingResponse;

@Service
public class RegistryCitationServiceImpl implements RegistryCitationService {

  private static final Logger LOG = LoggerFactory.getLogger(RegistryCitationServiceImpl.class);

  private static final ZoneId UTC = ZoneId.of("UTC");
  private static final DateTimeFormatter REGULAR_DATE_FORMAT =
      DateTimeFormatter.ofPattern("d MMMM yyyy");
  private static final int BATCH_SIZE = 5_000;

  private final DataCiteMetadataBuilderService metadataBuilderService;
  private final DoiIssuingService doiIssuingService;
  private final DatasetDoiDataCiteHandlingService datasetDoiDataCiteHandlingService;
  private final CitationMapper citationMapper;
  private final DatasetMapper datasetMapper;
  private final String citationText;

  public RegistryCitationServiceImpl(
      DataCiteMetadataBuilderService metadataBuilderService,
      DoiIssuingService doiIssuingService,
      DatasetDoiDataCiteHandlingService datasetDoiDataCiteHandlingService,
      CitationMapper citationMapper,
      DatasetMapper datasetMapper,
      @Value("${citation.text}") String citationText) {
    this.metadataBuilderService = metadataBuilderService;
    this.doiIssuingService = doiIssuingService;
    this.datasetDoiDataCiteHandlingService = datasetDoiDataCiteHandlingService;
    this.citationMapper = citationMapper;
    this.datasetMapper = datasetMapper;
    this.citationText = citationText;
  }

  @Override
  public String getCitationText(DOI citationDoi) {
    return get(citationDoi).getCitation();
  }

  @Override
  public Citation create(Citation citation, List<CitationDatasetUsage> citationDatasetUsages) {
    DOI doi = doiIssuingService.newDerivedDatasetDOI();

    citation.setDoi(doi);
    citation.setCitation(
        MessageFormat.format(citationText, LocalDate.now(UTC).format(REGULAR_DATE_FORMAT), doi));

    DataCiteMetadata metadata = metadataBuilderService.buildMetadata(citation);

    datasetDoiDataCiteHandlingService.scheduleDerivedDatasetRegistration(
        doi, metadata, citation.getTarget(), citation.getRegistrationDate());

    citationMapper.create(citation);
    Iterators.partition(citationDatasetUsages.iterator(), BATCH_SIZE)
        .forEachRemaining(batch -> citationMapper.addCitationDatasets(doi, batch));

    return citation;
  }

  @Override
  public Citation get(DOI citationDoi) {
    return citationMapper.get(citationDoi);
  }

  @Override
  public PagingResponse<Citation> getDatasetCitations(String datasetKeyOrDoi, Pageable page) {
    PagingResponse<Citation> result;

    if (RegistryDoiUtils.isUuid(datasetKeyOrDoi)) {
      UUID datasetKey = UUID.fromString(datasetKeyOrDoi);
      result =
          pagingResponse(
              page,
              citationMapper.countByDataset(datasetKey),
              citationMapper.listByDataset(datasetKey, page));
    } else if (DOI.isParsable(datasetKeyOrDoi)) {
      List<Dataset> datasets = datasetMapper.listByDOI(datasetKeyOrDoi, new PagingRequest());

      if (CollectionUtils.isNotEmpty(datasets)) {
        Dataset dataset = datasets.get(0);
        result =
            pagingResponse(
                page,
                citationMapper.countByDataset(dataset.getKey()),
                citationMapper.listByDataset(dataset.getKey(), page));
      } else {
        result = new PagingResponse<>(0L, 20, 0L);
      }
    } else {
      result = new PagingResponse<>(0L, 20, 0L);
    }

    return result;
  }

  @Override
  public PagingResponse<Dataset> getCitationDatasets(DOI citationDoi, Pageable page) {
    return pagingResponse(
        page,
        citationMapper.countByCitation(citationDoi),
        citationMapper.listByCitation(citationDoi, page));
  }

  @Scheduled(cron = "${citation.cronPattern}")
  public void registerPostponedCitations() {
    LOG.info("Start registering delayed citations");

    List<Citation> citationsToRegister = citationMapper.listByRegistrationDate(new Date());

    for (Citation citation : citationsToRegister) {
      LOG.debug("Start registering citation {}", citation.getDoi());
      DataCiteMetadata metadata = metadataBuilderService.buildMetadata(citation);

      datasetDoiDataCiteHandlingService.scheduleDerivedDatasetRegistration(
          citation.getDoi(), metadata, citation.getTarget(), citation.getRegistrationDate());
    }
  }
}
