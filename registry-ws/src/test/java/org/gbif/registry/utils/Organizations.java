/*
 * Copyright 2013 Global Biodiversity Information Facility (GBIF)
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
package org.gbif.registry.utils;

import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.guice.RegistryTestModules;
import org.gbif.registry.ws.resources.NodeResource;
import org.gbif.registry.ws.resources.OrganizationResource;

import java.util.UUID;

import com.google.inject.Injector;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.codehaus.jackson.type.TypeReference;

public class Organizations extends JsonBackedData<Organization> {

  private static final Organizations INSTANCE = new Organizations();
  private static NodeService nodeService;
  private static OrganizationService organizationService;

  public static final String ORGANIZATION_TITLE = "The BGBM";

  private Organizations() {
    super("data/organization.json", new TypeReference<Organization>() {});
    Injector i = RegistryTestModules.webservice();
    nodeService = i.getInstance(NodeResource.class);
    organizationService = i.getInstance(OrganizationResource.class);
  }

  public static Organization newInstance(UUID endorsingNodeKey) {
    Organization o = INSTANCE.newTypedInstance();
    o.setEndorsingNodeKey(endorsingNodeKey);
    o.setPassword("password");
    return o;
  }

  /**
   * Persist a new Organization for use in Unit Tests.
   *
   * @return persisted Organization
   */
  public static Organization newPersistedInstance() {
    UUID nodeKey = nodeService.create(Nodes.newInstance());
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
   *
   * @return credentials
   */
  public static UsernamePasswordCredentials credentials(Organization organization) {
    return new UsernamePasswordCredentials(organization.getKey().toString(), organization.getPassword());
  }
}
