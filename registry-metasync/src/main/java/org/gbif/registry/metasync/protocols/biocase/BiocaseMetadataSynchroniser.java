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
package org.gbif.registry.metasync.protocols.biocase;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.Citation;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.vocabulary.EndpointType;
import org.gbif.api.vocabulary.InstallationType;
import org.gbif.api.vocabulary.License;
import org.gbif.api.vocabulary.TagName;
import org.gbif.common.parsers.core.ParseResult;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.registry.metasync.api.ErrorCode;
import org.gbif.registry.metasync.api.MetadataException;
import org.gbif.registry.metasync.api.SyncResult;
import org.gbif.registry.metasync.protocols.BaseProtocolHandler;
import org.gbif.registry.metasync.protocols.biocase.model.BiocaseArchive;
import org.gbif.registry.metasync.protocols.biocase.model.BiocaseCount;
import org.gbif.registry.metasync.protocols.biocase.model.InventoryDataset;
import org.gbif.registry.metasync.protocols.biocase.model.NewDatasetInventory;
import org.gbif.registry.metasync.protocols.biocase.model.OldDatasetInventory;
import org.gbif.registry.metasync.protocols.biocase.model.abcd12.SimpleAbcd12Metadata;
import org.gbif.registry.metasync.protocols.biocase.model.abcd206.SimpleAbcd206Metadata;
import org.gbif.registry.metasync.protocols.biocase.model.capabilities.Capabilities;
import org.gbif.registry.metasync.util.Constants;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import org.apache.http.client.HttpClient;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Synchronises metadata from a BioCASe Installation.
 *
 * <p>Every BioCASe Installation can have multiple Endpoints, each of those Endpoints can have
 * multiple Datasets and each of those Datasets can have multiple Endpoints (Web service, ABCD
 * Archive, DwC-A).
 *
 * <p>BioCASe synchronisation happens in the following steps:
 *
 * <ul>
 *   <li>For all endpoints of an Installation make a {@code capabilities} request, followed by a
 *       dataset inventory request, then a metadata and count (to get the number of records) request
 *       for each dataset
 *   <li>The type of inventory request depends on the capabilities. Newer version of BioCASe (3.4
 *       and greater) support a separate inventory request.
 * </ul>
 *
 * <p>Unfortunately BioCASe does not have good (stable) identifiers for Datasets so we need to rely
 * on the Dataset title.
 */
public class BiocaseMetadataSynchroniser extends BaseProtocolHandler {
  private static final Logger LOG = LoggerFactory.getLogger(BiocaseMetadataSynchroniser.class);

  public BiocaseMetadataSynchroniser(HttpClient httpClient) {
    super(httpClient);
  }

  @Override
  public boolean canHandle(Installation installation) {
    return installation.getType() == InstallationType.BIOCASE_INSTALLATION;
  }

  @Override
  public SyncResult syncInstallation(Installation installation, List<Dataset> datasets)
      throws MetadataException {
    try (MDC.MDCCloseable mdc1 =
        MDC.putCloseable("installationKey", installation.getKey().toString())) {
      checkArgument(
          installation.getType() == InstallationType.BIOCASE_INSTALLATION,
          "Only supports BioCASe Installations");

      List<Dataset> added = Lists.newArrayList();
      List<Dataset> deleted = Lists.newArrayList();
      Map<Dataset, Dataset> updated = Maps.newHashMap();

      for (Endpoint endpoint : installation.getEndpoints()) {
        try (MDC.MDCCloseable mdc2 =
            MDC.putCloseable("endpointKey", endpoint.getKey().toString())) {
          LOG.info("Starting synchronization of endpoint: {}", endpoint.getUrl());

          Capabilities capabilities = getCapabilities(endpoint);
          if (capabilities.getPreferredSchema() == null) {
            throw new MetadataException("No preferred schema", ErrorCode.PROTOCOL_ERROR);
          }
          Map<String, InventoryDataset> datasetInventory =
              getDatasetInventory(capabilities, endpoint);
          for (String datasetTitle : datasetInventory.keySet()) {
            Dataset newDataset;

            if (capabilities.getPreferredSchema().equals(Constants.ABCD_12_SCHEMA)) {
              SimpleAbcd12Metadata metadata = get12Metadata(endpoint, datasetTitle, capabilities);
              newDataset = convertToDataset(metadata, endpoint, capabilities);
            } else {
              SimpleAbcd206Metadata metadata = get206Metadata(endpoint, datasetTitle, capabilities);
              newDataset =
                  convertToDataset(
                      metadata, endpoint, capabilities, datasetInventory.get(datasetTitle));
            }

            Long count = getDatasetCount(endpoint, datasetTitle, capabilities);
            if (count != null && count > 0) {
              newDataset.addMachineTag(
                  MachineTag.newInstance(TagName.DECLARED_COUNT, String.valueOf(count)));
            }

            Dataset existingDataset = findDataset(datasetTitle, datasets);
            if (existingDataset == null) {
              added.add(newDataset);
            } else {
              updated.put(existingDataset, newDataset);
            }
          }
        }
      }

      // All Datasets that weren't updated must have been deleted
      for (Dataset dataset : datasets) {
        if (!updated.containsKey(dataset)) {
          deleted.add(dataset);
        }
      }

      return new SyncResult(updated, added, deleted, installation);
    }
  }

