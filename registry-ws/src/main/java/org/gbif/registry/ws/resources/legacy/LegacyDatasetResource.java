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
package org.gbif.registry.ws.resources.legacy;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NetworkService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.domain.ws.ErrorResponse;
import org.gbif.registry.domain.ws.IptEntityResponse;
import org.gbif.registry.domain.ws.LegacyDataset;
import org.gbif.registry.domain.ws.LegacyDatasetResponse;
import org.gbif.registry.domain.ws.LegacyDatasetResponseListWrapper;
import org.gbif.registry.domain.ws.util.LegacyResourceConstants;
import org.gbif.registry.ws.util.LegacyResourceUtils;
import org.gbif.ws.NotFoundException;
import org.gbif.ws.util.CommonWsUtils;

import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;

import io.swagger.v3.oas.annotations.Hidden;

import static org.gbif.registry.ws.util.LegacyResourceUtils.extractOrgKeyFromSecurity;

/**
 * Handle all legacy web service Dataset requests (excluding IPT requests), previously handled by
 * the GBRDS.
 */
@Hidden
@RestController
@RequestMapping("registry")
public class LegacyDatasetResource {

  private static final Logger LOG = LoggerFactory.getLogger(LegacyDatasetResource.class);

  private final OrganizationService organizationService;
  private final DatasetService datasetService;
  private final InstallationService installationService;
  private final IptResource iptResource;
  private final NetworkService networkService;

  public LegacyDatasetResource(
      OrganizationService organizationService,
      DatasetService datasetService,
      IptResource iptResource,
      InstallationService installationService,
      NetworkService networkService) {
    this.organizationService = organizationService;
    this.datasetService = datasetService;
    this.iptResource = iptResource;
    this.installationService = installationService;
    this.networkService = networkService;
  }

  /**
   * Register GBRDS dataset, handling incoming request with path /resource. The primary contact,
   * publishing organization key, and resource name are mandatory. Only after both the dataset and
   * primary contact have been persisted is a response with {@link HttpStatus#CREATED} (201) returned.
   *
   * @param dataset {@link LegacyDataset} with HTTP form parameters
   * @return {@link ResponseEntity}
   * @see IptResource#registerDataset(LegacyDataset, Authentication)
   */
  @PostMapping(
      value = "resource",
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
      produces = MediaType.APPLICATION_XML_VALUE)
  public ResponseEntity<IptEntityResponse> registerDataset(
      @RequestParam LegacyDataset dataset, Authentication authentication) {
    // reuse existing subresource
    return iptResource.registerDataset(dataset, authentication);
  }

