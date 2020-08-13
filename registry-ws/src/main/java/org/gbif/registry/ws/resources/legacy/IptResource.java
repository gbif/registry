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
package org.gbif.registry.ws.resources.legacy;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.License;
import org.gbif.registry.domain.ws.IptEntityResponse;
import org.gbif.registry.domain.ws.LegacyDataset;
import org.gbif.registry.domain.ws.LegacyInstallation;
import org.gbif.registry.ws.util.LegacyResourceUtils;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.gbif.registry.ws.util.LegacyResourceUtils.extractOrgKeyFromSecurity;

/**
 * Handle all legacy web service requests coming from IPT installations, previously handled by the
 * GBRDS.
 */
@RestController
@RequestMapping("registry/ipt")
public class IptResource {

  private static final Logger LOG = LoggerFactory.getLogger(IptResource.class);

  private final InstallationService installationService;
  private final OrganizationService organizationService;
  private final DatasetService datasetService;
  private static final Long ONE = 1L;

  public IptResource(
      InstallationService installationService,
      OrganizationService organizationService,
      DatasetService datasetService) {
    this.installationService = installationService;
    this.organizationService = organizationService;
    this.datasetService = datasetService;
  }

  /**
   * Register IPT installation, handling incoming request with path /ipt/register. The primary
   * contact and hosting organization key are mandatory. Only after both the installation and
   * primary contact have been persisted is a ResponseEntity with HttpStatus.CREATED returned.
   *
   * @param installation IptInstallation with HTTP form parameters
   * @return ResponseEntity with HttpStatus.CREATED if successful
   */
  @PostMapping(
      value = "register",
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
      produces = MediaType.APPLICATION_XML_VALUE)
  public ResponseEntity<IptEntityResponse> registerIpt(
      @RequestParam LegacyInstallation installation, Authentication authentication) {
    if (installation != null) {
      // set required fields
      String user = authentication.getName();
      installation.setCreatedBy(user);
      installation.setModifiedBy(user);
      // add contact and endpoint to installation
      installation.prepare();
      // primary contact and hosting organization key are mandatory
      Contact primary = installation.getPrimaryContact();
      if (primary != null && LegacyResourceUtils.isValid(installation, organizationService)) {
        // persist installation
        UUID key = installationService.create(installation.toApiInstallation());
        // persist contact
        if (key != null) {
          // set primary contact's required fields
          primary.setCreatedBy(user);
          primary.setModifiedBy(user);
          // persist primary contact
          installationService.addContact(key, primary);
          // try to persist FEED endpoint (non-mandatory)
          Endpoint endpoint = installation.getFeedEndpoint();
          if (endpoint != null) {
            // set endpoint's required fields
            endpoint.setCreatedBy(user);
            endpoint.setModifiedBy(user);
            installationService.addEndpoint(key, endpoint);
          }
          LOG.info("IPT installation registered successfully, key={}", key);

          // construct GenericEntity response object expected by IPT
          IptEntityResponse entity = new IptEntityResponse(key.toString());
          // return Response
          return ResponseEntity.status(HttpStatus.CREATED)
              .cacheControl(CacheControl.noCache())
              .body(entity);
        } else {
          LOG.error("IPT installation could not be persisted!");
        }
      } else {
        LOG.error(
            "Mandatory primary contact and/or hosting organization key missing or incomplete!");
      }
    }
    LOG.error("IPT installation registration failed");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .cacheControl(CacheControl.noCache())
        .build();
  }

