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
package org.gbif.registry.metasync.protocols.digir;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.Citation;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.util.MachineTagUtils;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.EndpointType;
import org.gbif.api.vocabulary.InstallationType;
import org.gbif.api.vocabulary.TagName;
import org.gbif.api.vocabulary.TagNamespace;
import org.gbif.registry.metasync.api.ErrorCode;
import org.gbif.registry.metasync.api.MetadataException;
import org.gbif.registry.metasync.api.SyncResult;
import org.gbif.registry.metasync.protocols.BaseProtocolHandler;
import org.gbif.registry.metasync.protocols.digir.model.DigirContact;
import org.gbif.registry.metasync.protocols.digir.model.DigirMetadata;
import org.gbif.registry.metasync.protocols.digir.model.DigirResource;
import org.gbif.registry.metasync.util.Constants;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * DiGIR synchronisation happens by issuing a metadata request to the single endpoint that the
 * installation should have. The response contains a list of resources which we parse to GBIF {@link
 * Dataset} objects, each having a single Endpoint.
 */
public class DigirMetadataSynchroniser extends BaseProtocolHandler {
  private static final Logger LOG = LoggerFactory.getLogger(DigirMetadataSynchroniser.class);

  // keyword used to identify if an endpoint is of type DIGIR_MANIS
  private static final String MANIS_KEYWORD = "manis";
  // only schemaLocation of type DIGIR_MANIS not containing the word "manis"
  private static final String MANIS_SCHEMA_LOCATION =
      "http://bnhm.berkeley.edu/DwC/bnhm_dc2_schema.xsd";

  public DigirMetadataSynchroniser(HttpClient httpClient) {
    super(httpClient);
  }

  @Override
  public boolean canHandle(Installation installation) {
    return installation.getType() == InstallationType.DIGIR_INSTALLATION;
  }

  @Override
  public SyncResult syncInstallation(Installation installation, List<Dataset> datasets)
      throws MetadataException {
    checkArgument(
        installation.getType() == InstallationType.DIGIR_INSTALLATION,
        "Only supports DiGIR Installations");

    if (installation.getEndpoints().size() != 1) {
      throw new MetadataException(
          "A DiGIR Installation should only ever have one Endpoint, this one has ["
              + installation.getEndpoints().size()
              + "]",
          ErrorCode.OTHER_ERROR);
    }
    Endpoint endpoint = installation.getEndpoints().get(0);

    DigirMetadata metadata = getDigirMetadata(endpoint);
    updateInstallation(metadata, installation);
    updateInstallationEndpoint(metadata, endpoint);
    return mapToDatasets(metadata, datasets, endpoint.getUrl(), installation);
  }

  /** Query for the number of records in the dataset, that is, the number we expect to crawl. */
  @Override
  public Long getDatasetCount(Dataset dataset, Endpoint endpoint) throws MetadataException {
    try {
      DigirMetadata metadata = getDigirMetadata(endpoint);

      String code = MachineTagUtils.firstTag(dataset, TagName.DIGIR_CODE).getValue();

      int numberOfRecords =
          metadata.getResources().stream()
              .filter((r) -> code.equals(r.getCode()))
              .findFirst()
              .map(DigirResource::getNumberOfRecords)
              .orElse(0);

      LOG.info("Retrieved count of {}", numberOfRecords);

      if (numberOfRecords != 0) {
        return (long) numberOfRecords;
      }

      return null;
    } catch (Exception e) {
      throw new MetadataException(
          "Unable to retrieve count of DiGIR dataset [" + dataset.getKey() + "]",
          e,
          ErrorCode.OTHER_ERROR);
    }
  }

  private DigirMetadata getDigirMetadata(Endpoint endpoint) throws MetadataException {
    return doHttpRequest(endpoint.getUrl(), newDigester(DigirMetadata.class));
  }

  /** Updates the Installation in in place with all the data gathered from the Endpoint. */
  private void updateInstallation(DigirMetadata metadata, Installation installation) {
    installation.setContacts(
        matchContacts(
            installation.getContacts(),
            convertToRegistryContacts(metadata.getHost().getContacts())));

    installation.setDescription(metadata.getHost().getDescription());

    installation.addMachineTag(
        MachineTag.newInstance(TagName.DIGIR_CODE, metadata.getHost().getCode()));
    installation.addMachineTag(
        MachineTag.newInstance(
            TagNamespace.GBIF_METASYNC.getNamespace(),
            Constants.INSTALLATION_VERSION,
            metadata.getImplementation()));
  }

  /** Updates the single Endpoint that a DiGIR Installation has. */
  private void updateInstallationEndpoint(DigirMetadata metadata, Endpoint endpoint) {
    endpoint.setDescription(metadata.getHost().getDescription());
  }

