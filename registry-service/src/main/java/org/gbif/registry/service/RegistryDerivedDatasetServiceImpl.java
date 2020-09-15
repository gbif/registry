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
import org.gbif.api.model.common.DoiData;
import org.gbif.api.model.common.DoiStatus;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.service.datacite.DataCiteValidator;
import org.gbif.registry.doi.DataCiteMetadataBuilderService;
import org.gbif.registry.doi.DatasetDoiDataCiteHandlingService;
import org.gbif.registry.doi.DoiIssuingService;
import org.gbif.registry.doi.util.RegistryDoiUtils;
import org.gbif.registry.domain.ws.DerivedDataset;
import org.gbif.registry.domain.ws.DerivedDatasetUsage;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.DerivedDatasetMapper;
import org.gbif.registry.persistence.mapper.DoiMapper;

import java.net.URI;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.JAXBException;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.common.collect.Iterators;

import static org.gbif.registry.service.util.ServiceUtils.pagingResponse;

@Service
public class RegistryDerivedDatasetServiceImpl implements RegistryDerivedDatasetService {

  private static final Logger LOG =
      LoggerFactory.getLogger(RegistryDerivedDatasetServiceImpl.class);

  private static final ZoneId UTC = ZoneId.of("UTC");
  private static final DateTimeFormatter REGULAR_DATE_FORMAT =
      DateTimeFormatter.ofPattern("d MMMM yyyy");
  private static final int BATCH_SIZE = 5_000;

  private final DataCiteMetadataBuilderService metadataBuilderService;
  private final DoiIssuingService doiIssuingService;
  private final DatasetDoiDataCiteHandlingService datasetDoiDataCiteHandlingService;
  private final DerivedDatasetMapper derivedDatasetMapper;
  private final DatasetMapper datasetMapper;
  private final DoiMapper doiMapper;
  private final String citationText;

  public RegistryDerivedDatasetServiceImpl(
      DataCiteMetadataBuilderService metadataBuilderService,
      DoiIssuingService doiIssuingService,
      DatasetDoiDataCiteHandlingService datasetDoiDataCiteHandlingService,
      DerivedDatasetMapper derivedDatasetMapper,
      DatasetMapper datasetMapper,
      DoiMapper doiMapper,
      @Value("${citation.text}") String citationText) {
    this.metadataBuilderService = metadataBuilderService;
    this.doiIssuingService = doiIssuingService;
    this.datasetDoiDataCiteHandlingService = datasetDoiDataCiteHandlingService;
    this.derivedDatasetMapper = derivedDatasetMapper;
    this.datasetMapper = datasetMapper;
    this.doiMapper = doiMapper;
    this.citationText = citationText;
  }

  @Override
  public String getCitationText(DOI citationDoi) {
    return get(citationDoi).getCitation();
  }

  @Override
  public DerivedDataset create(
      DerivedDataset derivedDataset, List<DerivedDatasetUsage> derivedDatasetUsages) {
    DOI doi = doiIssuingService.newDerivedDatasetDOI();

    derivedDataset.setDoi(doi);
    derivedDataset.setCitation(
        MessageFormat.format(citationText, LocalDate.now(UTC).format(REGULAR_DATE_FORMAT), doi));

    DataCiteMetadata metadata = metadataBuilderService.buildMetadata(derivedDataset);

    datasetDoiDataCiteHandlingService.scheduleDerivedDatasetRegistration(
        doi, metadata, derivedDataset.getTarget(), derivedDataset.getRegistrationDate());

    derivedDatasetMapper.create(derivedDataset);
    Iterators.partition(derivedDatasetUsages.iterator(), BATCH_SIZE)
        .forEachRemaining(batch -> derivedDatasetMapper.addCitationDatasets(doi, batch));

    return derivedDataset;
  }

  @Override
  public void update(DOI doi, URI target) {
    DoiData doiData = doiMapper.get(doi);

    if (doiData.getStatus() != DoiStatus.REGISTERED) {
      throw new IllegalStateException("Can change target only for registered DOI");
    }

    String xmlMetadata = doiMapper.getMetadata(doi);

    DataCiteMetadata metadata;
    try {
      metadata = DataCiteValidator.fromXml(xmlMetadata);
    } catch (JAXBException e) {
      throw new IllegalStateException("Can't parse metadata");
    }

    datasetDoiDataCiteHandlingService.scheduleDerivedDatasetUpdating(doi, metadata, target);

    derivedDatasetMapper.updateTarget(doi, target);
  }

  @Override
  public DerivedDataset get(DOI citationDoi) {
    return derivedDatasetMapper.get(citationDoi);
  }

  @Override
  public PagingResponse<DerivedDataset> getDerivedDataset(String datasetKeyOrDoi, Pageable page) {
    PagingResponse<DerivedDataset> result;

    if (RegistryDoiUtils.isUuid(datasetKeyOrDoi)) {
      UUID datasetKey = UUID.fromString(datasetKeyOrDoi);
      result =
          pagingResponse(
              page,
              derivedDatasetMapper.countByDataset(datasetKey),
              derivedDatasetMapper.listByDataset(datasetKey, page));
    } else if (DOI.isParsable(datasetKeyOrDoi)) {
      List<Dataset> datasets = datasetMapper.listByDOI(datasetKeyOrDoi, new PagingRequest());

      if (CollectionUtils.isNotEmpty(datasets)) {
        Dataset dataset = datasets.get(0);
        result =
            pagingResponse(
                page,
                derivedDatasetMapper.countByDataset(dataset.getKey()),
                derivedDatasetMapper.listByDataset(dataset.getKey(), page));
      } else {
        result = new PagingResponse<>(0L, 20, 0L);
      }
    } else {
      result = new PagingResponse<>(0L, 20, 0L);
    }

    return result;
  }

  @Override
  public PagingResponse<DerivedDatasetUsage> getRelatedDatasets(
      DOI derivedDatasetDoi, Pageable page) {
    return pagingResponse(
        page,
        derivedDatasetMapper.countDerivedDatasetUsages(derivedDatasetDoi),
        derivedDatasetMapper.listDerivedDatasetUsages(derivedDatasetDoi, page));
  }

  @Scheduled(cron = "${citation.cronPattern}")
  public void registerPostponedCitations() {
    LOG.info("Start registering delayed citations");
    List<DerivedDataset> citationsToRegister =
        derivedDatasetMapper.listByRegistrationDate(new Date());

    for (DerivedDataset derivedDataset : citationsToRegister) {
      LOG.debug("Start registering derivedDataset {}", derivedDataset.getDoi());
      DataCiteMetadata metadata = metadataBuilderService.buildMetadata(derivedDataset);

      datasetDoiDataCiteHandlingService.scheduleDerivedDatasetRegistration(
          derivedDataset.getDoi(),
          metadata,
          derivedDataset.getTarget(),
          derivedDataset.getRegistrationDate());
    }
  }
}
