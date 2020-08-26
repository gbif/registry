package org.gbif.registry.ws.resources;

import org.gbif.registry.doi.generator.DoiGenerator;
import org.gbif.registry.domain.ws.CitationRequest;
import org.gbif.registry.domain.ws.CitationResponse;
import org.gbif.registry.persistence.mapper.DoiMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("citation")
public class CitationResource {

  private final DoiGenerator doiGenerator;
  private final DoiMapper doiMapper;

  public CitationResource(DoiGenerator doiGenerator, DoiMapper doiMapper) {
    this.doiGenerator = doiGenerator;
    this.doiMapper = doiMapper;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public CitationResponse createCitation(@RequestBody CitationRequest request) {
    throw new UnsupportedOperationException("not implemented");
  }

  // TODO: 26/08/2020 update method

  @GetMapping(path = "{doiPrefix}/{doiSuffix}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity getCitation(
      @PathVariable("doiPrefix") String doiPrefix,
      @PathVariable("doiSuffix") String doiSuffix) {
    throw new UnsupportedOperationException("not implemented");
  }

  @GetMapping(path = "dataset/{key}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity getDatasetCitation() {
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
