/*
 * Copyright 2013 Global Biodiversity Information Facility (GBIF)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.ws.resources;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.directory.Augmenter;
import org.gbif.registry.persistence.mapper.CommentMapper;
import org.gbif.registry.persistence.mapper.ContactMapper;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.EndpointMapper;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.InstallationMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.NodeMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.ws.guice.Trim;
import org.gbif.registry.ws.security.EditorAuthorizationService;
import org.gbif.registry.gdpr.GdprService;
import org.gbif.ws.server.interceptor.NullToNotFound;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;

@Singleton
@Path("node")
public class NodeResource extends BaseNetworkEntityResource<Node> implements NodeService {

  private final NodeMapper nodeMapper;
  private final OrganizationMapper organizationMapper;
  private final InstallationMapper installationMapper;
  private final DatasetMapper datasetMapper;
  private final Augmenter nodeAugmenter;

  @Inject
  public NodeResource(
    NodeMapper nodeMapper,
    IdentifierMapper identifierMapper,
    CommentMapper commentMapper,
    ContactMapper contactMapper,
    EndpointMapper endpointMapper,
    MachineTagMapper machineTagMapper,
    TagMapper tagMapper,
    OrganizationMapper organizationMapper,
    DatasetMapper datasetMapper,
    InstallationMapper installationMapper,
    EventBus eventBus,
    Augmenter nodeAugmenter,
    EditorAuthorizationService userAuthService,
    GdprService gdprService) {
    super(nodeMapper, commentMapper, contactMapper, endpointMapper, identifierMapper, machineTagMapper, tagMapper,
      Node.class, eventBus, userAuthService, gdprService);
    this.nodeMapper = nodeMapper;
    this.organizationMapper = organizationMapper;
    this.nodeAugmenter = nodeAugmenter;
    this.datasetMapper = datasetMapper;
    this.installationMapper = installationMapper;
  }

  @GET
  @Path("{key}")
  @Nullable
  @NullToNotFound
  @Override
  public Node get(@PathParam("key") UUID key) {
    return nodeAugmenter.augment(super.get(key));
  }


  /**
   * All network entities support simple (!) search with "&q=".
   * This is to support the console user interface, and is in addition to any complex, faceted search that might
   * additionally be supported, such as dataset search.
   */
  @GET
  public PagingResponse<Node> list(@Nullable @QueryParam("q") String query,
    @Nullable @QueryParam("identifierType") IdentifierType identifierType,
    @Nullable @QueryParam("identifier") String identifier,
    @Nullable @Context Pageable page) {
    // This is getting messy: http://dev.gbif.org/issues/browse/REG-426
    if (identifierType != null && identifier != null) {
      return listByIdentifier(identifierType, identifier, page);
    } else if (identifier != null) {
      return listByIdentifier(identifier, page);
    } else if (Strings.isNullOrEmpty(query)) {
      return list(page);
    } else {
      return search(query, page);
    }
  }

  /**
   * Decorates the Nodes in the response with the Augmenter.
   */
  private PagingResponse<Node> decorateResponse(PagingResponse<Node> response) {
    for (Node n : response.getResults()) {
      nodeAugmenter.augment(n);
    }
    return response;
  }

  @Override
  public PagingResponse<Node> search(String query, @Nullable Pageable page) {
    return decorateResponse(super.search(query, page));
  }

  @Override
  public PagingResponse<Node> list(@Nullable Pageable page) {
    return decorateResponse(super.list(page));
  }

  @Override
  public PagingResponse<Node> listByIdentifier(IdentifierType type, String identifier, @Nullable Pageable page) {
    return decorateResponse(super.listByIdentifier(type, identifier, page));
  }

  @Override
  public PagingResponse<Node> listByIdentifier(String identifier, @Nullable Pageable page) {
    return decorateResponse(super.listByIdentifier(identifier, page));
  }

  @GET
  @Path("{key}/organization")
  @Override
  public PagingResponse<Organization> endorsedOrganizations(@PathParam("key") UUID nodeKey, @Context Pageable page) {
    return new PagingResponse<Organization>(page, organizationMapper.countOrganizationsEndorsedBy(nodeKey),
      organizationMapper.organizationsEndorsedBy(nodeKey, page));
  }

  @GET
  @Path("pendingEndorsement")
  @Override
  public PagingResponse<Organization> pendingEndorsements(@Context Pageable page) {
    return new PagingResponse<Organization>(page, organizationMapper.countPendingEndorsements(null),
      organizationMapper.pendingEndorsements(null, page));
  }

  @GET
  @Path("{key}/pendingEndorsement")
  @Override
  public PagingResponse<Organization> pendingEndorsements(@PathParam("key") UUID nodeKey, @Context Pageable page) {
    return new PagingResponse<Organization>(page, organizationMapper.countPendingEndorsements(nodeKey),
      organizationMapper.pendingEndorsements(nodeKey, page));
  }

  @GET
  @Path("country/{key}")
  @Nullable
  public Node getByCountry(@PathParam("key") String isoCode) {
    return getByCountry(Country.fromIsoCode(isoCode));
  }

  @Nullable
  @Override
  public Node getByCountry(Country country) {
    return nodeAugmenter.augment(nodeMapper.getByCountry(country));
  }

  @GET
  @Path("country")
  @Override
  public List<Country> listNodeCountries() {
    return nodeMapper.listNodeCountries();
  }

  @GET
  @Path("activeCountries")
  @Override
  public List<Country> listActiveCountries() {
    return nodeMapper.listActiveCountries();
  }

  @GET
  @Override
  @Path("{key}/dataset")
  public PagingResponse<Dataset> endorsedDatasets(@PathParam("key") UUID nodeKey, @Context Pageable page) {
    return pagingResponse(page, datasetMapper.countDatasetsEndorsedBy(nodeKey),
      datasetMapper.listDatasetsEndorsedBy(nodeKey, page));
  }

  @GET
  @Path("{key}/contact")
  @Override
  public List<Contact> listContacts(@PathParam("key") UUID targetEntityKey) {
    throw new UnsupportedOperationException("Contacts are manually managed in the Directory");
  }

  @DELETE
  @Path("{key}/contact/{contactKey}")
  @RolesAllowed(ADMIN_ROLE)
  @Override
  public void deleteContact(@PathParam("key") UUID targetEntityKey, @PathParam("contactKey") int contactKey) {
    throw new UnsupportedOperationException("Contacts are manually managed in the Directory");
  }

  @Override
  public int addContact(@PathParam("key") UUID targetEntityKey, @NotNull @Valid @Trim Contact contact) {
    throw new UnsupportedOperationException("Contacts are manually managed in the Directory");
  }

  @GET
  @Path("{key}/installation")
  @Override
  public PagingResponse<Installation> installations(@PathParam("key") UUID nodeKey, @Context Pageable page) {
    return pagingResponse(page, installationMapper.countInstallationsEndorsedBy(nodeKey),
      installationMapper.listInstallationsEndorsedBy(nodeKey, page));
  }
}
