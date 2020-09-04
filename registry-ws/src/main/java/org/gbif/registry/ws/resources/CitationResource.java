package org.gbif.registry.ws.resources;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.registry.domain.ws.Citation;
import org.gbif.registry.domain.ws.CitationCreationRequest;
import org.gbif.registry.service.RegistryCitationService;
import org.gbif.registry.service.RegistryDatasetService;
import org.gbif.ws.WebApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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

import java.util.UUID;

import static org.gbif.registry.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.security.UserRoles.USER_ROLE;

@RestController
@RequestMapping(value = "citation", produces = MediaType.APPLICATION_JSON_VALUE)
public class CitationResource {

  private static final Logger LOG = LoggerFactory.getLogger(CitationResource.class);

  private final RegistryCitationService citationService;
  private final RegistryDatasetService datasetService;

  public CitationResource(RegistryCitationService citationService, RegistryDatasetService datasetService) {
    this.citationService = citationService;
    this.datasetService = datasetService;
  }

  @Secured({ADMIN_ROLE, USER_ROLE})
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public Citation createCitation(@RequestBody CitationCreationRequest request) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final String nameFromContext = authentication != null ? authentication.getName() : null;
    request.setCreator(nameFromContext);

    if (datasetService.checkDatasetsExist(request.getRelatedDatasets())) {
      LOG.debug("Invalid related datasets identifiers");
      throw new WebApplicationException("Wrong dataset identifiers", HttpStatus.BAD_REQUEST);
    }

    return citationService.create(request);
  }

  public Citation getCitation(DOI doi) {
    return citationService.get(doi);
  }

  @GetMapping("{doiPrefix}/{doiSuffix}")
  public Citation getCitation(
      @PathVariable("doiPrefix") String doiPrefix,
      @PathVariable("doiSuffix") String doiSuffix) {
    return getCitation(new DOI(doiPrefix, doiSuffix));
  }

  @GetMapping("dataset/{key}")
  public PagingResponse<Citation> getDatasetCitations(@PathVariable("key") UUID datasetKey, Pageable page) {
    return citationService.getDatasetCitations(datasetKey, page);
  }

  public String getCitationText(DOI doi) {
    return citationService.getCitationText(doi);
  }

  @GetMapping("{doiPrefix}/{doiSuffix}/citation")
  public String getCitationText(
      @PathVariable("doiPrefix") String doiPrefix,
      @PathVariable("doiSuffix") String doiSuffix) {
    return getCitationText(new DOI(doiPrefix, doiSuffix));
  }

  public PagingResponse<Dataset> getCitationDatasets(DOI citationDoi, Pageable page) {
    return citationService.getCitationDatasets(citationDoi, page);
  }

  @GetMapping("{doiPrefix}/{doiSuffix}/datasets")
  public PagingResponse<Dataset> getCitationDatasets(
      @PathVariable("doiPrefix") String doiPrefix,
      @PathVariable("doiSuffix") String doiSuffix,
      Pageable page) {
    return getCitationDatasets(new DOI(doiPrefix, doiSuffix), page);
  }
}