  /** Query for the number of records in the dataset, that is, the number we expect to crawl. */
  @Override
  public Long getDatasetCount(Dataset dataset, Endpoint endpoint) throws MetadataException {
    // BioCASe archive endpoints could be passed through, but we need the real BioCASe endpoint.
    // (And it's important to get an up-to-date count, as the archive might be old.)
    if (endpoint.getType() != EndpointType.BIOCASE) {
      Optional<Endpoint> newEndpoint =
          dataset.getEndpoints().stream()
              .filter((e) -> e.getType() == EndpointType.BIOCASE)
              .findAny();
      if (newEndpoint.isPresent()) {
        endpoint = newEndpoint.get();
      } else {
        LOG.warn(
            "No BioCASe endpoint for a BioCASe archive dataset, can't retrieve count. (Have {})",
            endpoint);
        return null;
      }
    }

    Capabilities capabilities = getCapabilities(endpoint);
    if (capabilities.getPreferredSchema() == null) {
      throw new MetadataException("No preferred schema", ErrorCode.PROTOCOL_ERROR);
    }

    Long count = getDatasetCount(endpoint, dataset.getTitle(), capabilities);

    LOG.info("Retrieved count of {}", count);
    return count;
  }

  public URI buildUri(URI url, String parameter, String value) throws MetadataException {
    try {
      return new URIBuilder(url).addParameter(parameter, value).build();
    } catch (URISyntaxException e) {
      throw new MetadataException(e, ErrorCode.OTHER_ERROR);
    }
  }

  /** Does a Capabilities request against the Endpoint. */
  private Capabilities getCapabilities(Endpoint endpoint) throws MetadataException {
    return doHttpRequest(endpoint.getUrl(), newDigester(Capabilities.class));
  }

  /**
   * Tries to get an inventory (list) of Datasets for this BioCASe Endpoint. Depending on the
   * version of the Installation there are two ways to do this.
   */
  private Map<String, InventoryDataset> getDatasetInventory(
      Capabilities capabilities, Endpoint endpoint) throws MetadataException {
    String version = capabilities.getVersions().get("pywrapper");
    if (checkIfSupportsNewInventory(version)) {
      return doNewStyleInventory(endpoint);
    } else {
      return doOldStyleInventory(endpoint, capabilities);
    }
  }

  /** Tries to get a count for this BioCASe Endpoint. */
  private Long getDatasetCount(Endpoint endpoint, String datasetTitle, Capabilities capabilities)
      throws MetadataException {
    String requestParameter =
        TemplateUtils.getBiocaseCountRequest(capabilities.getPreferredSchema(), datasetTitle);
    URI uri = buildUri(endpoint.getUrl(), "request", requestParameter);
    BiocaseCount count = doHttpRequest(uri, newDigester(BiocaseCount.class));
    return count.getCount();
  }

