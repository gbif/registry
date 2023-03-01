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

import org.gbif.api.annotation.NullToNotFound;
import org.gbif.api.annotation.Trim;
import org.gbif.api.documentation.CommonParameters;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Network;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.PostPersist;
import org.gbif.api.model.registry.PrePersist;
import org.gbif.api.model.registry.search.KeyTitleResult;
import org.gbif.api.service.registry.NetworkService;
import org.gbif.registry.domain.ws.NetworkRequestSearchParams;
import org.gbif.registry.events.ChangedComponentEvent;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.NetworkMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.persistence.service.MapperServiceLocator;
import org.gbif.registry.service.WithMyBatis;
import org.gbif.ws.WebApplicationException;

import java.util.List;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.groups.Default;

import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Strings;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import static org.gbif.registry.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.security.UserRoles.EDITOR_ROLE;
import static org.gbif.registry.security.UserRoles.IPT_ROLE;

@io.swagger.v3.oas.annotations.tags.Tag(
  name = "Networks",
  description = "**Networks** are collections of datasets, organized outside the Node-Organization model to serve " +
    "some purpose.\n\n" +
    "The largest network is [Ocean Biodiversity Information Systems (OBIS)](https://www.gbif.org/network/2b7c7b4f-4d4f-40d3-94de-c28b6fa054a6).\n\n" +
    "The dataset API provides CRUD and discovery services for networks. " +
    "Networks are arbitrary collections of datasets grouped for some purpose.\n\n" +
    "Please note deletion of networks is logical, meaning network entries remain registered forever and only get a " +
    "deleted timestamp. On the other hand, deletion of a network's contacts, endpoints, identifiers, tags, " +
    "machine tags, comments, and metadata descriptions is physical, meaning the entries are permanently removed.",
  extensions = @io.swagger.v3.oas.annotations.extensions.Extension(
    name = "Order", properties = @ExtensionProperty(name = "Order", value = "0400")))
@Validated
@Primary
@RestController
@RequestMapping(value = "network", produces = MediaType.APPLICATION_JSON_VALUE)
public class NetworkResource extends BaseNetworkEntityResource<Network> implements NetworkService {

  private final DatasetMapper datasetMapper;
  private final NetworkMapper networkMapper;
  private final OrganizationMapper organizationMapper;
  private final EventManager eventManager;

  public NetworkResource(
      MapperServiceLocator mapperServiceLocator,
      EventManager eventManager,
      WithMyBatis withMyBatis) {
    super(
        mapperServiceLocator.getNetworkMapper(),
        mapperServiceLocator,
        Network.class,
        eventManager,
        withMyBatis);
    this.eventManager = eventManager;
    this.datasetMapper = mapperServiceLocator.getDatasetMapper();
    this.networkMapper = mapperServiceLocator.getNetworkMapper();
    this.organizationMapper = mapperServiceLocator.getOrganizationMapper();
  }

  @Operation(
    operationId = "getNetwork",
    summary = "Get details of a single network",
    description = "Details of a single network.  Also works for deleted networks.",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0300")),
    tags = "BASIC")
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(
    responseCode = "200",
    description = "Network found and returned")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{key}")
  @NullToNotFound("/network/{key}")
  @Override
  public Network get(@PathVariable UUID key) {
    return super.get(key);
  }

