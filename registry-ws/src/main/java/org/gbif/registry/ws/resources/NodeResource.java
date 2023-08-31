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
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.PostPersist;
import org.gbif.api.model.registry.PrePersist;
import org.gbif.api.model.registry.search.KeyTitleResult;
import org.gbif.api.model.registry.search.NodeRequestSearchParams;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.directory.Augmenter;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.InstallationMapper;
import org.gbif.registry.persistence.mapper.NodeMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.persistence.mapper.params.BaseListParams;
import org.gbif.registry.persistence.mapper.params.InstallationListParams;
import org.gbif.registry.persistence.mapper.params.NodeListParams;
import org.gbif.registry.persistence.mapper.params.OrganizationListParams;
import org.gbif.registry.persistence.service.MapperServiceLocator;
import org.gbif.registry.service.WithMyBatis;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.groups.Default;

import org.springframework.context.annotation.Primary;
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

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import static org.gbif.registry.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.security.UserRoles.EDITOR_ROLE;

@io.swagger.v3.oas.annotations.tags.Tag(
    name = "Participant nodes",
    description =
        "Each of GBIF's formal participants designates and establishes a **node** responsible for "
            + "coordinating GBIF-related in-country activities.\n\n"
            + "The nodes API provides CRUD and discovery services for nodes. Its most prominent use on the GBIF "
            + "portal is to drive the [GBIF network page](https://www.gbif.org/the-gbif-network) and country pages.\n\n"
            + "Please note deletion of nodes is logical, meaning node entries remain registered forever and only get a "
            + "deleted timestamp. On the other hand, deletion of a nodes's endpoints, identifiers, tags, "
            + "machine tags, comments, and metadata descriptions is physical, meaning the entries are permanently removed.\n\n"
            + "Also note, nodes are managed in a different, internal GBIF system, so the usual create-update-delete methods"
            + "exist but are not documented here.",
    extensions =
        @io.swagger.v3.oas.annotations.extensions.Extension(
            name = "Order",
            properties = @ExtensionProperty(name = "Order", value = "0300")))
