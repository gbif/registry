package org.gbif.registry.ws.client.collections;

import org.gbif.api.annotation.Trim;
import org.gbif.api.model.collections.lookup.LookupResult;
import org.gbif.api.vocabulary.Country;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("grscicoll/lookup")
public interface LookupClient {

  @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  LookupResult lookup(
      @RequestParam(value = "datasetKey", required = false) UUID datasetKey,
      @RequestParam(value = "institutionCode", required = false) @Trim String institutionCode,
      @RequestParam(value = "institutionId", required = false) @Trim String institutionId,
      @RequestParam(value = "ownerInstitutionCode", required = false) @Trim
          String ownerInstitutionCode,
      @RequestParam(value = "collectionCode", required = false) @Trim String collectionCode,
      @RequestParam(value = "collectionId", required = false) @Trim String collectionId,
      @RequestParam(value = "country") Country country,
      @RequestParam(value = "verbose", required = false) boolean verbose);
}
