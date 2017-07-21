package org.gbif.registry;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.database.LiquibaseModules;
import org.gbif.registry.grizzly.RegistryServerWithIdentity;
import org.gbif.registry.guice.RegistryTestModules;
import org.gbif.registry.surety.persistence.ChallengeCodeMapper;
import org.gbif.registry.surety.persistence.ChallengeCodeSupportMapper;
import org.gbif.registry.utils.Nodes;
import org.gbif.registry.utils.Organizations;
import org.gbif.registry.ws.resources.NodeResource;

import java.util.UUID;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static org.gbif.registry.guice.RegistryTestModules.webservice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for the endorsement of newly created Organization.
 * Th
 */
public class OrganizationCreationIT {

  @ClassRule
  public static final RegistryServerWithIdentity registryServer = RegistryServerWithIdentity.INSTANCE;

  @Rule
  public final DatabaseInitializer databaseRule = new DatabaseInitializer(LiquibaseModules.database());

  /**
   * It is not in the scope of this test to test the email bits.
   */
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
    UUID newOrganizationKey = organizationService.create(o);

    assertEquals(Long.valueOf(0), nodeService.endorsedOrganizations(node.getKey(), new PagingRequest()).getCount());
    assertEquals(Long.valueOf(1), nodeService.pendingEndorsements(new PagingRequest()).getCount());
    assertEquals(Long.valueOf(1), nodeService.pendingEndorsements(node.getKey(), new PagingRequest()).getCount());
    assertEquals("Paging is not returning the correct count", Long.valueOf(1),
            nodeService.pendingEndorsements(new PagingRequest()).getCount());

    final Injector webserviceInj = RegistryTestModules.webservice();

    ChallengeCodeMapper challengeCodeMapper = webserviceInj.getInstance(ChallengeCodeMapper.class);
    ChallengeCodeSupportMapper<UUID> challengeCodeSupportMapper = webserviceInj.getInstance(Key.get(new TypeLiteral<ChallengeCodeSupportMapper<UUID>>(){}));

    Integer challengeCodeKey = challengeCodeSupportMapper.getChallengeCodeKey(newOrganizationKey);
    UUID challengeCode = challengeCodeMapper.getChallengeCode(challengeCodeKey);
    assertTrue("endorsement should be confirmed", organizationService.confirmEndorsement(newOrganizationKey, challengeCode));

    //We should have no more pending endorsement for this node
    assertEquals(Long.valueOf(0), nodeService.pendingEndorsements(node.getKey(), new PagingRequest()).getCount());
  }
}
