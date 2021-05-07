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
package org.gbif.registry.test;

import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.UUID;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class Organizations extends JsonBackedData<Organization> {

  private NodeService nodeService;
  private OrganizationService organizationService;

  private Nodes nodes;

  public static final String ORGANIZATION_TITLE = "The BGBM";

  @Autowired
  private Organizations(
      NodeService nodeService,
      OrganizationService organizationService,
      Nodes nodes,
      ObjectMapper objectMapper,
      SimplePrincipalProvider simplePrincipalProvider) {
    super(
        "data/organization.json",
        new TypeReference<Organization>() {},
        objectMapper,
        simplePrincipalProvider);
    this.nodeService = nodeService;
    this.organizationService = organizationService;
    this.nodes = nodes;
  }

  public Organization newInstance(UUID endorsingNodeKey) {
    Organization o = super.newInstance();
    o.setEndorsingNodeKey(endorsingNodeKey);
    o.setPassword("password");
    return o;
  }

  /**
   * Persist a new Organization for use in Unit Tests.
   *
   * @return persisted Organization
   */
  public Organization newPersistedInstance() {
    UUID nodeKey = nodeService.create(nodes.newInstance());
    Organization organization = newInstance(nodeKey);
    // password was not included in organization.json, so set it here
    organization.setPassword("password");
    UUID organizationKey = organizationService.create(organization);
    return organizationService.get(organizationKey);
  }

  public Organization newPersistedInstance(UUID nodeKey) {
    Organization organization = newInstance(nodeKey);
    // password was not included in organization.json, so set it here
    organization.setPassword("password");
    UUID organizationKey = organizationService.create(organization);
    return organizationService.get(organizationKey);
  }

  /**
   * Populate credentials used in ws requests.
   *
   * @param organization Organization
   * @return credentials
   */
  public static UsernamePasswordCredentials credentials(Organization organization) {
    return new UsernamePasswordCredentials(
        organization.getKey().toString(), organization.getPassword());
  }
}
