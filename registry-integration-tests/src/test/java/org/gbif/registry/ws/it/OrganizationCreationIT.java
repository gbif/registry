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
package org.gbif.registry.ws.it;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.EndorsementStatus;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.persistence.ChallengeCodeSupportMapper;
import org.gbif.registry.persistence.mapper.UserMapper;
import org.gbif.registry.persistence.mapper.surety.ChallengeCodeMapper;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.registry.ws.client.NodeClient;
import org.gbif.registry.ws.client.OrganizationClient;
import org.gbif.registry.ws.resources.OrganizationResource;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.gbif.ws.security.KeyStore;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.gbif.api.vocabulary.AppRole.APP;
import static org.gbif.api.vocabulary.UserRole.REGISTRY_ADMIN;
import static org.gbif.api.vocabulary.UserRole.USER;
import static org.gbif.registry.ws.it.fixtures.TestConstants.IT_APP_KEY;
import static org.gbif.registry.ws.it.fixtures.TestConstants.TEST_ADMIN;
import static org.gbif.registry.ws.it.fixtures.TestConstants.TEST_EDITOR;
import static org.gbif.registry.ws.it.fixtures.TestConstants.TEST_USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the endorsement process of newly created Organization. Since the expected
 * behavior is different based on the credentials, the injectors are created inside the specific
 * tests.
 */
public class OrganizationCreationIT extends BaseItTest {

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule = new TestCaseDatabaseInitializer();

  private final OrganizationResource organizationResource;
  private final OrganizationService organizationClient;
  private final OrganizationService adminOrganizationClient;
  private final OrganizationService userOrganizationClient;
  private final NodeService nodeResource;
  private final NodeService nodeClient;
  private final ChallengeCodeMapper challengeCodeMapper;
  private final ChallengeCodeSupportMapper<UUID> challengeCodeSupportMapper;
  private final TestDataFactory testDataFactory;
  private final UserMapper userMapper;

  @Autowired
  public OrganizationCreationIT(
      OrganizationService organizationResource,
      NodeService nodeResource,
      ChallengeCodeMapper challengeCodeMapper,
      ChallengeCodeSupportMapper<UUID> challengeCodeSupportMapper,
      TestDataFactory testDataFactory,
      SimplePrincipalProvider simplePrincipalProvider,
      EsManageServer esServer,
      @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") @LocalServerPort
          int localServerPort,
      KeyStore keyStore,
      UserMapper userMapper) {
    super(simplePrincipalProvider, esServer);
    this.organizationResource = ((OrganizationResource) organizationResource);
    this.nodeResource = nodeResource;
    this.userMapper = userMapper;
    this.nodeClient = prepareClient(localServerPort, keyStore, NodeClient.class);
    this.challengeCodeMapper = challengeCodeMapper;
    this.challengeCodeSupportMapper = challengeCodeSupportMapper;
    this.testDataFactory = testDataFactory;
    this.organizationClient =
        prepareClient(IT_APP_KEY, IT_APP_KEY, localServerPort, keyStore, OrganizationClient.class);
    this.adminOrganizationClient =
        prepareClient(localServerPort, keyStore, OrganizationClient.class);
    this.userOrganizationClient =
        prepareClient(TEST_USER, IT_APP_KEY, localServerPort, keyStore, OrganizationClient.class);
  }

  @BeforeEach
  public void init() {
    // reset SimplePrincipleProvider, configured for web service client tests only
    setupPrincipal(TEST_ADMIN, REGISTRY_ADMIN, APP);
  }

  @SuppressWarnings("rawtypes")
  private void setupPrincipal(String name, Enum... roles) {
    // reset SimplePrincipleProvider, configured for web service client tests only
    if (getSimplePrincipalProvider() != null) {
      getSimplePrincipalProvider().setPrincipal(name);
      SecurityContext ctx = SecurityContextHolder.createEmptyContext();
      SecurityContextHolder.setContext(ctx);
      ctx.setAuthentication(
          new UsernamePasswordAuthenticationToken(
              getSimplePrincipalProvider().get().getName(),
              "",
              Arrays.stream(roles)
                  .map(Enum::name)
                  .map(SimpleGrantedAuthority::new)
                  .collect(Collectors.toList())));
    }
  }

