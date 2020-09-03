package org.gbif.registry.ws.resources;

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
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.gbif.registry.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.security.UserRoles.USER_ROLE;
import static org.gbif.registry.ws.util.WsUtils.pagingResponse;

@RestController
@RequestMapping(value = "citation", produces = MediaType.APPLICATION_JSON_VALUE)
public class CitationResource {

  private static final ZoneId UTC = ZoneId.of("UTC");
  private static final DateTimeFormatter REGULAR_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy");

  private final DoiGenerator doiGenerator;
  private final DataCiteDoiHandlerStrategy doiHandlerStrategy;
  private final CitationMapper citationMapper;

  public CitationResource(
      DoiGenerator doiGenerator,
      DataCiteDoiHandlerStrategy doiHandlerStrategy,
      CitationMapper citationMapper) {
    this.doiGenerator = doiGenerator;
    this.doiHandlerStrategy = doiHandlerStrategy;
    this.citationMapper = citationMapper;
  }

  @Secured({ADMIN_ROLE, USER_ROLE})
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public Citation createCitation(@RequestBody CitationCreationRequest request) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final String nameFromContext = authentication != null ? authentication.getName() : null;

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
    citation.setCreatedBy(nameFromContext);
    citation.setModifiedBy(nameFromContext);

    DataCiteMetadata metadata = doiHandlerStrategy.buildMetadata(citation);

    doiHandlerStrategy.scheduleDerivedDatasetRegistration(doi, metadata, request.getTarget());

    citationMapper.create(citation);
    for (String relatedDatasetKeyOrDoi : request.getRelatedDatasets()) {
      citationMapper.addDatasetCitation(relatedDatasetKeyOrDoi, doi);
    }

    return citation;
  }

  public Citation getCitation(DOI doi) {
    return citationMapper.get(doi);
  }

  @GetMapping("{doiPrefix}/{doiSuffix}")
  public Citation getCitation(
      @PathVariable("doiPrefix") String doiPrefix,
      @PathVariable("doiSuffix") String doiSuffix) {
    return getCitation(new DOI(doiPrefix, doiSuffix));
  }

  @GetMapping("dataset/{key}")
  public PagingResponse<Citation> getDatasetCitation(@PathVariable("key") UUID datasetKey, Pageable page) {
    return pagingResponse(
        page,
        citationMapper.countByDataset(datasetKey),
        citationMapper.listByDataset(datasetKey, page)
    );
  }

  public String getCitationText(DOI doi) {
    return citationMapper.get(doi).getCitation();
  }

  @GetMapping("{doiPrefix}/{doiSuffix}/citation")
  public String getCitationText(
      @PathVariable("doiPrefix") String doiPrefix,
      @PathVariable("doiSuffix") String doiSuffix) {
    return getCitationText(new DOI(doiPrefix, doiSuffix));
  }

  public PagingResponse<Dataset> getCitationDatasets(DOI citationDoi, Pageable page) {
    return pagingResponse(
        page,
        citationMapper.countByCitation(citationDoi),
        citationMapper.listByCitation(citationDoi, page)
    );
  }

  @GetMapping("{doiPrefix}/{doiSuffix}/datasets")
  public PagingResponse<Dataset> getCitationDatasets(
      @PathVariable("doiPrefix") String doiPrefix,
      @PathVariable("doiSuffix") String doiSuffix,
      Pageable page) {
    return getCitationDatasets(new DOI(doiPrefix, doiSuffix), page);
  }
}
