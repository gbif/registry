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
package org.gbif.registry.metasync.protocols.tapir;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.vocabulary.EndpointType;
import org.gbif.api.vocabulary.InstallationType;
import org.gbif.api.vocabulary.License;
import org.gbif.api.vocabulary.TagName;
import org.gbif.api.vocabulary.TagNamespace;
import org.gbif.common.parsers.core.ParseResult;
import org.gbif.registry.metasync.api.ErrorCode;
import org.gbif.registry.metasync.api.MetadataException;
import org.gbif.registry.metasync.api.SyncResult;
import org.gbif.registry.metasync.protocols.BaseProtocolHandler;
import org.gbif.registry.metasync.protocols.tapir.model.capabilities.Capabilities;
import org.gbif.registry.metasync.protocols.tapir.model.capabilities.Schema;
import org.gbif.registry.metasync.protocols.tapir.model.metadata.TapirContact;
import org.gbif.registry.metasync.protocols.tapir.model.metadata.TapirMetadata;
import org.gbif.registry.metasync.protocols.tapir.model.metadata.TapirRelatedEntity;
import org.gbif.registry.metasync.protocols.tapir.model.search.TapirSearch;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.http.client.HttpClient;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Synchronises metadata from a TAPIR Installation.
 *
 * <p>Every TAPIR Installation can have multiple Endpoints. Each of these Endpoints represents a
 * single Dataset and each of the Datasets can have a single Endpoint. Note: TAPIR supports Archives
 * but no one seems to be using them so they are not supported here.
 *
 * <p>The process is as follows:
 *
 * <p>
 *
 * <ol>
 *   <li>For every Endpoint do a {@code capabilities} as well as a {@code metadata} request
 *   <li>Convert the data into a {@link Dataset} object
 *   <li>When we have all new {@code Dataset} objects try to map them to existing ones using the
 *       {@code local id} (which is the last part of the URL)
 * </ol>
 *
 * Note: If there is an exception during processing one of the Endpoints the whole synchronisation
 * process will be aborted. I'm doing this to prevent inconsistencies.
 */
public class TapirMetadataSynchroniser extends BaseProtocolHandler {
  private static final Logger LOG = LoggerFactory.getLogger(TapirMetadataSynchroniser.class);

  // TAPIR supports multiple output formats, so we map a namespace to a search template with
  // appropriate output model
  // The maps are ordered by output model, with the preferred (highest priority) output for DwC/ABCD
  // coming first
  private static final LinkedListMultimap<String, String> ORDERED_TEMPLATE_MAPPING =
      LinkedListMultimap.create();

  static {
    // DwC
    ORDERED_TEMPLATE_MAPPING.put(
        "http://rs.tdwg.org/dwc/dwcore/",
        "http://rs.gbif.org/templates/tapir/dwc/1.4/sci_name_range.xml");
    ORDERED_TEMPLATE_MAPPING.put(
        "http://rs.tdwg.org/dwc/geospatial/",
        "http://rs.gbif.org/templates/tapir/dwc/1.4/sci_name_range.xml");
    ORDERED_TEMPLATE_MAPPING.put(
        "http://rs.tdwg.org/dwc/curatorial/",
        "http://rs.gbif.org/templates/tapir/dwc/1.4/sci_name_range.xml");
    ORDERED_TEMPLATE_MAPPING.put(
        "http://rs.tdwg.org/dwc/terms/",
        "http://rs.tdwg.org/tapir/cs/dwc/terms/2009-09-23/template/dwc_sci_name_range.xml");
    ORDERED_TEMPLATE_MAPPING.put(
        "http://digir.net/schema/conceptual/darwin/2003/1.0",
        "http://rs.gbif.org/templates/tapir/dwc/1.0/sci_name_range.xml");
    ORDERED_TEMPLATE_MAPPING.put(
        "http://www.tdwg.org/schemas/abcd/2.06",
        "http://rs.gbif.org/templates/tapir/abcd/206/sci_name_range.xml");
    // ABCD (Default to 2.0.6 whenever 2.0.5 is encountered)
    ORDERED_TEMPLATE_MAPPING.put(
        "http://www.tdwg.org/schemas/abcd/2.05",
        "http://rs.gbif.org/templates/tapir/abcd/206/sci_name_range.xml");
    ORDERED_TEMPLATE_MAPPING.put(
        "http://www.tdwg.org/schemas/abcd/1.2",
        "http://rs.gbif.org/templates/tapir/abcd/1.2/sci_name_range.xml");
  }

