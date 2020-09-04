package org.gbif.registry.service;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.registry.doi.generator.DoiGenerator;
import org.gbif.registry.doi.handler.DataCiteDoiHandlerStrategy;
import org.gbif.registry.domain.ws.Citation;
import org.gbif.registry.domain.ws.CitationCreationRequest;
import org.gbif.registry.persistence.mapper.CitationMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.gbif.registry.service.util.ServiceUtils.pagingResponse;

@Service
public class RegistryCitationServiceImpl implements RegistryCitationService {

  private static final ZoneId UTC = ZoneId.of("UTC");
  private static final DateTimeFormatter REGULAR_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy");

  private final DoiGenerator doiGenerator;
  private final DataCiteDoiHandlerStrategy doiHandlerStrategy;
  private final CitationMapper citationMapper;

  public RegistryCitationServiceImpl(
      DoiGenerator doiGenerator,
      DataCiteDoiHandlerStrategy doiHandlerStrategy,
      CitationMapper citationMapper) {
    this.doiGenerator = doiGenerator;
    this.doiHandlerStrategy = doiHandlerStrategy;
    this.citationMapper = citationMapper;
  }

  @Override
  public String getCitationText(DOI citationDoi) {
    return get(citationDoi).getCitation();
  }

  @Override
  public Citation create(CitationCreationRequest request) {
    DOI doi = doiGenerator.newDerivedDatasetDOI();

    Citation citation = new Citation();
    citation.setDoi(doi);
    citation.setOriginalDownloadDOI(request.getOriginalDownloadDOI());
    citation.setCitation(
        "Citation GBIF.org ("
            + LocalDate.now(UTC).format(REGULAR_DATE_FORMAT)
            + ") Filtered export of GBIF occurrence data https://doi.org/" + doi);
    citation.setTarget(request.getTarget());
    citation.setTitle(request.getTitle());
    citation.setCreatedBy(request.getCreator());
    citation.setModifiedBy(request.getCreator());

    DataCiteMetadata metadata = doiHandlerStrategy.buildMetadata(citation);

    doiHandlerStrategy.scheduleDerivedDatasetRegistration(doi, metadata, request.getTarget());

    citationMapper.create(citation);
    for (String relatedDatasetKeyOrDoi : request.getRelatedDatasets()) {
      citationMapper.addDatasetCitation(relatedDatasetKeyOrDoi, doi);
    }

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
}
