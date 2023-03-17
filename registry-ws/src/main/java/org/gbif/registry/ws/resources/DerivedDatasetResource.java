/*
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
import org.gbif.registry.events.CreateEvent;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.events.UpdateEvent;
import org.gbif.registry.service.RegistryDatasetService;
import org.gbif.registry.service.RegistryDerivedDatasetService;
import org.gbif.registry.service.RegistryOccurrenceDownloadService;
import org.gbif.registry.ws.util.SecurityUtil;
import org.gbif.ws.WebApplicationException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.groups.Default;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import static org.gbif.registry.security.SecurityContextCheck.checkIsNotAdmin;
import static org.gbif.registry.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.security.UserRoles.USER_ROLE;

@Tag(name = "Derived datasets",
  description = "Derived datasets are metadata records allowing the citation of a " +
  "subset of a larger dataset.  Typically the large dataset is a monthly download containing all GBIF occurrences " +
  "and made available on a public cloud service.",
  externalDocs = @ExternalDocumentation(description = "GBIF data blog article", url = "https://data-blog.gbif.org/post/derived-datasets/"))
@RestController
@RequestMapping(path = "derivedDataset", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class DerivedDatasetResource {

  private static final Logger LOG = LoggerFactory.getLogger(DerivedDatasetResource.class);

  private final RegistryDerivedDatasetService derivedDatasetService;
  private final RegistryDatasetService datasetService;
  private final RegistryOccurrenceDownloadService occurrenceDownloadService;
  private final EventManager eventManager;

  public DerivedDatasetResource(
      RegistryDerivedDatasetService derivedDatasetService,
      RegistryDatasetService datasetService,
      RegistryOccurrenceDownloadService occurrenceDownloadService,
      EventManager eventManager) {
    this.derivedDatasetService = derivedDatasetService;
    this.datasetService = datasetService;
    this.occurrenceDownloadService = occurrenceDownloadService;
    this.eventManager = eventManager;
  }

  @Operation(
    operationId = "createDerivedDataset",
    summary = "Create a new derived dataset",
    description = "Creates a new derived dataset with the specified source dataset and records of what subset " +
      "should be cited.",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0100")))
  @ApiResponse(
    responseCode = "201",
    description = "Derived dataset information.")
  @Secured({ADMIN_ROLE, USER_ROLE})
  @Validated({PrePersist.class, Default.class})
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public DerivedDataset create(
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "The request body should contain the parameters below.  Note the `relatedDatasets` map " +
        "may use GBIF Dataset DOIs or UUIDs as keys, with the values being the number of records to cite " +
        "from each dataset."
    )
    @RequestBody @Valid DerivedDatasetCreationRequest request) {
    return createDerivedDataset(request, request.getRelatedDatasets());
  }

  // It seems complicated to document the multipart form upload, and it's probably
  // only intended for use on the website anyway.
  @Hidden
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
        if (lineElements.length > 1) {
          records.put(lineElements[0], Long.valueOf(lineElements[1]));
        } else {
          records.put(lineElements[0], null);
        }
      }
    } catch (IOException e) {
      throw new WebApplicationException(
          "Error while reading file", HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (NumberFormatException e) {
      LOG.error("Invalid number {}", e.getMessage());
      throw new WebApplicationException("Invalid number " + e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    return createDerivedDataset(request, records);
  }

  private DerivedDataset createDerivedDataset(
      DerivedDatasetCreationRequest request, Map<String, Long> relatedDatasets) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final String nameFromContext = authentication != null ? authentication.getName() : null;

    if (relatedDatasets == null || relatedDatasets.isEmpty()) {
      LOG.error("No related datasets found");
      throw new WebApplicationException("Related datasets are required", HttpStatus.BAD_REQUEST);
    }

    if (request.getOriginalDownloadDOI() != null
        && !occurrenceDownloadService.checkOccurrenceDownloadExists(
            request.getOriginalDownloadDOI())) {
      LOG.error("Invalid original download DOI");
      throw new WebApplicationException("Invalid original download DOI", HttpStatus.BAD_REQUEST);
    }

    List<DerivedDatasetUsage> derivedDatasetUsages;
    try {
      derivedDatasetUsages = datasetService.ensureDerivedDatasetDatasetUsagesValid(relatedDatasets);
    } catch (IllegalArgumentException e) {
      LOG.error("Invalid related datasets identifiers");
      throw new WebApplicationException(
          "Invalid related datasets identifiers: " + e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    DerivedDataset derivedDataset =
        derivedDatasetService.create(
            toDerivedDataset(request, nameFromContext), derivedDatasetUsages);
    eventManager.post(CreateEvent.newInstance(derivedDataset, DerivedDataset.class));
    return derivedDataset;
  }

  public DerivedDataset getDerivedDataset(DOI doi) {
    return derivedDatasetService.get(doi);
  }

  @Operation(
    operationId = "updateDerivedDataset",
    summary = "Updates a derived dataset",
    description = "Updates the values of a derived dataset.\n\n" +
      "This is restricted to administrators, contact the [helpdesk](https://www.gbif.org/contact-us) " +
      "for assistance.",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0105")))
  @ApiResponse(
    responseCode = "201",
    description = "Derived dataset updated.")
  @Secured({ADMIN_ROLE, USER_ROLE})
  @PutMapping(path = "{doiPrefix}/{doiSuffix}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public void update(
      @PathVariable("doiPrefix") String doiPrefix,
      @PathVariable("doiSuffix") String doiSuffix,
      @RequestBody @Valid DerivedDatasetUpdateRequest request) {
    update(new DOI(doiPrefix, doiSuffix), request);
  }

  public void update(DOI derivedDatasetDoi, DerivedDatasetUpdateRequest request) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String nameFromContext = authentication != null ? authentication.getName() : null;

    DerivedDataset derivedDataset = derivedDatasetService.get(derivedDatasetDoi);

    if (derivedDataset == null) {
      LOG.error("Derived dataset with the DOI {} was not found", derivedDatasetDoi);
      throw new WebApplicationException(
          "Derived dataset with the DOI was not found", HttpStatus.NOT_FOUND);
    }

    if (!Objects.equals(derivedDataset.getCreatedBy(), nameFromContext)
        && checkIsNotAdmin(authentication)) {
      LOG.error(
          "User {} is not allowed to update the Derived dataset {}",
          nameFromContext,
          derivedDatasetDoi);
      throw new WebApplicationException(
          "User is not allowed to update the Derived dataset", HttpStatus.FORBIDDEN);
    }

    try {
      Optional.ofNullable(request.getSourceUrl()).ifPresent(derivedDataset::setSourceUrl);
      Optional.ofNullable(request.getDescription()).ifPresent(derivedDataset::setDescription);
      Optional.ofNullable(request.getTitle()).ifPresent(derivedDataset::setTitle);
      derivedDataset.setModifiedBy(nameFromContext);
      derivedDatasetService.update(derivedDataset);
      eventManager.post(
          UpdateEvent.newInstance(derivedDataset, derivedDataset, DerivedDataset.class));
    } catch (IllegalStateException e) {
      LOG.error(e.getMessage());
      throw new WebApplicationException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(
    operationId = "getDerivedDatasetByDoi",
    summary = "Retrieve a derived dataset record",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0105")))
  @ApiResponse(
    responseCode = "200",
    description = "Derived dataset information.")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{doiPrefix}/{doiSuffix}")
  public ResponseEntity<DerivedDataset> getDerivedDataset(
      @PathVariable("doiPrefix") String doiPrefix, @PathVariable("doiSuffix") String doiSuffix) {
    DerivedDataset result = getDerivedDataset(new DOI(doiPrefix, doiSuffix));
    return result != null ? ResponseEntity.ok(result) : ResponseEntity.notFound().build();
  }

  @Operation(
    operationId = "getDerivedDatasetByDatasetKey",
    summary = "Retrieve derived datasets of a dataset by key",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0110")))
  @Parameter(
    name = "key",
    description = "Dataset key",
    schema = @Schema(implementation = UUID.class)
  )
  @Pageable.OffsetLimitParameters
  @ApiResponse(
    responseCode = "200",
    description = "Derived dataset information.")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("dataset/{key}")
  public PagingResponse<DerivedDataset> getDerivedDatasets(
      @PathVariable("key") UUID datasetKey, Pageable page) {
    return getDerivedDatasets(datasetKey.toString(), page);
  }

  @Operation(
    operationId = "getDerivedDatasetByDatasetDoi",
    summary = "Retrieve derived datasets of a dataset by DOI",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0120")))
  @Pageable.OffsetLimitParameters
  @ApiResponse(
    responseCode = "200",
    description = "Derived dataset information.")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("dataset/{doiPrefix}/{doiSuffix}")
  public PagingResponse<DerivedDataset> getDerivedDatasets(
      @PathVariable("doiPrefix") String doiPrefix,
      @PathVariable("doiSuffix") String doiSuffix,
      Pageable page) {
    return getDerivedDatasets(new DOI(doiPrefix, doiSuffix).getDoiName(), page);
  }

  @Operation(
    operationId = "getDerivedDatasetByUser",
    summary = "Retrieve derived datasets of a dataset by User",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0130")))
  @Pageable.OffsetLimitParameters
  @ApiResponse(
    responseCode = "200",
    description = "Derived dataset information.")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @Secured({ADMIN_ROLE, USER_ROLE})
  @GetMapping("user/{user}")
  public PagingResponse<DerivedDataset> listByUser(
      @PathVariable("user") String user, Pageable page) {
    if (SecurityUtil.isAuthenticatedUser(user)
        || SecurityUtil.isAuthenticatedUserInRole(ADMIN_ROLE)) {
      return derivedDatasetService.listByUser(user, page);
    }
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Request denied to user");
  }

  public PagingResponse<DerivedDataset> getDerivedDatasets(String datasetKeyOrDoi, Pageable page) {
    return derivedDatasetService.getDerivedDataset(datasetKeyOrDoi, page);
  }

  public String getCitationText(DOI doi) {
    return derivedDatasetService.getCitationText(doi);
  }

  @Operation(
    operationId = "getDerivedDatasetCitation",
    summary = "Retrieve derived dataset citation",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0200")))
  @Pageable.OffsetLimitParameters
  @ApiResponse(
    responseCode = "200",
    description = "Derived dataset citation.")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{doiPrefix}/{doiSuffix}/citation")
  public ResponseEntity<String> getCitationText(
      @PathVariable("doiPrefix") String doiPrefix, @PathVariable("doiSuffix") String doiSuffix) {
    String result = getCitationText(new DOI(doiPrefix, doiSuffix));
    return result != null ? ResponseEntity.ok(result) : ResponseEntity.notFound().build();
  }

  public PagingResponse<DerivedDatasetUsage> getRelatedDatasets(
      DOI derivedDatasetDoi, Pageable page) {
    return derivedDatasetService.getRelatedDatasets(derivedDatasetDoi, page);
  }

  @Operation(
    operationId = "getDerivedDatasetRelatedDatasets",
    summary = "Retrieve derived dataset related datasets",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0210")))
  @Pageable.OffsetLimitParameters
  @ApiResponse(
    responseCode = "200",
    description = "Related datasets.")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{doiPrefix}/{doiSuffix}/datasets")
  public PagingResponse<DerivedDatasetUsage> getRelatedDatasets(
      @PathVariable("doiPrefix") String doiPrefix,
      @PathVariable("doiSuffix") String doiSuffix,
      Pageable page) {
    return getRelatedDatasets(new DOI(doiPrefix, doiSuffix), page);
  }

  private DerivedDataset toDerivedDataset(DerivedDatasetCreationRequest request, String creator) {
    DerivedDataset derivedDataset = new DerivedDataset();
    derivedDataset.setOriginalDownloadDOI(request.getOriginalDownloadDOI());
    derivedDataset.setDescription(request.getDescription());
    derivedDataset.setSourceUrl(request.getSourceUrl());
    derivedDataset.setTitle(request.getTitle());
    derivedDataset.setCreatedBy(creator);
    derivedDataset.setModifiedBy(creator);
    derivedDataset.setRegistrationDate(request.getRegistrationDate());

    return derivedDataset;
  }
}