  /**
   * Given a version string in the normal "major.minor.patch" format evaluates whether this version
   * of BioCASe supports the new style inventory request or not. All versions of BioCASe 3.4 and
   * above do support this.
   */
  private boolean checkIfSupportsNewInventory(String version) {
    if (version == null) {
      return false;
    }

    String[] versionParts = version.split("\\.");
    try {
      // Trying to parse the "major" part first, if we succeed and it is greater than 3 we do
      // support the nev inventory
      // style, if it is less than three we don't support it. If it is equal to 3 we need to check
      // the "minor" component
      int majorVersion = Integer.valueOf(versionParts[0]);
      if (majorVersion < 3) {
        return false;
      }
      if (majorVersion > 3) {
        return true;
      }

      // "major" version is 3 but there is no "minor" part
      if (versionParts.length < 2) {
        return false;
      }

      // Check whether the "minor" version is greater than or equal to 4
      int minorVersion = Integer.valueOf(versionParts[1]);
      return minorVersion >= 4;
    } catch (NumberFormatException ignored) {
      return false;
    }
  }

  /**
   * Does a request against the dedicated {@code inventory} endpoint which lists all Datasets that
   * are available as well as all Archives.
   */
  private Map<String, InventoryDataset> doNewStyleInventory(Endpoint endpoint)
      throws MetadataException {
    URI uri = buildUri(endpoint.getUrl(), "inventory", "1");
    NewDatasetInventory inventory = doHttpRequest(uri, newDigester(NewDatasetInventory.class));
    Map<String, InventoryDataset> datasets = new HashMap<>();
    if (inventory == null) return datasets;
    for (InventoryDataset inventoryDataset : inventory.getDatasets()) {
      datasets.put(inventoryDataset.getTitle(), inventoryDataset);
    }
    return datasets;
  }

  /**
   * Does a search request against this Endpoint specially crafted to only find all Dataset titles.
   */
  private Map<String, InventoryDataset> doOldStyleInventory(
      Endpoint endpoint, Capabilities capabilities) throws MetadataException {
    String requestParameter =
        TemplateUtils.getBiocaseInventoryRequest(capabilities.getPreferredSchema());
    URI uri = buildUri(endpoint.getUrl(), "request", requestParameter);
    OldDatasetInventory inventory = doHttpRequest(uri, newDigester(OldDatasetInventory.class));
    Map<String, InventoryDataset> datasets = new HashMap<>();
    if (inventory == null) return datasets;
    for (String title : inventory.getDatasets()) {
      datasets.put(title, null);
    }
    return datasets;
  }

  /** Does a search request against this Endpoint to get all the Metadata for a single Dataset. */
  private SimpleAbcd206Metadata get206Metadata(
      Endpoint endpoint, String dataset, Capabilities capabilities) throws MetadataException {
    String requestParameter =
        TemplateUtils.getBiocaseMetadataRequest(capabilities.getPreferredSchema(), dataset);
    URI uri = buildUri(endpoint.getUrl(), "request", requestParameter);
    return doHttpRequest(uri, newDigester(SimpleAbcd206Metadata.class));
  }

  /** Does a search request against this Endpoint to get all the Metadata for a single Dataset. */
  private SimpleAbcd12Metadata get12Metadata(
      Endpoint endpoint, String dataset, Capabilities capabilities) throws MetadataException {
    String requestParameter =
        TemplateUtils.getBiocaseMetadataRequest(capabilities.getPreferredSchema(), dataset);
    URI uri = buildUri(endpoint.getUrl(), "request", requestParameter);
    return doHttpRequest(uri, newDigester(SimpleAbcd12Metadata.class));
  }