  /**
   * Update IPT installation, handling incoming request with path /ipt/update/{key}. The primary
   * contact and hosting organization key are mandatory. Only after both the installation and
   * primary contact have been updated is a ResponseEntity with HttpStatus.CREATED returned.
   *
   * @param installationKey installation key (UUID) coming in as path param
   * @param installation IptInstallation with HTTP form parameters
   * @return ResponseEntity with HttpStatus.NO_CONTENT if successful
   */
  @PostMapping(value = "update/{key}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public ResponseEntity<Void> updateIpt(
      @PathVariable("key") UUID installationKey,
      @RequestParam LegacyInstallation installation,
      Authentication authentication) {
    if (installation != null && installationKey != null) {
      // set required fields
      String user = authentication.getName();
      installation.setCreatedBy(user);
      installation.setModifiedBy(user);
      // set key from path parameter
      installation.setKey(installationKey);
      // retrieve existing installation
      Installation existing = installationService.get(installationKey);
      // populate installation with existing primary contact so it gets updated, not duplicated
      installation.setContacts(existing.getContacts());
      // add contact and endpoint to installation
      installation.prepare();
      // ensure the hosting organization exists, and primary contact exists
      Contact contact = installation.getPrimaryContact();
      if (contact != null
          && LegacyResourceUtils.isValidOnUpdate(
              installation, installationService, organizationService)) {
        // update only fields that could have changed
        existing.setModifiedBy(user);
        existing.setTitle(installation.getTitle());
        existing.setDescription(installation.getDescription());
        existing.setType(installation.getType());
        existing.setOrganizationKey(installation.getOrganizationKey());

        // persist changes
        installationService.update(existing);

        // set primary contact's required field(s)
        contact.setModifiedBy(user);
        // add/update primary contact: Contacts are mutable, so try to update if the Contact already
        // exists
        if (contact.getKey() == null) {
          contact.setCreatedBy(user);
          installationService.addContact(installationKey, contact);
        } else {
          installationService.updateContact(installationKey, contact);
        }

        // try to persist FEED endpoint (non-mandatory): Endpoints not mutable, so delete all then
        // re-add
        List<Endpoint> endpoints = installationService.listEndpoints(installationKey);
        for (Endpoint endpoint : endpoints) {
          installationService.deleteEndpoint(installationKey, endpoint.getKey());
        }
        Endpoint endpoint = installation.getFeedEndpoint();
        if (endpoint != null) {
          // set endpoint's required fields
          endpoint.setCreatedBy(user);
          endpoint.setModifiedBy(user);
          installationService.addEndpoint(installationKey, endpoint);
        }

        LOG.info("IPT installation updated successfully, key={}", installationKey);
        return ResponseEntity.noContent().cacheControl(CacheControl.noCache()).build();
      } else {
        LOG.error(
            "Mandatory primary contact and/or hosting organization key missing or incomplete!");
      }
    }
    LOG.error("IPT installation update failed");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .cacheControl(CacheControl.noCache())
        .build();
  }

