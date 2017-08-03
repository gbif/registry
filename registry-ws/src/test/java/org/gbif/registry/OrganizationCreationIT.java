package org.gbif.registry;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.database.LiquibaseModules;
import org.gbif.registry.grizzly.RegistryServer;
import org.gbif.registry.guice.RegistryTestModules;
import org.gbif.registry.surety.persistence.ChallengeCodeMapper;
import org.gbif.registry.surety.persistence.ChallengeCodeSupportMapper;
import org.gbif.registry.utils.Contacts;
import org.gbif.registry.utils.Nodes;
import org.gbif.registry.utils.Organizations;
import org.gbif.registry.ws.fixtures.TestConstants;
import org.gbif.registry.ws.resources.NodeResource;

import java.util.UUID;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static org.gbif.registry.guice.RegistryTestModules.webservice;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for the endorsement process of newly created Organization.
 * Since the expected behavior is different based on the credentials, the injectors are created inside the
 * specific tests.
 */
public class OrganizationCreationIT {

  @ClassRule
  public static final RegistryServer registryServer = RegistryServer.INSTANCE;

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

    //we need to create the organization using the appKey
    final Injector webserviceAppKey = RegistryTestModules.webserviceAppKeyClient();
    OrganizationService organizationService =  webserviceAppKey.getInstance(OrganizationService.class);

    Organization organization = prepareOrganization(nodeService, organizationService);

    assertEquals(Long.valueOf(0), nodeService.endorsedOrganizations(organization.getEndorsingNodeKey(), new PagingRequest()).getCount());
    assertEquals(Long.valueOf(1), nodeService.pendingEndorsements(new PagingRequest()).getCount());
    assertEquals(Long.valueOf(1), nodeService.pendingEndorsements(organization.getEndorsingNodeKey(), new PagingRequest()).getCount());
    assertEquals("Paging is not returning the correct count", Long.valueOf(1),
            nodeService.pendingEndorsements(new PagingRequest()).getCount());

    final Injector webserviceInj = RegistryTestModules.webservice();

    ChallengeCodeMapper challengeCodeMapper = webserviceInj.getInstance(ChallengeCodeMapper.class);
    ChallengeCodeSupportMapper<UUID> challengeCodeSupportMapper = webserviceInj.getInstance(Key.get(new TypeLiteral<ChallengeCodeSupportMapper<UUID>>(){}));

    Integer challengeCodeKey = challengeCodeSupportMapper.getChallengeCodeKey(organization.getKey());
    UUID challengeCode = challengeCodeMapper.getChallengeCode(challengeCodeKey);
    assertTrue("endorsement should be confirmed", organizationService.confirmEndorsement(organization.getKey(), challengeCode));

    //We should have no more pending endorsement for this node
    assertEquals(Long.valueOf(0), nodeService.pendingEndorsements(organization.getEndorsingNodeKey(), new PagingRequest()).getCount());

    //We should also have a contact
    assertEquals(1, webserviceInj.getInstance(OrganizationService.class).get(organization.getKey()).getContacts().size());
  }

  @Test
  public void testEndorsementsByAdmin() {
    final Injector webservice = webservice();
    final Injector webserviceAppClientWithAppKey = RegistryTestModules.webserviceAppKeyClient();

    NodeResource nodeService =  webservice.getInstance(NodeResource.class);
    OrganizationService organizationService = webserviceAppClientWithAppKey.getInstance(OrganizationService.class);

    Organization organization = prepareOrganization(nodeService, organizationService);
    assertEquals(Long.valueOf(0), nodeService.endorsedOrganizations(organization.getEndorsingNodeKey(), new PagingRequest()).getCount());
    assertFalse("endorsement should be NOT be confirmed using appkey and no confirmation code",
            organizationService.confirmEndorsement(organization.getKey(), null));

    Injector injector = RegistryTestModules.webserviceBasicAuthClient(TestConstants.TEST_ADMIN, TestConstants.TEST_ADMIN);
    OrganizationService adminOrganizationService = injector.getInstance(OrganizationService.class);

    assertTrue("endorsement should be confirmed", adminOrganizationService.confirmEndorsement(organization.getKey(), null));
  }

  private static Organization prepareOrganization(NodeResource nodeService, OrganizationService organizationService) {
    //first create a Node (we need one for endorsement)
    Node node = Nodes.newInstance();
    nodeService.create(node);
    node = nodeService.list(new PagingRequest()).getResults().get(0);

    Organization o = Organizations.newInstance(node.getKey());
    Contact organizationContact = Contacts.newInstance();
    o.getContacts().add(organizationContact);

    UUID newOrganizationKey = organizationService.create(o);
    o.setKey(newOrganizationKey);
    assertNotNull("The new organization should be created", newOrganizationKey);
    return o;
  }
}