  /**
   * Update GBRDS Dataset, handling incoming request with path /resource/{key}. The publishing
   * organization key is mandatory (supplied in the credentials not the parameters). The primary
   * contact is not required, but if any of the primary contact parameters were included in the
   * request, it is required. This is the difference between this method and registerDataset. Only
   * after both the dataset and optional primary contact have been updated is a response with
   * {@link HttpStatus#OK} (201) returned.
   *
   * @param datasetKey dataset key (UUID) coming in as path param
   * @param dataset {@link LegacyDataset} with HTTP form parameters
   * @return {@link ResponseEntity} with {@link HttpStatus#CREATED} (201) if successful
   */
  @PostMapping(
      value = "resource/{key}",
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
      produces = MediaType.APPLICATION_XML_VALUE)
  public ResponseEntity<LegacyDataset> updateDataset(
      @PathVariable("key") UUID datasetKey,
      @RequestParam LegacyDataset dataset,
      Authentication authentication) {
    // set required fields
    String user = authentication.getName();
    dataset.setCreatedBy(user);
    dataset.setModifiedBy(user);
    dataset.setKey(datasetKey);
    // retrieve existing dataset
    Dataset existing = datasetService.get(datasetKey);
    // populate dataset with existing primary contact so it gets updated, not duplicated
    dataset.setContacts(existing.getContacts());
    // if primary contact wasn't supplied, set existing one here so that it doesn't respond
    // BAD_REQUEST
    if (dataset.getPrimaryContactAddress() == null
        && dataset.getPrimaryContactEmail() == null
        && dataset.getPrimaryContactType() == null
        && dataset.getPrimaryContactPhone() == null
        && dataset.getPrimaryContactName() == null
        && dataset.getPrimaryContactDescription() == null) {
      dataset.setPrimaryContact(LegacyResourceUtils.getPrimaryContact(existing));
    }
    // otherwise, update primary contact and type
    else {
      dataset.prepare();
    }
    // If installation key wasn't provided, reuse existing dataset's installation key
    // Reason: non-IPT consumers weren't aware they could supply the parameter iptKey on dataset
    // updates before
    if (dataset.getInstallationKey() == null) {
      dataset.setInstallationKey(existing.getInstallationKey());
    }
    // Dataset can only have 1 installation key, log if the hosting installation is being changed
    else if (dataset.getInstallationKey() != existing.getInstallationKey()) {
      LOG.debug(
          "The dataset's technical installation is being changed from {} to {}",
          dataset.getInstallationKey(),
          existing.getInstallationKey());
    }
    // type can't be derived from endpoints, since there are no endpoints supplied on this update,
    // so re-set existing
    dataset.setType(existing.getType());
    // populate publishing organization from credentials
    dataset.setPublishingOrganizationKey(extractOrgKeyFromSecurity(authentication));
    // ensure the publishing organization exists, the installation exists, primary contact exists,
    // etc
    Contact contact = dataset.getPrimaryContact();
    if (contact != null
        && LegacyResourceUtils.isValidOnUpdate(
            dataset, datasetService, organizationService, installationService)) {
      // update only fields that could have changed
      existing.setModifiedBy(user);
      existing.setTitle(dataset.getTitle());
      existing.setDescription(dataset.getDescription());
      existing.setHomepage(dataset.getHomepage());
      existing.setLogoUrl(dataset.getLogoUrl());
      existing.setLanguage(dataset.getLanguage());
      existing.setInstallationKey(dataset.getInstallationKey());

      existing.setPublishingOrganizationKey(dataset.getPublishingOrganizationKey());

      // persist changes
      datasetService.update(existing);

      // set primary contact's required field(s)
      contact.setModifiedBy(user);
      // add/update primary contact: Contacts are mutable, so try to update if the Contact already
      // exists
      if (contact.getKey() == null) {
        contact.setCreatedBy(user);
        datasetService.addContact(datasetKey, contact);
      } else {
        datasetService.updateContact(datasetKey, contact);
      }

      // endpoint changes are done through Service API

      LOG.info("Dataset updated successfully, key={}", datasetKey);
      return ResponseEntity.status(HttpStatus.CREATED)
          .cacheControl(CacheControl.noCache())
          .body(dataset);
    } else {
      LOG.error("Request invalid. Dataset missing required fields or using stale keys!");
    }

    LOG.error("Dataset update failed");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .cacheControl(CacheControl.noCache())
        .build();
  }

