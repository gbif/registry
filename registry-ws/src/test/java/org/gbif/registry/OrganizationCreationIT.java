package org.gbif.registry;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.database.LiquibaseModules;
import org.gbif.registry.grizzly.RegistryServerWithIdentity;
import org.gbif.registry.guice.RegistryTestModules;
import org.gbif.registry.utils.Nodes;
import org.gbif.registry.utils.Organizations;
import org.gbif.registry.ws.resources.NodeResource;

import com.google.inject.Injector;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static org.gbif.registry.guice.RegistryTestModules.webservice;

import static org.junit.Assert.assertEquals;

/**
 * Integration tests for the endorsement of newly created Organization.
 */
public class OrganizationCreationIT {

  @ClassRule
  public static final RegistryServerWithIdentity registryServer = RegistryServerWithIdentity.INSTANCE;

  @Rule
  public final DatabaseInitializer databaseRule = new DatabaseInitializer(LiquibaseModules.database());

  @Test
  public void testEndorsements() {
    final Injector webservice = webservice();

    //first create a Node (we need on for endorsement)
    NodeResource nodeService =  webservice.getInstance(NodeResource.class);
    Node node = Nodes.newInstance();
    nodeService.create(node);
    node = nodeService.list(new PagingRequest()).getResults().get(0);

    Organization o = Organizations.newInstance(node.getKey());
    //we need to create the organization using the appKey
    final Injector webserviceAppKey = RegistryTestModules.webserviceAppKeyClient();
    OrganizationService organizationService =  webserviceAppKey.getInstance(OrganizationService.class);
    organizationService.create(o);

    assertEquals(Long.valueOf(0), nodeService.endorsedOrganizations(node.getKey(), new PagingRequest()).getCount());
    assertEquals(Long.valueOf(1), nodeService.pendingEndorsements(new PagingRequest()).getCount());
    assertEquals(Long.valueOf(1), nodeService.pendingEndorsements(node.getKey(), new PagingRequest()).getCount());
    assertEquals("Paging is not returning the correct count", Long.valueOf(1),
            nodeService.pendingEndorsements(new PagingRequest()).getCount());
  }
}
