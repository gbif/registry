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
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Network;
import org.gbif.api.service.registry.NetworkService;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.persistence.mapper.CommentMapper;
import org.gbif.registry.persistence.mapper.ContactMapper;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.EndpointMapper;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.NetworkMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.ws.security.EditorAuthorizationService;

import java.util.UUID;
import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;

/**
 * A MyBATIS implementation of the service.
 */
@Path("network")
@Singleton
public class NetworkResource extends BaseNetworkEntityResource<Network> implements NetworkService {

  private final DatasetMapper datasetMapper;
  private final NetworkMapper networkMapper;

  @Inject
  public NetworkResource(
    NetworkMapper networkMapper,
    ContactMapper contactMapper,
    EndpointMapper endpointMapper,
    IdentifierMapper identifierMapper,
    MachineTagMapper machineTagMapper,
    TagMapper tagMapper,
    CommentMapper commentMapper,
    DatasetMapper datasetMapper,
    EventBus eventBus,
    EditorAuthorizationService userAuthService) {
    super(networkMapper,
      commentMapper,
      contactMapper,
      endpointMapper,
      identifierMapper,
      machineTagMapper,
      tagMapper,
      Network.class,
      eventBus,
      userAuthService);
    this.datasetMapper = datasetMapper;
    this.networkMapper = networkMapper;
  }


  /**
   * All network entities support simple (!) search with "&q=".
   * This is to support the console user interface, and is in addition to any complex, faceted search that might
   * additionally be supported, such as dataset search.
   */
  @GET
  public PagingResponse<Network> list(
    @Nullable @QueryParam("identifierType") IdentifierType identifierType,
    @Nullable @QueryParam("identifier") String identifier,
    @Nullable @QueryParam("machineTagNamespace") String namespace,
    @Nullable @QueryParam("machineTagName") String name,
    @Nullable @QueryParam("machineTagValue") String value,
    @Nullable @QueryParam("q") String query,
    @Nullable @Context Pageable page
  ) {
    // This is getting messy: http://dev.gbif.org/issues/browse/REG-426
    if (identifierType != null && identifier != null) {
      return listByIdentifier(identifierType, identifier, page);
    } else if (identifier != null) {
      return listByIdentifier(identifier, page);
    } else if (namespace != null) {
      return listByMachineTag(namespace, name, value, page);
    } else if (Strings.isNullOrEmpty(query)) {
      return list(page);
    } else {
      return search(query, page);
    }
  }

  @Path("{key}/constituents")
  @GET
  @Override
  public PagingResponse<Dataset> listConstituents(@PathParam("key") UUID networkKey, @Context Pageable page) {
    return pagingResponse(page, (long) networkMapper.countDatasetsInNetwork(networkKey),
      datasetMapper.listDatasetsInNetwork(networkKey, page));
  }

  @Path("{key}/constituents/{datasetKey}")
  @POST
  @RolesAllowed(ADMIN_ROLE)
  @Override
  public void addConstituent(@PathParam("key") UUID networkKey, @PathParam("datasetKey") UUID datasetKey) {
    networkMapper.addDatasetConstituent(networkKey, datasetKey);
  }


  @Path("{key}/constituents/{datasetKey}")
  @DELETE
  @RolesAllowed(ADMIN_ROLE)
  @Override
  public void removeConstituent(@PathParam("key") UUID networkKey, @PathParam("datasetKey") UUID datasetKey) {
    networkMapper.deleteDatasetConstituent(networkKey, datasetKey);
  }

}