  /**
   * Checks whether dataset belongs to the organization.
   *
   * @param datasetKey dataset key (UUID) coming in as path variable
   * @param organizationKey organization key (UUID) coming in as path variable
   * @return {@link ResponseEntity} with true or false value
   */
  @GetMapping(
      value = {"resource/{key}/belongs/organisation/{organisationKey}"},
      consumes = {MediaType.ALL_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<?> datasetBelongsToOrganisation(
      @PathVariable("key") UUID datasetKey, @PathVariable("organisationKey") UUID organizationKey) {
    LOG.debug(
        "Check dataset belongs to organization, datasetKey={}, organizationKey={}",
        datasetKey,
        organizationKey);
    try {
      // verify organization with key exists, otherwise NotFoundException gets thrown
      organizationService.get(organizationKey);
    } catch (NotFoundException e) {
      LOG.error(
          "The organization with key {} specified by query parameter does not exist",
          organizationKey);
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(new ErrorResponse("No organisation matches the key provided"));
    }

    try {
      Dataset dataset = datasetService.get(datasetKey);
      // true - if belongs, false - otherwise
      return ResponseEntity.ok(organizationKey.equals(dataset.getPublishingOrganizationKey()));
    } catch (NotFoundException e) {
      LOG.error("The dataset with key {} specified by parameter does not exist", datasetKey);
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(new ErrorResponse("No dataset matches the key provided"));
    }
  }

  /**
   * Retrieve all Datasets owned by an organization, handling incoming request with path /resource.
   * The publishing organization query parameter is mandatory. Only after both the organizationKey
   * is verified to correspond to an existing organization, is a response including the list of
   * Datasets returned.
   *
   * @param organizationKey organization key (UUID) coming in as query param
   * @return {@link ResponseEntity} with list of Datasets or empty list with error message if none found
   */
  @GetMapping(
      value = {"resource", "resource{extension:\\.[a-z]+}"},
      consumes = {MediaType.ALL_VALUE},
      produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<?> datasetsForOrganization(
      @PathVariable(value = "extension", required = false) String extension,
      @RequestParam(value = "organisationKey", required = false) UUID organizationKey,
      HttpServletResponse httpResponse) {
    String responseType =
        CommonWsUtils.getResponseTypeByExtension(extension, MediaType.APPLICATION_XML_VALUE);
    if (responseType != null) {
      httpResponse.setContentType(responseType);
    } else {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .cacheControl(CacheControl.noCache())
          .build();
    }

    if (organizationKey != null) {
      try {
        LOG.debug("Get all Datasets owned by Organization, key={}", organizationKey);
        // verify organization with key exists, otherwise NotFoundException gets thrown
        organizationService.get(organizationKey);

        List<LegacyDatasetResponse> datasets = Lists.newArrayList();
        PagingRequest page = new PagingRequest(0, LegacyResourceConstants.WS_PAGE_SIZE);
        PagingResponse<Dataset> response;
        do {
          LOG.debug(
              "Requesting {} datasets starting at offset {}", page.getLimit(), page.getOffset());
          response = organizationService.publishedDatasets(organizationKey, page);
          for (Dataset d : response.getResults()) {
            Contact contact = LegacyResourceUtils.getPrimaryContact(d);
            datasets.add(new LegacyDatasetResponse(d, contact));
          }
          page.nextPage();
        } while (!response.isEndOfRecords());
        LOG.debug("Get all Datasets owned by Organization finished");

        return ResponseEntity.status(HttpStatus.OK)
            .contentType(MediaType.parseMediaType(responseType))
            .body(new LegacyDatasetResponseListWrapper(datasets));
      } catch (NotFoundException e) {
        LOG.error(
            "The organization with key {} specified by query parameter does not exist",
            organizationKey);
      }
    }
    return ResponseEntity.status(HttpStatus.OK)
        .body(new ErrorResponse("No organisation matches the key provided"));
  }

  /**
   * Read GBRDS Dataset, handling incoming request with path /resource/{key}.
   *
   * @param datasetKey dataset key (UUID) coming in as path param
   * @return {@link ResponseEntity} with {@link HttpStatus#OK} (200) if dataset exists
   */
  @GetMapping(
      value = {"resource/{key:[a-zA-Z0-9-]+}", "resource/{key:[a-zA-Z0-9-]+}{extension:\\.[a-z]+}"},
      consumes = {MediaType.ALL_VALUE},
      produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<?> readDataset(
      @PathVariable("key") UUID datasetKey,
      @PathVariable(value = "extension", required = false) String extension,
      HttpServletResponse response) {
    String responseType =
        CommonWsUtils.getResponseTypeByExtension(extension, MediaType.APPLICATION_XML_VALUE);
    if (responseType != null) {
      response.setContentType(responseType);
    } else {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .cacheControl(CacheControl.noCache())
          .build();
    }
    try {
      LOG.debug("Get Dataset, key={}", datasetKey);
      // verify Dataset with key exists, otherwise NotFoundException gets thrown
      Dataset dataset = datasetService.get(datasetKey);
      Contact contact = LegacyResourceUtils.getPrimaryContact(dataset);
      return ResponseEntity.status(HttpStatus.OK)
          .cacheControl(CacheControl.noCache())
          .contentType(MediaType.parseMediaType(responseType))
          .body(new LegacyDatasetResponse(dataset, contact));
    } catch (NotFoundException e) {
      LOG.error("The dataset with key {} specified by path parameter does not exist", datasetKey);
    }
    return ResponseEntity.status(HttpStatus.OK)
        .cacheControl(CacheControl.noCache())
        .contentType(MediaType.parseMediaType(responseType))
        .body(new ErrorResponse("No resource matches the key provided"));
  }

  /**
   * Delete GBRDS Dataset, handling incoming request with path /resource/{key}. Only credentials are
   * mandatory. If deletion is successful, returns response with {@link HttpStatus#OK}.
   *
   * @param datasetKey dataset key (UUID) coming in as path param
   * @return {@link ResponseEntity} with {@link HttpStatus#OK} if successful
   * @see IptResource#deleteDataset(java.util.UUID)
   */
  @SuppressWarnings("rawtypes")
  @DeleteMapping(value = "resource/{key}")
  public ResponseEntity deleteDataset(@PathVariable("key") UUID datasetKey) {
    // reuse existing method
    return iptResource.deleteDataset(datasetKey);
  }

  @PostMapping(value = "resource/{key}/network/{networkKey}")
  public ResponseEntity<Void> addDatasetToNetwork(
      @PathVariable("networkKey") UUID networkKey, @PathVariable("key") UUID key) {
    networkService.addConstituent(networkKey, key);
    return ResponseEntity.noContent().cacheControl(CacheControl.noCache()).build();
  }

  @DeleteMapping(value = "resource/{key}/network/{networkKey}")
  public ResponseEntity<Void> removeDatasetFromNetwork(
      @PathVariable("networkKey") UUID networkKey, @PathVariable("key") UUID key) {
    networkService.removeConstituent(networkKey, key);
    return ResponseEntity.noContent().cacheControl(CacheControl.noCache()).build();
  }
}