  public TapirMetadataSynchroniser(HttpClient httpClient) {
    super(httpClient);
  }

  @Override
  public boolean canHandle(Installation installation) {
    return installation.getType() == InstallationType.TAPIR_INSTALLATION;
  }

  @Override
  public SyncResult syncInstallation(Installation installation, List<Dataset> datasets)
      throws MetadataException {
    checkArgument(
        installation.getType() == InstallationType.TAPIR_INSTALLATION,
        "Only supports TAPIR Installations");

    List<Dataset> added = Lists.newArrayList();
    List<Dataset> deleted = Lists.newArrayList();
    Map<Dataset, Dataset> updated = Maps.newHashMap();

    // This metadata will be used to update the Installation itself
    TapirMetadata updaterMetadata = null;

    for (Endpoint endpoint : installation.getEndpoints()) {
      String localId = getLocalId(endpoint);

      Capabilities capabilities = getCapabilities(endpoint);
      if (capabilities == null) {
        throw new MetadataException(
            "Did not receive a valid Capabilities response for [" + endpoint.getKey() + "]",
            ErrorCode.PROTOCOL_ERROR);
      }

      TapirMetadata metadata = getTapirMetadata(endpoint);

      updateInstallationEndpoint(metadata, endpoint);

      String outputModelTemplate = getPreferredOutputModelTemplate(capabilities.getSchemas());
      URI searchRequestUrl = buildSearchRequestUrl(endpoint, outputModelTemplate);
      TapirSearch search = getTapirSearch(searchRequestUrl);

      Dataset newDataset = convertToDataset(capabilities, metadata, search);
      Dataset existingDataset = findDataset(localId, datasets);
      if (existingDataset == null) {
        added.add(newDataset);
      } else {
        updated.put(existingDataset, newDataset);
      }

      updaterMetadata = metadata;
    }

    // All Datasets that weren't updated must have been deleted
    for (Dataset dataset : datasets) {
      if (!updated.containsKey(dataset)) {
        deleted.add(dataset);
      }
    }

    updateInstallation(installation, updaterMetadata);

    return new SyncResult(updated, added, deleted, installation);
  }

  /**
   * Query for the number of records in the dataset, that is, the number we expect to crawl.
   *
   * <p>There are only four working TaPIR datasets, so I'm not implementing this (2018-05).
   */
  @Override
  public Long getDatasetCount(Dataset dataset, Endpoint endpoint) {
    LOG.info("Not implemented, returning null");
    return null;
  }

  /**
   * Gets the <em>local id</em> from the Endpoint. This is the last part of the URL and the only
   * uniquely identifying piece for a TAPIR Dataset.
   */
  private String getLocalId(Endpoint endpoint) throws MetadataException {
    String[] split = endpoint.getUrl().toASCIIString().split("/");

    if (split.length < 2) {
      throw new MetadataException(
          "Could not find local Id for [" + endpoint.getUrl() + "]", ErrorCode.OTHER_ERROR);
    }

    return split[split.length - 1];
  }

  /** Does a Capabilities request against the TAPIR Endpoint. */
  private Capabilities getCapabilities(Endpoint endpoint) throws MetadataException {
    URI uri;
    try {
      uri = new URIBuilder(endpoint.getUrl()).addParameter("op", "capabilities").build();
    } catch (URISyntaxException e) {
      throw new MetadataException(e, ErrorCode.OTHER_ERROR);
    }

    return doHttpRequest(uri, newDigester(Capabilities.class));
  }