  private Dataset convertToDataset(
      SimpleAbcd206Metadata metadata,
      Endpoint installationEndpoint,
      Capabilities capabilities,
      InventoryDataset inventoryDataset) {
    Dataset dataset = new Dataset();
    dataset.setTitle(metadata.getName());
    dataset.setDescription(metadata.getDetails());
    dataset.setHomepage(metadata.getHomepage());
    dataset.setLogoUrl(metadata.getLogoUrl());

    // Respect publisher issued dataset license.
    // Best practice is to supply a machine readable license specifying license URI and Text
    License license =
        getLicenseParser().parseUriThenTitle(metadata.getLicenseUri(), metadata.getLicenseText());
    if (!license.equals(License.UNSPECIFIED)) {
      LOG.info(
          "Machine readable license {} parsed from License/URI {} & License/Text {}",
          license,
          metadata.getLicenseUri(),
          metadata.getLicenseText());
    }
    // ..alternatively, a machine readable license can be detected in rights
    else {
      ParseResult<License> licenseFromRights = getLicenseParser().parse(metadata.getRights());
      if (licenseFromRights.isSuccessful()) {
        license = licenseFromRights.getPayload();
        LOG.info(
            "Machine readable license {} parsed from rights {}", license, metadata.getRights());
      }
    }
    dataset.setLicense(license);

    // Respect publisher issued DOIs if provided.
    if (DOI.isParsable(metadata.getDatasetGUID())) {
      dataset.setDoi(new DOI(metadata.getDatasetGUID()));
    }

    Citation citation = new Citation();
    citation.setText(metadata.getCitationText());
    dataset.setCitation(citation);

    dataset.setContacts(metadata.getContacts());

    // Add DWC-A and ABCD-A endpoints.
    if (inventoryDataset != null) {
      for (BiocaseArchive archive : inventoryDataset.getArchives()) {
        Endpoint archiveEndpoint = new Endpoint();
        archiveEndpoint.setUrl(archive.getArchiveUrl());
        archiveEndpoint.addMachineTag(
            MachineTag.newInstance(
                TagName.ARCHIVE_ORIGIN, InstallationType.BIOCASE_INSTALLATION.name()));

        if (archive.getRowType() != null
            && archive.getRowType().toString().equals(DwcTerm.Occurrence.qualifiedName())) {
          LOG.info("Found BioCASe occurrence DWCA {}", archive);
          archiveEndpoint.setType(EndpointType.DWC_ARCHIVE);
        } else {
          LOG.info("Found BioCASe XML archive (or non-occurrence DWCA) {}", archive);
          archiveEndpoint.setType(EndpointType.BIOCASE_XML_ARCHIVE);
        }
        dataset.addEndpoint(archiveEndpoint);
      }
    }

    // Add BioCASe endpoint.
    Endpoint endpoint = new Endpoint();
    endpoint.setType(EndpointType.BIOCASE);
    endpoint.setUrl(installationEndpoint.getUrl());
    endpoint.addMachineTag(
        MachineTag.newInstance(TagName.CONCEPTUAL_SCHEMA, capabilities.getPreferredSchema()));

    dataset.addEndpoint(endpoint);
    return dataset;
  }

  private Dataset convertToDataset(
      SimpleAbcd12Metadata metadata, Endpoint installationEndpoint, Capabilities capabilities) {
    Dataset dataset = new Dataset();
    dataset.setTitle(metadata.getCode());
    dataset.setDescription(metadata.getDescription());
    dataset.setHomepage(metadata.getHomepage());
    dataset.setLogoUrl(metadata.getLogoUrl());

    // Respect publisher issued dataset license, it can be detected in rights
    ParseResult<License> licenseFromRights = getLicenseParser().parse(metadata.getRights());
    if (licenseFromRights.isSuccessful()) {
      License license = licenseFromRights.getPayload();
      LOG.info("Machine readable license {} parsed from rights {}", license, metadata.getRights());
      dataset.setLicense(license);
    }

    Citation citation = new Citation();
    citation.setText(metadata.getCitationText());
    dataset.setCitation(citation);

    dataset.setContacts(metadata.getContacts());

    Endpoint endpoint = new Endpoint();
    endpoint.setType(EndpointType.BIOCASE);
    endpoint.setUrl(installationEndpoint.getUrl());
    endpoint.addMachineTag(
        MachineTag.newInstance(TagName.CONCEPTUAL_SCHEMA, capabilities.getPreferredSchema()));

    dataset.addEndpoint(endpoint);
    return dataset;
  }

  /**
   * Tries to find a matching Dataset in the list of provided Datasets by looking at the title.
   *
   * @return the matching Dataset or {@code null} if it could not be found
   */
  @Nullable
  private Dataset findDataset(String datasetTitle, Iterable<Dataset> datasets) {
    for (Dataset dataset : datasets) {
      if (dataset.getTitle().equals(datasetTitle)) {
        return dataset;
      }
    }

    return null;
  }
}
