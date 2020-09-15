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
package org.gbif.registry.ws.resources;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.PrePersist;
import org.gbif.registry.domain.ws.DerivedDataset;
import org.gbif.registry.domain.ws.DerivedDatasetCreationRequest;
import org.gbif.registry.domain.ws.DerivedDatasetUpdateRequest;
import org.gbif.registry.domain.ws.DerivedDatasetUsage;
import org.gbif.registry.service.RegistryDerivedDatasetService;
import org.gbif.registry.service.RegistryDatasetService;
import org.gbif.registry.service.RegistryOccurrenceDownloadService;
import org.gbif.ws.WebApplicationException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.groups.Default;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static org.gbif.registry.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.security.UserRoles.USER_ROLE;

@RestController
@RequestMapping(value = "derived_dataset", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class DerivedDatasetResource {

  private static final Logger LOG = LoggerFactory.getLogger(DerivedDatasetResource.class);

  private final RegistryDerivedDatasetService derivedDatasetService;
  private final RegistryDatasetService datasetService;
  private final RegistryOccurrenceDownloadService occurrenceDownloadService;

  public DerivedDatasetResource(
      RegistryDerivedDatasetService derivedDatasetService,
      RegistryDatasetService datasetService,
      RegistryOccurrenceDownloadService occurrenceDownloadService) {
    this.derivedDatasetService = derivedDatasetService;
    this.datasetService = datasetService;
    this.occurrenceDownloadService = occurrenceDownloadService;
  }

  @Secured({ADMIN_ROLE, USER_ROLE})
  @Validated({PrePersist.class, Default.class})
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public DerivedDataset create(@RequestBody @Valid DerivedDatasetCreationRequest request) {
    return createDerivedDataset(request, request.getRelatedDatasets());
  }

  @Secured({ADMIN_ROLE, USER_ROLE})
  @Validated({PrePersist.class, Default.class})
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public DerivedDataset create(
      @RequestPart("derivedDataset") @Valid DerivedDatasetCreationRequest request,
      @RequestPart("relatedDatasets") MultipartFile file) {
    Map<String, Long> records = new HashMap<>();
    try (Scanner scanner = new Scanner(file.getInputStream())) {
      while (scanner.hasNextLine()) {
        String[] lineElements = scanner.nextLine().split(",");
        records.put(lineElements[0], Long.valueOf(lineElements[1]));
      }
    } catch (IOException e) {
      throw new WebApplicationException(
          "Error while reading file", HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (NumberFormatException e) {
      LOG.error("Wrong number {}", e.getMessage());
      throw new WebApplicationException("Invalid number " + e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    return createDerivedDataset(request, records);
  }

  private DerivedDataset createDerivedDataset(
    DerivedDatasetCreationRequest request, Map<String, Long> relatedDatasets) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final String nameFromContext = authentication != null ? authentication.getName() : null;
    request.setCreator(nameFromContext);

    if (!occurrenceDownloadService.checkOccurrenceDownloadExists(
        request.getOriginalDownloadDOI())) {
      LOG.error("Invalid original download DOI");
      throw new WebApplicationException("Invalid original download DOI", HttpStatus.BAD_REQUEST);
    }

    List<DerivedDatasetUsage> derivedDatasetUsages;
    try {
      derivedDatasetUsages = datasetService.ensureCitationDatasetUsagesValid(relatedDatasets);
    } catch (IllegalArgumentException e) {
      LOG.error("Invalid related datasets identifiers");
      throw new WebApplicationException(
          "Invalid related datasets identifiers", HttpStatus.BAD_REQUEST);
    }

    return derivedDatasetService.create(toDerivedDataset(request), derivedDatasetUsages);
  }

  public DerivedDataset getDerivedDataset(DOI doi) {
    return derivedDatasetService.get(doi);
  }

  @Secured({ADMIN_ROLE, USER_ROLE})
  @PutMapping(path = "{doiPrefix}/{doiSuffix}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public void updateCitation(
      @PathVariable("doiPrefix") String doiPrefix,
      @PathVariable("doiSuffix") String doiSuffix,
      @RequestBody @Valid DerivedDatasetUpdateRequest request) {
    updateCitation(new DOI(doiPrefix, doiSuffix), request);
  }

  public void updateCitation(DOI citationDoi, DerivedDatasetUpdateRequest request) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String nameFromContext = authentication != null ? authentication.getName() : null;

    DerivedDataset derivedDataset = derivedDatasetService.get(citationDoi);

    if (derivedDataset == null) {
      LOG.error("Citation with the DOI {} was not found", citationDoi);
      throw new WebApplicationException(
          "Citation with the DOI was not found", HttpStatus.NOT_FOUND);
    }

    if (!derivedDataset.getCreatedBy().equals(nameFromContext)) {
      LOG.error("User {} is not allowed to update the Citation {}", nameFromContext, citationDoi);
      throw new WebApplicationException(
          "User is not allowed to update the Citation", HttpStatus.FORBIDDEN);
    }

    try {
      derivedDatasetService.update(citationDoi, request.getTarget());
    } catch (IllegalStateException e) {
      LOG.error(e.getMessage());
      throw new WebApplicationException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping("{doiPrefix}/{doiSuffix}")
  public DerivedDataset getDerivedDataset(
      @PathVariable("doiPrefix") String doiPrefix, @PathVariable("doiSuffix") String doiSuffix) {
    return getDerivedDataset(new DOI(doiPrefix, doiSuffix));
  }

  @GetMapping("dataset/{key}")
  public PagingResponse<DerivedDataset> getDerivedDatasets(
      @PathVariable("key") UUID datasetKey, Pageable page) {
    return getDerivedDatasets(datasetKey.toString(), page);
  }

  @GetMapping("dataset/{doiPrefix}/{doiSuffix}")
  public PagingResponse<DerivedDataset> getDerivedDatasets(
      @PathVariable("doiPrefix") String doiPrefix,
      @PathVariable("doiSuffix") String doiSuffix,
      Pageable page) {
    return getDerivedDatasets(new DOI(doiPrefix, doiSuffix).getDoiName(), page);
  }

  public PagingResponse<DerivedDataset> getDerivedDatasets(String datasetKeyOrDoi, Pageable page) {
    return derivedDatasetService.getDerivedDataset(datasetKeyOrDoi, page);
  }

  public String getCitationText(DOI doi) {
    return derivedDatasetService.getCitationText(doi);
  }

  @GetMapping("{doiPrefix}/{doiSuffix}/citation")
  public String getCitationText(
      @PathVariable("doiPrefix") String doiPrefix, @PathVariable("doiSuffix") String doiSuffix) {
    return getCitationText(new DOI(doiPrefix, doiSuffix));
  }

  public PagingResponse<DerivedDatasetUsage> getRelatedDatasets(DOI citationDoi, Pageable page) {
    return derivedDatasetService.getRelatedDatasets(citationDoi, page);
  }

  @GetMapping("{doiPrefix}/{doiSuffix}/datasets")
  public PagingResponse<DerivedDatasetUsage> getRelatedDatasets(
      @PathVariable("doiPrefix") String doiPrefix,
      @PathVariable("doiSuffix") String doiSuffix,
      Pageable page) {
    return getRelatedDatasets(new DOI(doiPrefix, doiSuffix), page);
  }

  private DerivedDataset toDerivedDataset(DerivedDatasetCreationRequest request) {
    DerivedDataset derivedDataset = new DerivedDataset();
    derivedDataset.setOriginalDownloadDOI(request.getOriginalDownloadDOI());
    derivedDataset.setTarget(request.getTarget());
    derivedDataset.setTitle(request.getTitle());
    derivedDataset.setCreatedBy(request.getCreator());
    derivedDataset.setModifiedBy(request.getCreator());
    derivedDataset.setRegistrationDate(request.getRegistrationDate());

    return derivedDataset;
  }
}
