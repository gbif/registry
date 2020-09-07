package org.gbif.registry.service;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.registry.doi.DataCiteMetadataBuilderService;
import org.gbif.registry.doi.DatasetDoiDataCiteHandlingService;
import org.gbif.registry.doi.DoiIssuingService;
import org.gbif.registry.domain.ws.Citation;
import org.gbif.registry.domain.ws.CitationCreationRequest;
import org.gbif.registry.persistence.mapper.CitationMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.gbif.registry.service.util.ServiceUtils.pagingResponse;

@Service
public class RegistryCitationServiceImpl implements RegistryCitationService {

  private static final ZoneId UTC = ZoneId.of("UTC");
  private static final DateTimeFormatter REGULAR_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy");

  private final DataCiteMetadataBuilderService metadataBuilderService;
  private final DoiIssuingService doiIssuingService;
  private final DatasetDoiDataCiteHandlingService datasetDoiDataCiteHandlingService;
  private final CitationMapper citationMapper;
  private final String citationText;

  public RegistryCitationServiceImpl(
      DataCiteMetadataBuilderService metadataBuilderService,
      DoiIssuingService doiIssuingService,
      DatasetDoiDataCiteHandlingService datasetDoiDataCiteHandlingService,
      CitationMapper citationMapper,
      @Value("${citation.text}") String citationText) {
    this.metadataBuilderService = metadataBuilderService;
    this.doiIssuingService = doiIssuingService;
    this.datasetDoiDataCiteHandlingService = datasetDoiDataCiteHandlingService;
    this.citationMapper = citationMapper;
    this.citationText = citationText;
  }

  @Override
  public String getCitationText(DOI citationDoi) {
    return get(citationDoi).getCitation();
  }

  @Override
  public Citation create(CitationCreationRequest request) {
    DOI doi = doiIssuingService.newDerivedDatasetDOI();
    Citation citation = toCitation(request, doi);
    DataCiteMetadata metadata = metadataBuilderService.buildMetadata(citation);

    datasetDoiDataCiteHandlingService
        .scheduleDerivedDatasetRegistration(doi, metadata, request.getTarget(), request.getRegistrationDate());

    citationMapper.create(citation);
    for (String relatedDatasetKeyOrDoi : request.getRelatedDatasets()) {
      citationMapper.addDatasetCitation(relatedDatasetKeyOrDoi, doi);
    }

    return citation;
  }

  private Citation toCitation(CitationCreationRequest request, DOI doi) {
    Citation citation = new Citation();
    citation.setDoi(doi);
    citation.setOriginalDownloadDOI(request.getOriginalDownloadDOI());
    citation.setCitation(
        MessageFormat.format(citationText, LocalDate.now(UTC).format(REGULAR_DATE_FORMAT), doi));
    citation.setTarget(request.getTarget());
    citation.setTitle(request.getTitle());
    citation.setCreatedBy(request.getCreator());
    citation.setModifiedBy(request.getCreator());
    citation.setRegistrationDate(request.getRegistrationDate());

    return citation;
  }

  @Override
  public Citation get(DOI citationDoi) {
    return citationMapper.get(citationDoi);
  }

  @Override
  public PagingResponse<Citation> getDatasetCitations(UUID datasetKey, Pageable page) {
    return pagingResponse(
        page,
        citationMapper.countByDataset(datasetKey),
        citationMapper.listByDataset(datasetKey, page)
    );
  }

  @Override
  public PagingResponse<Dataset> getCitationDatasets(DOI citationDoi, Pageable page) {
    return pagingResponse(
        page,
        citationMapper.countByCitation(citationDoi),
        citationMapper.listByCitation(citationDoi, page)
    );
  }

  @Scheduled(cron = "0 0 0 * * *")
  public void registerPostponedCitations() {
    List<Citation> citationsToRegister = citationMapper.listByRegistrationDate(new Date());

    for (Citation citation : citationsToRegister) {
      DataCiteMetadata metadata = metadataBuilderService.buildMetadata(citation);

      datasetDoiDataCiteHandlingService
          .scheduleDerivedDatasetRegistration(citation.getDoi(), metadata, citation.getTarget(), citation.getRegistrationDate());
    }
  }
}
