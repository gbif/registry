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
import org.gbif.registry.domain.ws.CitationUpdateRequest;
import org.gbif.registry.persistence.mapper.CitationMapper;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.gbif.registry.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.security.UserRoles.USER_ROLE;

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

    request.setCreator(nameFromContext);

    DataCiteMetadata metadata = doiHandlerStrategy.buildMetadata(doi, request);

    doiHandlerStrategy.scheduleDerivedDatasetRegistration(doi, metadata, request.getTarget());

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

    citationMapper.create(citation);
    for (String relatedDatasetKeyOrDoi : request.getRelatedDatasets()) {
      citationMapper.addDatasetCitation(relatedDatasetKeyOrDoi, doi);
    }

    return citation;
  }

  @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public Citation updateCitation(@RequestBody CitationUpdateRequest request) {
    throw new UnsupportedOperationException("not implemented");
  }

  @GetMapping("{doiPrefix}/{doiSuffix}")
  public Citation getCitation(
      @PathVariable("doiPrefix") String doiPrefix,
      @PathVariable("doiSuffix") String doiSuffix) {
    return citationMapper.get(new DOI(doiPrefix, doiSuffix));
  }

  @GetMapping("dataset/{key}")
  public PagingResponse<Citation> getDatasetCitation(@PathVariable("key") UUID datasetKey, Pageable page) {
    return citationMapper.listByDataset(datasetKey, page);
  }

  @GetMapping("{doiPrefix}/{doiSuffix}/citation")
  public String getCitationText(
      @PathVariable("doiPrefix") String doiPrefix,
      @PathVariable("doiSuffix") String doiSuffix) {
    return citationMapper.get(new DOI(doiPrefix, doiSuffix)).getCitation();
  }

  @GetMapping("{doiPrefix}/{doiSuffix}/datasets")
  public PagingResponse<Dataset> getCitationDatasets(
      @PathVariable("doiPrefix") String doiPrefix,
      @PathVariable("doiSuffix") String doiSuffix,
      Pageable page) {
    return citationMapper.listByCitation(new DOI(doiPrefix, doiSuffix), page);
  }
}