  /** It is not in the scope of this test to test the email bits. */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testEndorsements(ServiceType serviceType) {
    OrganizationService service = getService(serviceType, organizationResource, organizationClient);
    NodeService nodeService = getService(serviceType, nodeResource, nodeClient);

    Organization organization =
        prepareOrganization(prepareNode(nodeService, testDataFactory), service, testDataFactory);

    assertEquals(
        0L,
        nodeService
            .endorsedOrganizations(organization.getEndorsingNodeKey(), new PagingRequest())
            .getCount());
    assertEquals(1L, nodeService.pendingEndorsements(new PagingRequest()).getCount());
    assertEquals(
        1L,
        nodeService
            .pendingEndorsements(organization.getEndorsingNodeKey(), new PagingRequest())
            .getCount());
    assertEquals(
        1L,
        nodeService.pendingEndorsements(new PagingRequest()).getCount(),
        "Paging is not returning the correct count");

    Integer challengeCodeKey =
        challengeCodeSupportMapper.getChallengeCodeKey(organization.getKey());
    UUID challengeCode = challengeCodeMapper.getChallengeCode(challengeCodeKey);
    assertTrue(
        service.confirmEndorsement(organization.getKey(), challengeCode),
        "endorsement should be confirmed");

    // We should have no more pending endorsement for this node
    assertEquals(
        0L,
        nodeService
            .pendingEndorsements(organization.getEndorsingNodeKey(), new PagingRequest())
            .getCount());

    // We should also have a contact
    assertEquals(1, service.get(organization.getKey()).getContacts().size());

    // and a comment
    assertEquals(1, service.get(organization.getKey()).getComments().size());
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testEndorsementsByUpdateMethodByAdmin(ServiceType serviceType) {
    OrganizationService service = getService(serviceType, organizationResource, organizationClient);
    NodeService nodeService = getService(serviceType, nodeResource, nodeClient);

    Organization organization =
        prepareOrganization(prepareNode(nodeService, testDataFactory), service, testDataFactory);
    assertEquals(
        0L,
        nodeService
            .endorsedOrganizations(organization.getEndorsingNodeKey(), new PagingRequest())
            .getCount());
    UUID organizationKey = organization.getKey();
    boolean endorsement = service.confirmEndorsement(organizationKey, UUID.randomUUID());
    assertFalse(
        endorsement, "endorsement should NOT be confirmed using appkey and no confirmation code");

    // reset principal - use ADMIN role
    setupPrincipal(TEST_ADMIN, REGISTRY_ADMIN);
    OrganizationService adminService =
        getService(serviceType, organizationResource, adminOrganizationClient);

    assertThrows(
        AccessDeniedException.class,
        () -> adminService.confirmEndorsement(organizationKey, UUID.randomUUID()),
        "endorsement should NOT be confirmed without confirmation code");

    // get the latest version (to get fields like modified)
    organization = service.get(organizationKey);
    organization.setEndorsementApproved(true);
    adminService.update(organization);

    assertEquals(
        0L,
        nodeService
            .endorsedOrganizations(organization.getEndorsingNodeKey(), new PagingRequest())
            .getCount(),
        "Organization can't be endorsed by update method");
  }

  @Test
  public void testEndorsementsByAdmin() {
    Organization organization =
        prepareOrganization(
            prepareNode(nodeResource, testDataFactory), organizationResource, testDataFactory);
    assertEquals(
        0L,
        nodeResource
            .endorsedOrganizations(organization.getEndorsingNodeKey(), new PagingRequest())
            .getCount());
    UUID organizationKey = organization.getKey();

    organization = organizationResource.get(organizationKey);
    assertNotNull(organization);
    assertFalse(organization.isEndorsementApproved());
    assertEquals(EndorsementStatus.WAITING_FOR_ENDORSEMENT, organization.getEndorsementStatus());
    assertNull(organization.getEndorsed());

    // reset principal - use ADMIN role
    setupPrincipal(TEST_ADMIN, REGISTRY_ADMIN);

    ResponseEntity<Void> response =
        organizationResource.confirmEndorsementEndpoint(organizationKey);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

    organization = organizationResource.get(organizationKey);
    assertNotNull(organization);
    assertTrue(organization.isEndorsementApproved());
    assertEquals(EndorsementStatus.ENDORSED, organization.getEndorsementStatus());
    assertNotNull(organization.getEndorsed());
    assertEquals(
        1L,
        nodeResource
            .endorsedOrganizations(organization.getEndorsingNodeKey(), new PagingRequest())
            .getCount());

    response = organizationResource.revokeEndorsementEndpoint(organizationKey);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

    organization = organizationResource.get(organizationKey);
    assertNotNull(organization);
    assertFalse(organization.isEndorsementApproved());
    assertEquals(EndorsementStatus.REJECTED, organization.getEndorsementStatus());
    assertNull(organization.getEndorsed());
    assertEquals(
        0L,
        nodeResource
            .endorsedOrganizations(organization.getEndorsingNodeKey(), new PagingRequest())
            .getCount());
  }

  @Test
  public void testChangeEndorsementStatus() {
    Organization organization =
        prepareOrganization(
            prepareNode(nodeResource, testDataFactory), organizationResource, testDataFactory);
    assertEquals(
        0L,
        nodeResource
            .endorsedOrganizations(organization.getEndorsingNodeKey(), new PagingRequest())
            .getCount());
    UUID organizationKey = organization.getKey();

    organization = organizationResource.get(organizationKey);
    assertNotNull(organization);
    assertFalse(organization.isEndorsementApproved());
    assertEquals(EndorsementStatus.WAITING_FOR_ENDORSEMENT, organization.getEndorsementStatus());
    assertNull(organization.getEndorsed());

    // change status to REJECTED
    organizationResource.changeEndorsementStatus(organizationKey, EndorsementStatus.REJECTED);

    organization = organizationResource.get(organizationKey);
    assertNotNull(organization);
    assertFalse(organization.isEndorsementApproved());
    assertEquals(EndorsementStatus.REJECTED, organization.getEndorsementStatus());
    assertNull(organization.getEndorsed());
    assertEquals(
        0L,
        nodeResource
            .endorsedOrganizations(organization.getEndorsingNodeKey(), new PagingRequest())
            .getCount());

    // change status to ENDORSED
    organizationResource.changeEndorsementStatus(organizationKey, EndorsementStatus.ENDORSED);

    organization = organizationResource.get(organizationKey);
    assertNotNull(organization);
    assertTrue(organization.isEndorsementApproved());
    assertEquals(EndorsementStatus.ENDORSED, organization.getEndorsementStatus());
    assertNotNull(organization.getEndorsed());
    assertEquals(
        1L,
        nodeResource
            .endorsedOrganizations(organization.getEndorsingNodeKey(), new PagingRequest())
            .getCount());
  }

  /**
   * Only Admin shall be allowed to set EndorsementApproved directly (without providing a
   * confirmationCode)
   */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testSetEndorsementsByNonAdmin(ServiceType serviceType) {
    OrganizationService service = getService(serviceType, organizationResource, organizationClient);
    NodeService nodeService = getService(serviceType, nodeResource, nodeClient);

    Organization organization =
        prepareOrganization(prepareNode(nodeService, testDataFactory), service, testDataFactory);

    // reset principal - use USER role
    setupPrincipal(TEST_USER, USER);

    OrganizationService userService =
        getService(serviceType, organizationResource, userOrganizationClient);

    Organization createdOrganization = userService.get(organization.getKey());
    createdOrganization.setEndorsementApproved(true);

    if (serviceType == ServiceType.RESOURCE) {
      // we use the resource class directly to use the update method of the API, which is the one that is secured
      OrganizationResource userServiceResource =
          (OrganizationResource)
              getService(serviceType, organizationResource, userOrganizationClient);

      // make sure an app can not change the endorsementApproved directly
      assertThrows(
          AccessDeniedException.class,
          () -> userServiceResource.update(createdOrganization.getKey(), createdOrganization));
    } else if (serviceType == ServiceType.CLIENT) {
      // the client always uses the secured method
      // make sure an app can not change the endorsementApproved directly
      assertThrows(AccessDeniedException.class, () -> userService.update(createdOrganization));
    }
  }

  @Test
  public void testUserAllowedToEndorseOrganization() {
    Organization organization =
        prepareOrganization(
            prepareNode(nodeResource, testDataFactory), organizationResource, testDataFactory);
    assertEquals(
        0L,
        nodeResource
            .endorsedOrganizations(organization.getEndorsingNodeKey(), new PagingRequest())
            .getCount());
    UUID organizationKey = organization.getKey();

    // admin
    ResponseEntity<Void> response =
        organizationResource.userAllowedToEndorseOrganization(organizationKey, TEST_ADMIN);

    assertNotNull(response);
    assertTrue(
        response.getStatusCode().is2xxSuccessful(),
        "Admin must be allowed to endorse organization");

    // reset principal - use USER role
    setupPrincipal(TEST_USER, USER);

    // user without editor rights
    response = organizationResource.userAllowedToEndorseOrganization(organizationKey, TEST_USER);

    assertNotNull(response);
    assertTrue(
        response.getStatusCode().is4xxClientError(),
        "User without editor rights must not be allowed to endorse organization");

    // reset principal - use USER role
    setupPrincipal(TEST_EDITOR, USER);

    // add editor rights
    userMapper.addEditorRight(TEST_EDITOR, organization.getEndorsingNodeKey());

    // user with editor rights
    response = organizationResource.userAllowedToEndorseOrganization(organizationKey, TEST_EDITOR);

    assertNotNull(response);
    assertTrue(
        response.getStatusCode().is2xxSuccessful(),
        "User with editor rights must be allowed to endorse organization");
  }

  private static Node prepareNode(NodeService nodeService, TestDataFactory testDataFactory) {
    // first create a Node (we need one for endorsement)
    Node node = testDataFactory.newNode();
    nodeService.create(node);
    return nodeService.list(new PagingRequest()).getResults().get(0);
  }

  private static Organization prepareOrganization(
      Node node, OrganizationService organizationService, TestDataFactory testDataFactory) {
    Organization o = testDataFactory.newOrganization(node.getKey());
    Contact organizationContact = testDataFactory.newContact();
    o.getContacts().add(organizationContact);

    Comment comment = testDataFactory.newComment();
    o.getComments().add(comment);

    UUID newOrganizationKey = organizationService.create(o);
    o.setKey(newOrganizationKey);
    assertNotNull(newOrganizationKey, "The new organization should be created");
    return o;
  }
}
