package org.gbif.registry.ws.resources;

import org.gbif.api.model.common.DOI;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.registry.doi.generator.DoiGenerator;
import org.gbif.registry.doi.handler.DataCiteDoiHandlerStrategy;
import org.gbif.registry.domain.ws.CitationCreationRequest;
import org.gbif.registry.domain.ws.CitationResponse;
import org.gbif.registry.domain.ws.CitationUpdateRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

import java.util.UUID;

import static org.gbif.registry.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.security.UserRoles.USER_ROLE;

@RestController
@RequestMapping("citation")
public class CitationResource {

  private final DoiGenerator doiGenerator;
  private final DataCiteDoiHandlerStrategy doiHandlerStrategy;

  public CitationResource(
      DoiGenerator doiGenerator,
      DataCiteDoiHandlerStrategy doiHandlerStrategy) {
    this.doiGenerator = doiGenerator;
    this.doiHandlerStrategy = doiHandlerStrategy;
  }

  @Secured({ADMIN_ROLE, USER_ROLE})
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public CitationResponse createCitation(@RequestBody CitationCreationRequest request) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    DOI doi = doiGenerator.newDerivedDatasetDOI();

    request.setCreator(authentication.getName());

    DataCiteMetadata metadata = doiHandlerStrategy.buildMetadata(doi, request);

    doiHandlerStrategy.scheduleDerivedDatasetRegistration(doi, metadata, request.getTarget());

    CitationResponse citationResponse = new CitationResponse();
    citationResponse.setAssignedDOI(doi);
    citationResponse.setCitation("citation"); // TODO: 27/08/2020 generate citation
    citationResponse.setTarget(request.getTarget());
    citationResponse.setTitle(request.getTitle());

    return citationResponse;
  }

  @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public CitationResponse createCitation(@RequestBody CitationUpdateRequest request) {
    throw new UnsupportedOperationException("not implemented");
  }

  @GetMapping(path = "{doiPrefix}/{doiSuffix}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity getCitation(
      @PathVariable("doiPrefix") String doiPrefix,
      @PathVariable("doiSuffix") String doiSuffix) {
    throw new UnsupportedOperationException("not implemented");
  }

  @GetMapping(path = "dataset/{key}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity getDatasetCitation(@PathVariable("key")UUID datasetKey) {
    throw new UnsupportedOperationException("not implemented");
  }

  @GetMapping(path = "{doiPrefix}/{doiSuffix}/citation", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity getCitationText(
      @PathVariable("doiPrefix") String doiPrefix,
      @PathVariable("doiSuffix") String doiSuffix) {
    throw new UnsupportedOperationException("not implemented");
  }

  @GetMapping(path = "{doiPrefix}/{doiSuffix}/datasets", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity getCitationDatasets(
      @PathVariable("doiPrefix") String doiPrefix,
      @PathVariable("doiSuffix") String doiSuffix) {
    throw new UnsupportedOperationException("not implemented");
  }
}
