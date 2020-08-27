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
package org.gbif.registry.ws.resources.collections;

import org.gbif.api.model.collections.lookup.LookupParams;
import org.gbif.api.model.collections.lookup.LookupResult;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.service.collections.lookup.DefaultLookupService;

import java.util.UUID;

import javax.annotation.Nullable;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "grscicoll/lookup", produces = MediaType.APPLICATION_JSON_VALUE)
public class LookupResource {

  private final DefaultLookupService lookupService;

  public LookupResource(DefaultLookupService lookupService) {
    this.lookupService = lookupService;
  }

  @GetMapping
  public LookupResult lookup(
      @Nullable @RequestParam(value = "datasetKey", required = false) UUID datasetKey,
      @Nullable @RequestParam(value = "institutionCode", required = false) String institutionCode,
      @Nullable @RequestParam(value = "institutionId", required = false) String institutionId,
      @Nullable @RequestParam(value = "ownerInstitutionCode", required = false)
          String ownerInstitutionCode,
      @Nullable @RequestParam(value = "collectionCode", required = false) String collectionCode,
      @Nullable @RequestParam(value = "collectionId", required = false) String collectionId,
      @Nullable Country country,
      @Nullable @RequestParam(value = "verbose", required = false) boolean verbose) {

    LookupParams params = new LookupParams();
    params.setDatasetKey(datasetKey);
    params.setInstitutionCode(institutionCode);
    params.setInstitutionId(institutionId);
    params.setOwnerInstitutionCode(ownerInstitutionCode);
    params.setCollectionCode(collectionCode);
    params.setCollectionId(collectionId);
    params.setCountry(country);
    params.setVerbose(verbose);

    return lookupService.lookup(params);
  }
}