  /**
   * Register IPT dataset, handling incoming request with path /ipt/resource. The primary contact
   * and publishing organization key are mandatory. Only after both the dataset and primary contact
   * have been persisted is a ResponseEntity with HttpStatus.CREATED returned. </br> Before being
   * persisted, the dataset is the UNSPECIFIED license. This will be replaced (if possible) by the
   * publisher assigned license when the dataset gets crawled the first time. Since IPT 2.2, the IPT
   * EML metadata document always includes a machine readable license. See discussion at
   * https://github.com/gbif/registry/issues/71
   *
   * @param dataset LegacyDataset with HTTP form parameters
   * @return ResponseEntity with HttpStatus.CREATED if successful
   */
  @PostMapping(
      value = "resource",
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
      produces = MediaType.APPLICATION_XML_VALUE)
  public ResponseEntity<IptEntityResponse> registerDataset(
      @RequestParam LegacyDataset dataset, Authentication authentication) {
    if (dataset != null) {
      // set required fields
      String user = authentication.getName();
      dataset.setCreatedBy(user);
      dataset.setModifiedBy(user);
      // if the installation key was missing, try to infer it from publishing organization's
      // installations
      if (dataset.getInstallationKey() == null) {
        dataset.setInstallationKey(inferInstallationKey(dataset));
      }
      // add contact and endpoint(s) to dataset
      dataset.prepare();
      // primary contact, publishing organization key, and installationKey are mandatory
      Contact contact = dataset.getPrimaryContact();
      if (contact != null
          && LegacyResourceUtils.isValid(dataset, organizationService, installationService)) {
        // generate a new GBIF API Dataset instance, derived from the LegacyDataset
        Dataset apiDataset = dataset.toApiDataset();
        // assign "null" license, which will be replaced on the first crawl
        apiDataset.setLicense(License.UNSPECIFIED);
        // persist dataset
        UUID key = datasetService.create(apiDataset);
        // persist contact
        if (key != null) {
          // set primary contact's required fields
          contact.setCreatedBy(user);
          contact.setModifiedBy(user);
          // add primary contact
          datasetService.addContact(key, contact);
          // try to persist endpoint(s) (non-mandatory)
          Endpoint emlEndpoint = dataset.getEmlEndpoint();
          if (emlEndpoint != null) {
            // set endpoint's required fields
            emlEndpoint.setCreatedBy(user);
            emlEndpoint.setModifiedBy(user);
            datasetService.addEndpoint(key, emlEndpoint);
          }
          Endpoint archiveEndpoint = dataset.getArchiveEndpoint();
          if (archiveEndpoint != null) {
            // set endpoint's required fields
            archiveEndpoint.setCreatedBy(user);
            archiveEndpoint.setModifiedBy(user);
            datasetService.addEndpoint(key, archiveEndpoint);
          }
          LOG.info("Dataset registered successfully, key={}", key);
          // construct response object expected by IPT
          IptEntityResponse entity = new IptEntityResponse(key.toString());
          // return Response
          return ResponseEntity.status(HttpStatus.CREATED)
              .cacheControl(CacheControl.noCache())
              .body(entity);
        } else {
          LOG.error("Dataset could not be persisted!");
        }
      } else {
        LOG.error(
            "Mandatory primary contact and/or publishing organization key missing or incomplete!");
      }
    }
    LOG.error("Dataset registration failed");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .cacheControl(CacheControl.noCache())
        .build();
  }

