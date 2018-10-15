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
package org.gbif.registry;

import org.apache.commons.beanutils.BeanUtils;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.utils.Installations;
import org.gbif.registry.utils.Nodes;
import org.gbif.registry.utils.Organizations;
import org.gbif.registry.ws.resources.InstallationResource;
import org.gbif.registry.ws.resources.NodeResource;
import org.gbif.registry.ws.resources.OrganizationResource;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.UUID;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.gbif.registry.guice.RegistryTestModules.webservice;
import static org.gbif.registry.guice.RegistryTestModules.webserviceClient;

import static org.junit.Assert.assertEquals;

/**
 * This is parameterized to run the same test routines for the following:
 * <ol>
 * <li>The persistence layer</li>
 * <li>The WS service layer</li>
 * <li>The WS service client layer</li>
 * </ol>
 */
@RunWith(Parameterized.class)
public class InstallationIT extends NetworkEntityTest<Installation> {

  private final OrganizationService organizationService;
  private final NodeService nodeService;

  @Parameters
  public static Iterable<Object[]> data() {
    final Injector webservice = webservice();
    final Injector client = webserviceClient();
    return ImmutableList.<Object[]>of(new Object[] {webservice.getInstance(InstallationResource.class),
      webservice.getInstance(OrganizationResource.class),
      webservice.getInstance(NodeResource.class),
      null},
      new Object[] {client.getInstance(InstallationService.class),
        client.getInstance(OrganizationService.class),
        client.getInstance(NodeService.class),
        client.getInstance(SimplePrincipalProvider.class)});
  }

  public InstallationIT(InstallationService service, OrganizationService organizationService, NodeService nodeService,
    @Nullable SimplePrincipalProvider pp) {
    super(service, pp);
    this.organizationService = organizationService;
    this.nodeService = nodeService;
  }

  @Override
  protected Installation newEntity() {
    UUID nodeKey = nodeService.create(Nodes.newInstance());
    Organization o = Organizations.newInstance(nodeKey);
    UUID key = organizationService.create(o);
    Organization organization = organizationService.get(key);
    Installation i = Installations.newInstance(organization.getKey());
    return i;
  }

  /**
   * Tests that we can successfully disable and undisable an installation.
   */
  @Test
  public void disableInstallation() {
    Installation e = newEntity();
    UUID key = getService().create(e);
    e = getService().get(key);
    assertEquals("Should not be disabled to start", false, e.isDisabled());
    e.setDisabled(true);
    getService().update(e);
    e = getService().get(e.getKey());
    assertEquals("We just disabled it", true, e.isDisabled());
    e.setDisabled(false);
    getService().update(e);
    e = getService().get(e.getKey());
    assertEquals("We just un-disabled it", false, e.isDisabled());

  }

  // Easier to test this here than other places due to our utility factory
  @Test
  public void testHostedByInstallationList() {
    Installation installation = create(newEntity(), 1);
    Organization organization = organizationService.get(installation.getOrganizationKey());
    Node node = nodeService.get(organization.getEndorsingNodeKey());

    PagingResponse<Installation> resp = nodeService.installations(node.getKey(), new PagingRequest());
    assertEquals("Paging counts are not being set", Long.valueOf(1), resp.getCount());

    resp = organizationService.installations(organization.getKey(), new PagingRequest());
    assertEquals("Paging counts are not being set", Long.valueOf(1), resp.getCount());
  }

  protected Installation duplicateForCreateAsEditorTest(Installation entity) throws Exception {
    Installation duplicate = (Installation) BeanUtils.cloneBean(entity);
    duplicate.setOrganizationKey(entity.getOrganizationKey());
    return duplicate;
  }

  protected UUID keyForCreateAsEditorTest(Installation entity) {
    return organizationService.get(entity.getOrganizationKey()).getEndorsingNodeKey();
  }
}