  /** Does a Metadata request against the TAPIR Endpoint. */
  private TapirMetadata getTapirMetadata(Endpoint endpoint) throws MetadataException {
    return doHttpRequest(endpoint.getUrl(), newDigester(TapirMetadata.class));
  }

  /**
   * Does a Search request against the TAPIR Endpoint.
   *
   * @param request request URI to search request used to retrieve number of records
   * @return TapirSearch, or null if the response could not be parsed into a TapirSearch
   * @throws MetadataException in case anything goes wrong during the request
   */
  private TapirSearch getTapirSearch(URI request) throws MetadataException {
    return doHttpRequest(request, newDigester(TapirSearch.class));
  }

  /** Updates the Endpoint of the Installation that we're currently working on. */
  private void updateInstallationEndpoint(TapirMetadata metadata, Endpoint endpoint) {
    endpoint.setDescription(metadata.getDescriptions().toString());
  }

  /** Converts the Capabilities and Metadata response from TAPIR into a GBIF Dataset. */
  private Dataset convertToDataset(
      Capabilities capabilities, TapirMetadata metadata, @Nullable TapirSearch search) {
    Dataset dataset = new Dataset();
    dataset.setTitle(metadata.getTitles().toString());
    dataset.setDescription(metadata.getDescriptions().toString());
    dataset.setHomepage(metadata.getAccessPoint());
    dataset.setLanguage(metadata.getDefaultLanguage());

    // Respect publisher issued dataset license, possibly provided in rights
    if (!metadata.getRights().getValues().isEmpty()) {
      for (String value : metadata.getRights().getValues().values()) {
        ParseResult<License> licenseFromRights = getLicenseParser().parse(value);
        if (licenseFromRights.isSuccessful()) {
          License license = licenseFromRights.getPayload();
          LOG.info("Machine readable license {} parsed from rights: {}", license, value);
          dataset.setLicense(license);
          break;
        }
      }
    }

    // Respect publisher issued DOIs if provided.
    if (DOI.isParsable(metadata.getIdentifier())) {
      dataset.setDoi(new DOI(metadata.getIdentifier()));
    }

    List<Contact> contacts = Lists.newArrayList();
    for (TapirRelatedEntity tapirRelatedEntity : metadata.getRelatedEntities()) {
      for (TapirContact tapirContact : tapirRelatedEntity.getContacts()) {
        Contact contact = new Contact();
        for (String r : tapirContact.getRoles()) {
          contact.addPosition(r);
        }
        contact.setFirstName(tapirContact.getFullName());
        contact.addPhone(tapirContact.getTelephone());
        contact.addEmail(tapirContact.getEmail());
        contact.setDescription(tapirContact.getTitle());
        contacts.add(contact);
      }
    }
    dataset.setContacts(contacts);

    Endpoint endpoint = new Endpoint();
    endpoint.setType(EndpointType.TAPIR);
    endpoint.setDescription(metadata.getTitles().toString());
    endpoint.setUrl(metadata.getAccessPoint());
    dataset.addEndpoint(endpoint);

    // This makes sure that we only save one content namespace/conceptual schema to the machine
    // tags. This is done
    // by picking the first match from the ORDERED_TEMPLATE_MAPPING
    boolean found = false;
    for (Map.Entry<String, String> entry : ORDERED_TEMPLATE_MAPPING.entries()) {
      if (found) break;
      for (Schema schema : capabilities.getSchemas()) {
        if (schema.getNamespace().toASCIIString().equalsIgnoreCase(entry.getKey())) {
          dataset.addMachineTag(
              MachineTag.newInstance(
                  TagName.CONCEPTUAL_SCHEMA, schema.getNamespace().toASCIIString()));
          found = true;
          break;
        }
      }
    }
    if (!found) {
      LOG.warn("Could not find a supported conceptual schema");
    }

    // if search response brought back the number of records, add corresponding tag
    if (search != null && search.getNumberOfRecords() != 0) {
      dataset.addMachineTag(
          MachineTag.newInstance(
              TagName.DECLARED_COUNT, String.valueOf(search.getNumberOfRecords())));
    }

    return dataset;
  }

