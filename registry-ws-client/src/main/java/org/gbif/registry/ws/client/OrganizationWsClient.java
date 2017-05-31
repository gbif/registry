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
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.search.KeyTitleResult;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.ws.client.guice.RegistryWs;
import org.gbif.ws.client.QueryParamBuilder;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.ws.rs.core.MultivaluedMap;

import com.google.inject.Inject;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * Client-side implementation to the OrganizationService.
 */
public class OrganizationWsClient extends BaseNetworkEntityClient<Organization>
  implements OrganizationService {

  @Inject
  public OrganizationWsClient(@RegistryWs WebResource resource, @Nullable ClientFilter authFilter) {
    super(Organization.class, resource.path("organization"), authFilter, GenericTypes.PAGING_ORGANIZATION);
  }

  @Override
  public PagingResponse<Dataset> hostedDatasets(UUID organizationKey, Pageable page) {
    return get(GenericTypes.PAGING_DATASET, null, null, page, String.valueOf(organizationKey), "hostedDataset");
  }

  @Override
  public PagingResponse<Dataset> publishedDatasets(UUID organizationKey, Pageable page) {
    return get(GenericTypes.PAGING_DATASET, null, null, page, String.valueOf(organizationKey), "publishedDataset");
  }

  @Override
  public PagingResponse<Organization> listByCountry(Country country, @Nullable Pageable page) {
    return get(GenericTypes.PAGING_ORGANIZATION, null, QueryParamBuilder.create("country", country).build(), page);
  }

  @Override
  public PagingResponse<Installation> installations(UUID organizationKey, Pageable page) {
    return get(GenericTypes.PAGING_INSTALLATION, null, null, page, String.valueOf(organizationKey), "installation");
  }

  @Override
  public PagingResponse<Organization> listDeleted(Pageable page) {
    return get(GenericTypes.PAGING_ORGANIZATION, null, null, page, "deleted");
  }

  @Override
  public PagingResponse<Organization> listPendingEndorsement(Pageable page) {
    return get(GenericTypes.PAGING_ORGANIZATION, null, null, page, "pending");
  }

  @Override
  public PagingResponse<Organization> listNonPublishing(Pageable page) {
    return get(GenericTypes.PAGING_ORGANIZATION, null, null, page, "nonPublishing");
  }

  @Override
  public List<KeyTitleResult> suggest(@Nullable String q) {
    MultivaluedMap queryParams = new MultivaluedMapImpl();
    queryParams.put("q", q);
    return get(GenericTypes.LIST_KEY_TITLE, null, queryParams, null, "suggest");
  }
}