  /**
   * Creates a new network.
   *
   * @param network network
   * @return key of entity created
   */
  // Method overridden only for documentation.
  @Operation(
    operationId = "createNetwork",
    summary = "Creates a new network",
    description = "Creates a new network.  Note contacts, endpoints, identifiers, tags, machine tags, comments and " +
      "metadata descriptions must be added in subsequent requests.")
  @ApiResponse(
    responseCode = "201",
    description = "Network created, new network's UUID returned")
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PrePersist.class, Default.class})
  @Override
  public UUID create(@RequestBody @Trim Network network) {
    return super.create(network);
  }

  /**
   * Updates the network.
   *
   * @param network network
   */
  // Method overridden only for documentation.
  @Operation(
    operationId = "updateNetwork",
    summary = "Update an existing network",
    description = "Updates the existing network.  Note contacts, endpoints, identifiers, tags, machine tags, comments and " +
      "metadata descriptions are not changed with this method.")
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(
    responseCode = "204",
    description = "Network updated")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PutMapping(value = "{key}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PostPersist.class, Default.class})
  @Override
  public void update(@PathVariable("key") UUID key, @Valid @RequestBody @Trim Network network) {
    super.update(key, network);
  }

  /**
   * Deletes the network.
   *
   * @param key key of network to delete
   */
  // Method overridden only for documentation.
  @Operation(
    operationId = "deleteNetwork",
    summary = "Delete a network",
    description = "Marks a network as deleted.  Note contacts, endpoints, identifiers, tags, machine tags, comments and " +
      "metadata descriptions are not changed.")
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(
    responseCode = "204",
    description = "Network deleted")
  @Docs.DefaultUnsuccessfulWriteResponses
  @DeleteMapping("{key}")
  @Override
  public void delete(@PathVariable UUID key) {
    super.delete(key);
  }

  /**
   * All network entities support simple (!) search with "&q=". This is to support the console user
   * interface, and is in addition to any complex, faceted search that might additionally be
   * supported, such as dataset search.
   */
  @Operation(
    operationId = "listNetworks",
    summary = "List all networks",
    description = "Lists all current networks (deleted networks are not listed).",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0100")),
    tags = "BASIC")
  @SimpleSearchParameters
  @ApiResponse(
    responseCode = "200",
    description = "Network search successful")
  @ApiResponse(
    responseCode = "400",
    description = "Invalid search query provided")
  @GetMapping
  public PagingResponse<Network> list(@Valid NetworkRequestSearchParams request, Pageable page) {
    if (request.getIdentifierType() != null && request.getIdentifier() != null) {
      return listByIdentifier(request.getIdentifierType(), request.getIdentifier(), page);
    } else if (request.getIdentifier() != null) {
      return listByIdentifier(request.getIdentifier(), page);
    } else if (request.getMachineTagNamespace() != null) {
      return listByMachineTag(
          request.getMachineTagNamespace(),
          request.getMachineTagName(),
          request.getMachineTagValue(),
          page);
    } else if (Strings.isNullOrEmpty(request.getQ())) {
      return list(page);
    } else {
      return search(request.getQ(), page);
    }
  }

  @Operation(
    operationId = "listNetworkConstituents",
    summary = "List all constituents (datasets) of a network",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0100")),
    tags = "BASIC")
  @Docs.DefaultEntityKeyParameter
  @Pageable.OffsetLimitParameters
  @ApiResponse(
    responseCode = "200",
    description = "Constituent dataset list")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{key}/constituents")
  @Override
  public PagingResponse<Dataset> listConstituents(
      @PathVariable("key") UUID networkKey, Pageable page) {
    return pagingResponse(
        page,
        (long) networkMapper.countDatasetsInNetwork(networkKey),
        datasetMapper.listDatasetsInNetwork(networkKey, page));
  }

  /**
   * Validates if the requested dataset exists.
   */
  private void existDatasetCheck(UUID datasetKey) {
    if (datasetMapper.get(datasetKey) == null) {
      throw new WebApplicationException(
          "Dataset " + datasetKey + " not found", HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Validates if the requested network exists.
   */
  private void existNetworkCheck(UUID networkKey) {
    if (networkMapper.get(networkKey) == null) {
      throw new WebApplicationException(
          "NetworkKey " + networkKey + " not found", HttpStatus.BAD_REQUEST);
    }
  }

  @Operation(
    operationId = "networkConstituentAdd",
    summary = "Add a constituent dataset to a network")
  @Docs.DefaultEntityKeyParameter
  @Parameter(
    name = "datasetKey",
    description = "The GBIF key of the dataset to add",
    schema = @Schema(implementation = UUID.class),
    in = ParameterIn.PATH
  )
  @ApiResponse(
    responseCode = "201",
    description = "Constituent added.",
    content = @Content)
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping("{key}/constituents/{datasetKey}")
  @Secured({ADMIN_ROLE, EDITOR_ROLE, IPT_ROLE})
  @Override
  public void addConstituent(@PathVariable("key") UUID networkKey, @PathVariable UUID datasetKey) {
    if (networkMapper.constituentExists(networkKey, datasetKey)) {
      throw new WebApplicationException(
          "Dataset " + datasetKey + " is already connected to the network " + networkKey,
          HttpStatus.BAD_REQUEST);
    }
    existDatasetCheck(datasetKey);
    existNetworkCheck(networkKey);
    networkMapper.addDatasetConstituent(networkKey, datasetKey);
    eventManager.post(ChangedComponentEvent.newInstance(datasetKey, Network.class, Dataset.class));
  }

  @Operation(
    operationId = "networkConstituentDelete",
    summary = "Remove a constituent dataset from a network")
  @Docs.DefaultEntityKeyParameter
  @Parameter(
    name = "datasetKey",
    description = "The GBIF key of the dataset to remove",
    schema = @Schema(implementation = UUID.class),
    in = ParameterIn.PATH
  )
  @ApiResponse(
    responseCode = "204",
    description = "Constituent removed.",
    content = @Content)
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @DeleteMapping("{key}/constituents/{datasetKey}")
  @Secured({ADMIN_ROLE, EDITOR_ROLE, IPT_ROLE})
  @Override
  public void removeConstituent(
      @PathVariable("key") UUID networkKey, @PathVariable UUID datasetKey) {
    if (!networkMapper.constituentExists(networkKey, datasetKey)) {
      throw new WebApplicationException(
          "Dataset " + datasetKey + " is not connected to the network " + networkKey,
          HttpStatus.BAD_REQUEST);
    }
    networkMapper.deleteDatasetConstituent(networkKey, datasetKey);
    eventManager.post(ChangedComponentEvent.newInstance(datasetKey, Network.class, Dataset.class));
  }

  @Operation(
    operationId = "suggestNetworks",
    summary = "Suggest networks.",
    description = "Search that returns up to 20 matching networks. Results are ordered by relevance. " +
      "The response is smaller than an network search.",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "1300")),
    tags = "BASIC"
  )
  @CommonParameters.QParameter
  @ApiResponse(
    responseCode = "200",
    description = "Network search successful")
  @ApiResponse(
    responseCode = "400",
    description = "Invalid search query provided")
  @GetMapping("suggest")
  @Override
  public List<KeyTitleResult> suggest(@RequestParam(value = "q", required = false) String label) {
    return networkMapper.suggest(label);
  }

  @Hidden // TODO Not sure whether this is supposed to be public
  @GetMapping("{key}/organization")
  @Override
  public PagingResponse<Organization> publishingOrganizations(
      @PathVariable("key") UUID networkKey, Pageable page) {
    return new PagingResponse<>(
        page,
        organizationMapper.countPublishingOrganizationsInNetwork(networkKey),
        organizationMapper.listPublishingOrganizationsInNetwork(networkKey, page));
  }
}