  /**
   * Tries to find a matching Dataset in the list of provided Datasets.
   *
   * @return the matching Dataset or {@code null} if it could not be found
   */
  @Nullable
  private Dataset findDataset(String localId, Iterable<Dataset> datasets) throws MetadataException {
    for (Dataset dataset : datasets) {
      for (Endpoint endpoint : dataset.getEndpoints()) {
        if (localId.equals(getLocalId(endpoint))) {
          return dataset;
        }
      }
    }
    return null;
  }

  /**
   * Updates the Installation with the data that is universal across all Datasets. As the
   * Installation itself doesn't have any Metadata Endpoint there is very little information we can
   * extract.
   */
  /*
   * There don't seem to be any of these in the Registry database.
   */
  private void updateInstallation(Installation installation, TapirMetadata updaterMetadata) {
    installation.addMachineTag(
        MachineTag.newInstance(
            TagNamespace.GBIF_METASYNC.getNamespace(),
            "version",
            updaterMetadata.getSoftwareVersion()));
    installation.addMachineTag(
        MachineTag.newInstance(
            TagNamespace.GBIF_METASYNC.getNamespace(),
            "software_name",
            updaterMetadata.getSoftwareName()));
  }

  /**
   * Construct a map of the atoms for the TAPIR search request.
   *
   * @param outputModelTemplate output model template URI
   * @return The atoms for the TAPIR search request (always in the same order which is important for
   *     testing) used only to return the count.
   */
  private Map<String, String> buildTapirSearchRequestParameters(String outputModelTemplate) {
    Map<String, String> params = Maps.newLinkedHashMap();
    params.put("op", "s");
    params.put("t", outputModelTemplate);
    params.put("count", "true");

    params.put("start", "0");
    params.put("limit", "1");

    params.put("lower", "AAA");
    params.put("upper", "zzz");

    return params;
  }

  /**
   * Build the TAPIR search request URL that will return the number of records count. It has
   * scientific name range AAA-zzz, offset 0, limit 1, and count equal to true.
   *
   * @param endpoint Endpoint
   * @param outputModelTemplate output model template to use in request
   * @return TAPIR search request URL
   */
  public URI buildSearchRequestUrl(Endpoint endpoint, String outputModelTemplate) {
    try {
      URIBuilder uriBuilder = new URIBuilder(endpoint.getUrl());
      for (Map.Entry<String, String> paramEntry :
          buildTapirSearchRequestParameters(outputModelTemplate).entrySet()) {
        uriBuilder.addParameter(paramEntry.getKey(), paramEntry.getValue());
      }
      return uriBuilder.build();
    } catch (URISyntaxException e) {
      // It's coming from a valid URL so it shouldn't ever throw this, so we swallow this
      return null;
    }
  }

  /**
   * Gets the preferred output model template to use for requests to the TAPIR endpoint using the
   * list of supported namespaces coming from the capabilities response's list of Schemas. </br>
   * E.g. schema with namespace ABCD 2.06 is preferred over ABCD 1.2, so the output model
   * corresponding to ABCD 2.06 is returned.
   *
   * @param schemas TAPIR capabilities list of Schemas
   * @return the preferred output model template
   * @throws MetadataException if no preferred output model template was found
   */
  private String getPreferredOutputModelTemplate(Iterable<Schema> schemas)
      throws MetadataException {
    // iterate through ordered namespace -> output model mappings
    for (Map.Entry<String, String> entry : ORDERED_TEMPLATE_MAPPING.entries()) {
      // was this namespace listed in the capabilities?
      for (Schema schema : schemas) {
        if (entry.getKey().equalsIgnoreCase(schema.getNamespace().toString())) {
          // return the output model template, corresponding to the highest priority namespace
          // encountered
          return entry.getValue();
        }
      }
    }
    throw new MetadataException(
        "No namespace found matching a DwC or ABCD output model", ErrorCode.PROTOCOL_ERROR);
  }
}
