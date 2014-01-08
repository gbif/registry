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
package org.gbif.registry.ws.client;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Network;
import org.gbif.api.service.registry.NetworkService;
import org.gbif.registry.ws.client.guice.RegistryWs;

import java.util.UUID;
import javax.annotation.Nullable;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;

import com.google.inject.Inject;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.ClientFilter;

/**
 * Client-side implementation to the NetworkService.
 */
public class NetworkWsClient extends BaseNetworkEntityClient<Network> implements NetworkService {

  @Inject
  public NetworkWsClient(@RegistryWs WebResource resource, @Nullable ClientFilter authFilter) {
    super(Network.class, resource.path("network"), authFilter, GenericTypes.PAGING_NETWORK);
  }


  @Override
  public PagingResponse<Dataset> listConstituents(UUID networkKey, @Nullable Pageable page) {
    return get(GenericTypes.PAGING_DATASET, page, networkKey.toString(), "constituents");
  }

  @Override
  public void addConstituent(UUID networkKey, UUID datasetKey) {
    post("", networkKey.toString(), "constituents", datasetKey.toString());
  }

  @Override
  public void removeConstituent(UUID networkKey, UUID datasetKey) {
    delete(networkKey.toString(), "constituents", datasetKey.toString());
  }
}
