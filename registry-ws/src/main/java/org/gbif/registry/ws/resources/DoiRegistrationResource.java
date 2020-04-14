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

import org.gbif.api.annotation.NullToNotFound;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.api.model.common.DoiStatus;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.AlternateIdentifiers;
import org.gbif.doi.service.InvalidMetadataException;
import org.gbif.doi.service.datacite.DataCiteValidator;
import org.gbif.registry.doi.generator.DoiGenerator;
import org.gbif.registry.doi.registration.DoiRegistration;
import org.gbif.registry.doi.registration.DoiRegistrationService;
import org.gbif.registry.domain.doi.DoiType;
import org.gbif.registry.persistence.mapper.DoiMapper;
import org.gbif.ws.WebApplicationException;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Resource class that exposes services to interact with DOI issued thru GBIF and DataCite. */
@Validated
@RestController
@RequestMapping("doi")
public class DoiRegistrationResource implements DoiRegistrationService {

  private static final Logger LOG = LoggerFactory.getLogger(DoiRegistrationResource.class);

  private final DoiGenerator doiGenerator;
  private final DoiMapper doiMapper;

  public DoiRegistrationResource(DoiGenerator doiGenerator, DoiMapper doiMapper) {
    this.doiGenerator = doiGenerator;
    this.doiMapper = doiMapper;
  }

  /** Generates a new DOI based on the DoiType. */
  @PostMapping("gen/{type}")
  @Override
  public DOI generate(@PathVariable DoiType type) {
    checkIsUserAuthenticated();
    return genDoiByType(type);
  }

  /** Retrieves the DOI information. */
  @GetMapping(value = "{prefix}/{suffix}", produces = MediaType.APPLICATION_JSON_VALUE)
  @NullToNotFound("/doi/{prefix}/{suffix}")
  @Override
  public DoiData get(@PathVariable String prefix, @PathVariable String suffix) {
    return doiMapper.get(new DOI(prefix, suffix));
  }

  /** Deletes an existent DOI. */
  @DeleteMapping("{prefix}/{suffix}")
  @Override
  public void delete(@PathVariable String prefix, @PathVariable String suffix) {
    LOG.info("Deleting DOI {} {}", prefix, suffix);
    doiGenerator.delete(new DOI(prefix, suffix));
  }

  /**
   * Register a new DOI, if the registration object doesn't contain a DOI a new DOI is generated.
   */
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  public DOI register(@RequestBody DoiRegistration doiRegistration) {
    return createOrUpdate(
        doiRegistration,
        doiRegistrationToRegister ->
            // Persist the DOI
            Optional.ofNullable(doiRegistrationToRegister.getDoi())
                .ifPresent(
                    doi -> {
                      Optional.ofNullable(doiMapper.get(doi))
                          .ifPresent(
                              doiData -> {
                                // if DOI is not NEW throw an exception
                                if (DoiStatus.NEW != doiData.getStatus()) {
                                  throw new WebApplicationException(
                                      "Doi already exists", HttpStatus.BAD_REQUEST);
                                }
                              });
                      doiMapper.update(doi, doiMapper.get(doi), doiRegistration.getMetadata());
                    }));
  }

  /**
   * Register a new DOI, if the registration object doesn't contain a DOI a new DOI is generated.
   */
  @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  public DOI update(@RequestBody DoiRegistration doiRegistration) {
    return createOrUpdate(
        doiRegistration,
        existingDoiRegistration ->
            // Update the DOI
            Optional.ofNullable(existingDoiRegistration.getDoi())
                .ifPresent(
                    doi -> {
                      Optional.ofNullable(doiMapper.get(doi))
                          .ifPresent(
                              doiData -> {
                                // if DOI is not NEW throw an exception
                                if (DoiStatus.DELETED == doiData.getStatus()) {
                                  throw new WebApplicationException(
                                      "Doi already exists", HttpStatus.BAD_REQUEST);
                                }
                              });
                      doiMapper.update(doi, doiMapper.get(doi), doiRegistration.getMetadata());
                    }));
  }

  private DOI createOrUpdate(DoiRegistration doiRegistration, Consumer<DoiRegistration> preFilter) {
    checkIsUserAuthenticated();
    try {
      preFilter.accept(doiRegistration);
      // registration contains a DOI already
      DOI doi =
          doiRegistration.getDoi() == null
              ? genDoiByType(doiRegistration.getType())
              : doiRegistration.getDoi();
      // Ensures that the metadata contains the DOI as an alternative identifier
      DataCiteMetadata dataCiteMetadata = DataCiteValidator.fromXml(doiRegistration.getMetadata());
      DataCiteMetadata metadata =
          DataCiteMetadata.copyOf(dataCiteMetadata)
              .withAlternateIdentifiers(
                  addDoiToIdentifiers(dataCiteMetadata.getAlternateIdentifiers(), doi))
              .build();

      // handle registration
      if (DoiType.DATA_PACKAGE == doiRegistration.getType()) {
        doiGenerator.registerDataPackage(doi, metadata);
      } else if (DoiType.DOWNLOAD == doiRegistration.getType()) {
        doiGenerator.registerDownload(doi, metadata, doiRegistration.getKey());
      } else if (DoiType.DATASET == doiRegistration.getType()) {
        doiGenerator.registerDataset(doi, metadata, UUID.fromString(doiRegistration.getKey()));
      }

      LOG.info("DOI registered/updated {}", doi.getDoiName());
      return doi;
    } catch (InvalidMetadataException | JAXBException ex) {
      LOG.info("Error registering/updating DOI", ex);
      throw new WebApplicationException(
          "Invalid metadata, error registering/updating DOI", HttpStatus.BAD_REQUEST);
    }
  }

  /** Ensures that the DOI is included as AlternateIdentifier. */
  private static AlternateIdentifiers addDoiToIdentifiers(
      AlternateIdentifiers alternateIdentifiers, DOI doi) {
    AlternateIdentifiers.Builder<Void> builder = AlternateIdentifiers.builder();
    if (alternateIdentifiers != null && alternateIdentifiers.getAlternateIdentifier() != null) {
      builder.addAlternateIdentifier(
          alternateIdentifiers.getAlternateIdentifier().stream()
              .filter(
                  identifier ->
                      !identifier.getValue().equals(doi.getDoiName())
                          && !identifier.getAlternateIdentifierType().equalsIgnoreCase("DOI"))
              .collect(Collectors.toList()));
    }
    builder.addAlternateIdentifier(
        AlternateIdentifiers.AlternateIdentifier.builder()
            .withValue(doi.getDoiName())
            .withAlternateIdentifierType("DOI")
            .build());
    return builder.build();
  }

  /** Generates DOI based on the DoiType. */
  private DOI genDoiByType(DoiType doiType) {
    if (DoiType.DATA_PACKAGE == doiType) {
      return doiGenerator.newDataPackageDOI();
    } else if (DoiType.DOWNLOAD == doiType) {
      return doiGenerator.newDownloadDOI();
    } else {
      return doiGenerator.newDatasetDOI();
    }
  }

  // TODO: 03/03/2020 replace with annotation Secured(USER)
  /** Check that the user is authenticated. */
  private void checkIsUserAuthenticated() {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.getName() == null)
      throw new WebApplicationException(
          "Authorization is required to perform this operation on DOI", HttpStatus.UNAUTHORIZED);
  }
}