  /**
   * Update IPT Dataset, handling incoming request with path /ipt/resource/{key}. The publishing
   * organization key is mandatory (supplied in the credentials not the parameters). The contacts
   * are preserved from the existing dataset, careful not to duplicate contacts. Only after both the
   * dataset and primary contact have been updated is a Response with Status.OK returned. </br> This
   * update does not change the IPT Dataset license. The license gets updated every time the dataset
   * is crawled using the publisher assigned license found in the EML metadata document. Since IPT
   * 2.2, the IPT EML metadata document always includes a machine readable license.
   *
   * @param datasetKey dataset key (UUID) coming in as path param
   * @param dataset LegacyDataset with HTTP form parameters
   * @return ResponseEntity with HttpStatus.CREATED (201) if successful
   */
  @PostMapping(value = "resource/{key}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public ResponseEntity<Void> updateDataset(
      @PathVariable("key") UUID datasetKey,
      @RequestParam LegacyDataset dataset,
      Authentication authentication) {
    if (dataset != null) {
      // set required fields
      String user = authentication.getName();
      dataset.setCreatedBy(user);
      dataset.setModifiedBy(user);
      dataset.setKey(datasetKey);
      // retrieve existing dataset
      Dataset existing = datasetService.get(datasetKey);
      // populate dataset with existing primary contact so it gets updated, not duplicated
      dataset.setContacts(existing.getContacts());
      // update primary contact, endpoint(s), and type
      dataset.prepare();
      // retrieve existing dataset's installation key if it wasn't provided
      if (dataset.getInstallationKey() == null) {
        dataset.setInstallationKey(existing.getInstallationKey());
      }
      // Dataset can only have 1 installation key, log if the hosting installation is being changed
      // Reason: IPT versions before 2.0.5 didn't supply the parameter iptKey on dataset updates
      else if (dataset.getInstallationKey() != existing.getInstallationKey()) {
        LOG.debug(
            "The dataset's technical installation is being changed from {} to {}",
            dataset.getInstallationKey(),
            existing.getInstallationKey());
      }
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
        existing.setType(dataset.getType());
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

        // try to persist eml and archive endpoint(s) (non-mandatory): Endpoints not mutable, so
        // delete all then re-add
        List<Endpoint> endpoints = datasetService.listEndpoints(datasetKey);
        for (Endpoint endpoint : endpoints) {
          datasetService.deleteEndpoint(datasetKey, endpoint.getKey());
        }
        Endpoint emlEndpoint = dataset.getEmlEndpoint();
        if (emlEndpoint != null) {
          // set endpoint's required fields
          emlEndpoint.setCreatedBy(user);
          emlEndpoint.setModifiedBy(user);
          datasetService.addEndpoint(datasetKey, emlEndpoint);
        }
        Endpoint archiveEndpoint = dataset.getArchiveEndpoint();
        if (archiveEndpoint != null) {
          // set endpoint's required fields
          archiveEndpoint.setCreatedBy(user);
          archiveEndpoint.setModifiedBy(user);
          datasetService.addEndpoint(datasetKey, archiveEndpoint);
        }

        LOG.info("Dataset updated successfully, key={}", datasetKey);
        return ResponseEntity.noContent().cacheControl(CacheControl.noCache()).build();
      } else {
        LOG.error("Request invalid. Dataset missing required fields or using stale keys!");
      }
    }
    LOG.error("Dataset update failed");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .cacheControl(CacheControl.noCache())
        .build();
  }

  /**
   * Delete IPT Dataset, handling incoming request with path /ipt/resource/{key}. Only credentials
   * are mandatory. If deletion is successful, returns Response with Status.OK.
   *
   * @param datasetKey dataset key (UUID) coming in as path param
   * @return ResponseEntity with HttpStatus.OK if successful
   */
  @SuppressWarnings("rawtypes")
  @DeleteMapping("resource/{key}")
  public ResponseEntity deleteDataset(@PathVariable("key") UUID datasetKey) {
    if (datasetKey != null) {
      // retrieve existing dataset
      Dataset existing = datasetService.get(datasetKey);
      if (existing != null) {

        // logically delete dataset. Contacts and endpoints remain, referring to logically deleted
        // dataset
        datasetService.delete(datasetKey);

        LOG.info("Dataset deleted successfully, key={}", datasetKey);
        return ResponseEntity.status(HttpStatus.OK).cacheControl(CacheControl.noCache()).build();

      } else {
        LOG.error("Request invalid. Dataset to be deleted no longer exists!");
      }
    }
    LOG.error("Dataset delete failed");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .cacheControl(CacheControl.noCache())
        .build();
  }

  /**
   * This method tries to infer the Dataset's installation key (when it is missing). Inference is
   * done, using the rule that if the dataset's publishing organization only has 1 installation,
   * this must be the installation that serves the dataset. Conversely, if the organization has more
   * or less than 1 installation, no inference can be made, and null is returned instead.
   *
   * @param dataset LegacyDataset with HTTP form parameters
   * @return inferred installation key, or null if none inferred
   */
  private UUID inferInstallationKey(LegacyDataset dataset) {
    if (dataset.getInstallationKey() == null) {
      UUID organizationKey = dataset.getPublishingOrganizationKey();
      if (organizationKey != null) {
        PagingRequest page = new PagingRequest(0, 2);
        PagingResponse<Installation> response =
            organizationService.installations(organizationKey, page);
        // there is 1, and only 1 installation?
        if (ONE.equals(response.getCount())) {
          Installation installation = response.getResults().get(0);
          if (installation != null) {
            LOG.info(
                "The installation key was inferred successfully from publishing organization's single installation");
            return installation.getKey();
          }
        }
      }
    }
    LOG.error("The installation key could not be inferred from publishing organization!");
    return null;
  }
}