  /**
   * Maps the resources we got from the metadata response to Datasets that are currently hosted by
   * this Installation. We identify Datasets by the {@code code} attribute that we're getting from
   * the metadata response. We're saving this code on the Dataset itself as a machine tag.
   */
  private SyncResult mapToDatasets(
      DigirMetadata metadata, Iterable<Dataset> datasets, URI url, Installation installation) {
    List<Dataset> added = Lists.newArrayList();
    List<Dataset> deleted = Lists.newArrayList();
    Map<Dataset, Dataset> updated = Maps.newHashMap();

    // Maps currently existing DiGIR codes to the Datasets from our Registry that use those codes
    Map<String, Dataset> codeMap = Maps.newHashMap();
    for (Dataset dataset : datasets) {
      MachineTagUtils.firstTag(
          dataset, TagName.DIGIR_CODE, (mt) -> codeMap.put(mt.getValue(), dataset));
    }

    // Sort in either updated or added Datasets using the just built Map
    for (DigirResource resource : metadata.getResources()) {
      Dataset newDataset = convertToDataset(resource, url);
      if (codeMap.containsKey(resource.getCode())) {
        updated.put(codeMap.get(resource.getCode()), newDataset);
      } else {
        added.add(newDataset);
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

  /** Converts a DiGIR resource to a GBIF Dataset. */
  private Dataset convertToDataset(DigirResource resource, URI url) {
    Dataset dataset = new Dataset();
    dataset.setTitle(resource.getName());
    dataset.setDescription(resource.getDescription());

    // We're only using the very first related URI even though there might be more
    if (!resource.getRelatedInformation().isEmpty()) {
      dataset.setHomepage(resource.getRelatedInformation().iterator().next());
    }
    dataset.setCitation(new Citation(resource.getCitation(), null, false));
    dataset.setRights(resource.getUseRestrictions());
    dataset.setContacts(convertToRegistryContacts(resource.getContacts()));
    dataset.addMachineTag(MachineTag.newInstance(TagName.DIGIR_CODE, resource.getCode()));

    // Respect publisher issued DOIs if provided.
    if (DOI.isParsable(resource.getCode())) {
      dataset.setDoi(new DOI(resource.getCode()));
    }

    if (resource.getNumberOfRecords() != 0) {
      dataset.addMachineTag(
          MachineTag.newInstance(
              TagName.DECLARED_COUNT, String.valueOf(resource.getNumberOfRecords())));
    }

    if (resource.getMaxSearchResponseRecords() != 0) {
      dataset.addMachineTag(
          MachineTag.newInstance(
              TagName.MAX_SEARCH_RESPONSE_RECORDS,
              String.valueOf(resource.getMaxSearchResponseRecords())));
    }

    if (resource.getDateLastUpdated() != null) {
      dataset.addMachineTag(
          MachineTag.newInstance(
              TagName.DATE_LAST_UPDATED, resource.getDateLastUpdated().toString()));
    }

    for (Map.Entry<String, URI> entry : resource.getConceptualSchemas().entrySet()) {
      dataset.addMachineTag(
          MachineTag.newInstance(TagName.CONCEPTUAL_SCHEMA, entry.getValue().toASCIIString()));
    }

    // Each DiGIR Dataset has exactly one Endpoint, we create and populate it here
    Endpoint endpoint = new Endpoint();
    endpoint.setDescription(resource.getName());
    endpoint.setUrl(url);
    // normal DiGIR vs MaNIS DiGIR?
    endpoint.setType(determineEndpointType(resource.getConceptualSchemas()));
    dataset.addEndpoint(endpoint);

    return dataset;
  }

  /**
   * Iterates through the resource's map of namespace (conceptualSchemas) / schemaLocation key value
   * pairs. If a DIGIR_MANIS endpoint is found in the list, the EndpointType is equal to
   * DiGIR_MANIS. A DiGIR_MANIS endpoint is identified, by checking if the schemaLocation 1)contains
   * the word "manis" or 2) is equal to "http://bnhm.berkeley.edu/DwC/bnhm_dc2_schema.xsd".
   *
   * @param conceptualSchemas map with namespace (conceptualSchemas), schemaLocation key value pairs
   * @return endpoint type, defaulting to (normal) DiGIR
   */
  EndpointType determineEndpointType(Map<String, URI> conceptualSchemas) {
    for (URI schemaLocation : conceptualSchemas.values()) {
      if (schemaLocation.toString().equalsIgnoreCase(MANIS_SCHEMA_LOCATION)
          || schemaLocation.toString().toLowerCase().contains(MANIS_KEYWORD)) {
        return EndpointType.DIGIR_MANIS;
      }
    }
    return EndpointType.DIGIR;
  }

  /** Converts a list of DiGIR contacts to GBIF {@link Contact} objects. */
  private List<Contact> convertToRegistryContacts(Iterable<DigirContact> contacts) {
    List<Contact> resultList = Lists.newArrayList();
    for (DigirContact contact : contacts) {
      resultList.add(convertToRegistryContact(contact));
    }
    return resultList;
  }

  /** Converts a single DiGIR contact to a GBIF {@link Contact} */
  private Contact convertToRegistryContact(DigirContact digirContact) {
    Contact contact = new Contact();
    contact.setFirstName(trimToNull(digirContact.getName()));
    contact.addPosition(trimToNull(digirContact.getTitle()));
    contact.addEmail(trimToNull(digirContact.getEmail()));
    contact.addPhone(trimToNull(digirContact.getPhone()));
    contact.setType(ContactType.inferType(digirContact.getType()));
    return contact;
  }
}