@Validated
@Primary
@RestController
@RequestMapping(value = "node", produces = MediaType.APPLICATION_JSON_VALUE)
public class NodeResource extends BaseNetworkEntityResource<Node, NodeListParams>
    implements NodeService {

  private final NodeMapper nodeMapper;
  private final OrganizationMapper organizationMapper;
  private final InstallationMapper installationMapper;
  private final DatasetMapper datasetMapper;
  private final Augmenter nodeAugmenter;

  public NodeResource(
      MapperServiceLocator mapperServiceLocator,
      EventManager eventManager,
      Augmenter nodeAugmenter,
      WithMyBatis withMyBatis) {
    super(
        mapperServiceLocator.getNodeMapper(),
        mapperServiceLocator,
        Node.class,
        eventManager,
        withMyBatis);
    this.nodeMapper = mapperServiceLocator.getNodeMapper();
    this.organizationMapper = mapperServiceLocator.getOrganizationMapper();
    this.nodeAugmenter = nodeAugmenter;
    this.datasetMapper = mapperServiceLocator.getDatasetMapper();
    this.installationMapper = mapperServiceLocator.getInstallationMapper();
  }

  @Operation(
      operationId = "getNode",
      summary = "Get details of a single node",
      description = "Details of a single node.  Also works for deleted nodes.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0200")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "200", description = "Node found and returned")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{key}")
  @NullToNotFound("/node/{key}")
  @Override
  public Node get(@PathVariable UUID key) {
    return nodeAugmenter.augment(super.get(key));
  }

  /**
   * Creates a new node.
   *
   * @param node node
   * @return key of entity created
   */
  // Method overridden only for documentation.  Note it is hidden.
  @Hidden
  @Operation(
      operationId = "createNode",
      summary = "Create a new node",
      description =
          "Creates a new node.  Note endpoints, identifiers, tags, machine tags, comments and "
              + "metadata descriptions must be added in subsequent requests.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0201")))
  @ApiResponse(responseCode = "201", description = "Node created, new node's UUID returned")
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PrePersist.class, Default.class})
  @Override
  public UUID create(@RequestBody @Trim Node node) {
    return super.create(node);
  }

  /**
   * Updates the node.
   *
   * @param node node
   */
  // Method overridden only for documentation.  Note it is hidden.
  @Hidden
  @Operation(
      operationId = "updateNode",
      summary = "Update an existing node",
      description =
          "Updates the existing node.  Note endpoints, identifiers, tags, machine tags, comments and "
              + "metadata descriptions are not changed with this method.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0202")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "204", description = "Node updated")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PutMapping(value = "{key}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PostPersist.class, Default.class})
  @Override
  public void update(@PathVariable("key") UUID key, @Valid @RequestBody @Trim Node node) {
    super.update(key, node);
  }

  /**
   * Deletes the node.
   *
   * @param key key of node to delete
   */
  // Method overridden only for documentation.  Note it is hidden.
  @Hidden
  @Operation(
      operationId = "deleteNode",
      summary = "Delete a node",
      description =
          "Marks a node as deleted.  Note endpoints, identifiers, tags, machine tags, comments and "
              + "metadata descriptions are not changed.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0203")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "204", description = "Node deleted")
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
      operationId = "listNodes",
      summary = "List all nodes",
      description = "Lists all current nodes (deleted nodes are not listed).",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0100")))
  @SimpleSearchParameters
  @CommonParameters.QParameter
  @ApiResponse(responseCode = "200", description = "Node search successful")
  @ApiResponse(responseCode = "400", description = "Invalid search query provided")
  @GetMapping
  @Override
  public PagingResponse<Node> list(NodeRequestSearchParams request) {
    NodeListParams listParams =
        NodeListParams.builder()
            .query(parseQuery(request.getQ()))
            .from(parseFrom(request.getModified()))
            .to(parseTo(request.getModified()))
            .deleted(false)
            .identifier(request.getIdentifier())
            .identifierType(request.getIdentifierType())
            .mtNamespace(request.getMachineTagNamespace())
            .mtName(request.getMachineTagName())
            .mtValue(request.getMachineTagValue())
            .page(request.getPage())
            .build();

    return decorateResponse(
        pagingResponse(
            request.getPage(), nodeMapper.count(listParams), nodeMapper.list(listParams)));
  }

  /** Decorates the Nodes in the response with the Augmenter. */
  private PagingResponse<Node> decorateResponse(PagingResponse<Node> response) {
    for (Node n : response.getResults()) {
      nodeAugmenter.augment(n);
    }
    return response;
  }

  @Override
  public PagingResponse<Node> search(String query, Pageable page) {
    return decorateResponse(super.search(query, page));
  }

  @Override
  public PagingResponse<Node> list(Pageable page) {
    return decorateResponse(super.list(page));
  }

  @Override
  public PagingResponse<Node> listByIdentifier(
      IdentifierType type, String identifier, Pageable page) {
    return decorateResponse(super.listByIdentifier(type, identifier, page));
  }

  @Override
  public PagingResponse<Node> listByIdentifier(String identifier, Pageable page) {
    return decorateResponse(super.listByIdentifier(identifier, page));
  }

  @Operation(
      operationId = "getNodeOrganizations",
      summary = "List node's organizations",
      description = "Lists the organizations registered to this node.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0240")))
  @Docs.DefaultEntityKeyParameter
  @Pageable.OffsetLimitParameters
  @ApiResponse(responseCode = "200", description = "List of organizations")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{key}/organization")
  @Override
  public PagingResponse<Organization> endorsedOrganizations(
      @PathVariable("key") UUID nodeKey, Pageable page) {
    OrganizationListParams listParams =
        OrganizationListParams.builder()
            .endorsedByNodeKey(nodeKey)
            .isEndorsed(true)
            .page(page)
            .build();
    return new PagingResponse<>(
        page, organizationMapper.count(listParams), organizationMapper.list(listParams));
  }

  /**
   * @deprecated Use {@link OrganizationResource#listPendingEndorsement(Pageable)} instead.
   * @param page
   * @return
   */
  @Operation(
      operationId = "getPendingOrganizations2",
      summary = "List pending organizations",
      description =
          "Lists organizations whose endorsement is pending.\n\n"
              + "Use [getPendingOrganizations](#tag/Organizations/operation/getPendingOrganizations) instead.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0511")))
  @Pageable.OffsetLimitParameters
  @ApiResponse(responseCode = "200", description = "List of pending organizations")
  @Docs.DefaultUnsuccessfulReadResponses
  @Deprecated
  @GetMapping("pendingEndorsement")
  @Override
  public PagingResponse<Organization> pendingEndorsements(Pageable page) {
    OrganizationListParams listParams =
        OrganizationListParams.builder().isEndorsed(false).page(page).build();
    return new PagingResponse<>(
        page, organizationMapper.count(listParams), organizationMapper.list(listParams));
  }

  @Operation(
      operationId = "getNodePendingOrganizations",
      summary = "List pending organizations of a node",
      description = "Lists organizations whose endorsement  by the given node is pending.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0510")))
  @Pageable.OffsetLimitParameters
  @ApiResponse(responseCode = "200", description = "List of pending organizations")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{key}/pendingEndorsement")
  @Override
  public PagingResponse<Organization> pendingEndorsements(
      @PathVariable("key") UUID nodeKey, Pageable page) {
    OrganizationListParams listParams =
        OrganizationListParams.builder()
            .endorsedByNodeKey(nodeKey)
            .isEndorsed(false)
            .page(page)
            .build();
    return new PagingResponse<>(
        page, organizationMapper.count(listParams), organizationMapper.list(listParams));
  }

  @Operation(
      operationId = "getNodeByCountry",
      summary = "Get the node for a country",
      description =
          "Gets the country node by ISO 639-1 (2 letter) or ISO 639-2 (3 letter) country code",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0250")))
  @ApiResponse(responseCode = "200", description = "Country node")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("country/{key}")
  @Nullable
  public Node getByCountry(@PathVariable("key") String isoCode) {
    return getByCountry(Country.fromIsoCode(isoCode));
  }

  @Nullable
  @Override
  public Node getByCountry(Country country) {
    return nodeAugmenter.augment(nodeMapper.getByCountry(country));
  }

  @Operation(
      operationId = "getMemberCountries",
      summary = "List all GBIF member countries",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0251")))
  @ApiResponse(responseCode = "200", description = "List of countries")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("country")
  @Override
  public List<Country> listNodeCountries() {
    return nodeMapper.listNodeCountries();
  }

  @Operation(
      operationId = "getActiveCountries",
      summary = "List all GBIF member countries than are either voting or associate participants",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0252")))
  @ApiResponse(responseCode = "200", description = "List of countries")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("activeCountries")
  @Override
  public List<Country> listActiveCountries() {
    return nodeMapper.listActiveCountries();
  }

  @Operation(
      operationId = "getNodeDatasets",
      summary = "List all datasets from a node",
      description = "Lists datasets published by organizations endorsed by the node",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0253")))
  @Pageable.OffsetLimitParameters
  @ApiResponse(responseCode = "200", description = "List of datasets")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{key}/dataset")
  @Override
  public PagingResponse<Dataset> endorsedDatasets(
      @PathVariable("key") UUID nodeKey, Pageable page) {
    return pagingResponse(
        page,
        datasetMapper.countDatasetsEndorsedBy(nodeKey),
        datasetMapper.listDatasetsEndorsedBy(nodeKey, page));
  }

  @Hidden
  @GetMapping("{key}/contact")
  @Override
  public List<Contact> listContacts(@PathVariable("key") UUID targetEntityKey) {
    throw new UnsupportedOperationException("Contacts are manually managed in the Directory");
  }

  @Override
  protected PagingResponse<Node> list(BaseListParams params) {
    NodeListParams p = NodeListParams.from(params);
    return new PagingResponse<>(p.getPage(), nodeMapper.count(p), nodeMapper.list(p));
  }

  @Hidden
  @DeleteMapping("{key}/contact/{contactKey}")
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  @Override
  public void deleteContact(
      @PathVariable("key") UUID targetEntityKey, @PathVariable int contactKey) {
    throw new UnsupportedOperationException("Contacts are manually managed in the Directory");
  }

  @Hidden
  @PostMapping(value = "{key}/contact", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  @Override
  public int addContact(@PathVariable("key") UUID targetEntityKey, @RequestBody Contact contact) {
    throw new UnsupportedOperationException("Contacts are manually managed in the Directory");
  }

  @Operation(
      operationId = "getNodeInstallations",
      summary = "List node's installations",
      description = "Lists installations hosted by organizations endorsed by the node.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0260")))
  @Docs.DefaultEntityKeyParameter
  @Pageable.OffsetLimitParameters
  @ApiResponse(responseCode = "200", description = "List of technical installations")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{key}/installation")
  @Override
  public PagingResponse<Installation> installations(
      @PathVariable("key") UUID nodeKey, Pageable page) {
    InstallationListParams listParams =
        InstallationListParams.builder().endorsedByNodeKey(nodeKey).page(page).build();
    return pagingResponse(
        page, installationMapper.count(listParams), installationMapper.list(listParams));
  }

  @Operation(
      operationId = "suggestNodes",
      summary = "Suggest nodes.",
      description =
          "Search that returns up to 20 matching nodes. Results are ordered by relevance. "
              + "The response is smaller than an node search.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0103")))
  @CommonParameters.QParameter
  @ApiResponse(responseCode = "200", description = "Node search successful")
  @ApiResponse(responseCode = "400", description = "Invalid search query provided")
  @GetMapping("suggest")
  @Override
  public List<KeyTitleResult> suggest(@RequestParam(value = "q", required = false) String label) {
    return nodeMapper.suggest(label);
  }
}
