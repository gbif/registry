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
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.persistence.ChallengeCodeSupportMapper;
import org.gbif.registry.persistence.mapper.surety.ChallengeCodeMapper;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.registry.ws.client.OrganizationClient;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.gbif.ws.security.KeyStore;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.ValidationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
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
import static org.gbif.registry.ws.it.fixtures.TestConstants.TEST_USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the endorsement process of newly created Organization. Since the expected
 * behavior is different based on the credentials, the injectors are created inside the specific
 * tests.
 */
public class OrganizationCreationIT extends BaseItTest {

  private OrganizationService organizationResource;
  private OrganizationService organizationClient;
  private OrganizationService adminOrganizationClient;
  private OrganizationService userOrganizationClient;
  private NodeService nodeService;
  private ChallengeCodeMapper challengeCodeMapper;
  private ChallengeCodeSupportMapper<UUID> challengeCodeSupportMapper;
  private TestDataFactory testDataFactory;

  @Autowired
  public OrganizationCreationIT(
      OrganizationService organizationResource,
      NodeService nodeService,
      ChallengeCodeMapper challengeCodeMapper,
      ChallengeCodeSupportMapper<UUID> challengeCodeSupportMapper,
      TestDataFactory testDataFactory,
      SimplePrincipalProvider simplePrincipalProvider,
      EsManageServer esServer,
      @LocalServerPort int localServerPort,
      KeyStore keyStore) {
    super(simplePrincipalProvider, esServer);
    this.organizationResource = organizationResource;
    this.nodeService = nodeService;
    this.challengeCodeMapper = challengeCodeMapper;
    this.challengeCodeSupportMapper = challengeCodeSupportMapper;
    this.testDataFactory = testDataFactory;
    this.organizationClient =
        prepareClient(IT_APP_KEY, IT_APP_KEY, localServerPort, keyStore, OrganizationClient.class);
    this.adminOrganizationClient =
        prepareClient(TEST_ADMIN, IT_APP_KEY, localServerPort, keyStore, OrganizationClient.class);
    this.userOrganizationClient =
        prepareClient(TEST_USER, IT_APP_KEY, localServerPort, keyStore, OrganizationClient.class);
  }

  @BeforeEach
  public void init() {
    // reset SimplePrincipleProvider, configured for web service client tests only
    setupPrincipal(TEST_ADMIN, REGISTRY_ADMIN, APP);
  }

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
    Organization organization =
        prepareOrganization(prepareNode(nodeService, testDataFactory), service, testDataFactory);

    assertEquals(
        Long.valueOf(0),
        nodeService
            .endorsedOrganizations(organization.getEndorsingNodeKey(), new PagingRequest())
            .getCount());
    assertEquals(Long.valueOf(1), nodeService.pendingEndorsements(new PagingRequest()).getCount());
    assertEquals(
        Long.valueOf(1),
        nodeService
            .pendingEndorsements(organization.getEndorsingNodeKey(), new PagingRequest())
            .getCount());
    assertEquals(
        Long.valueOf(1),
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
        Long.valueOf(0),
        nodeService
            .pendingEndorsements(organization.getEndorsingNodeKey(), new PagingRequest())
            .getCount());

    // We should also have a contact
    assertEquals(1, service.get(organization.getKey()).getContacts().size());

    // and a comment
    assertEquals(1, service.get(organization.getKey()).getComments().size());
  }

  // TODO: 07/05/2020 client exception, compare to the old client
  @ParameterizedTest
  @EnumSource(
      value = ServiceType.class,
      names = {"RESOURCE"})
  public void testEndorsementsByAdmin(ServiceType serviceType) {
    OrganizationService service = getService(serviceType, organizationResource, organizationClient);
    Organization organization =
        prepareOrganization(prepareNode(nodeService, testDataFactory), service, testDataFactory);
    assertEquals(
        Long.valueOf(0),
        nodeService
            .endorsedOrganizations(organization.getEndorsingNodeKey(), new PagingRequest())
            .getCount());
    UUID organizationKey = organization.getKey();
    assertThrows(
        ValidationException.class,
        () -> service.confirmEndorsement(organizationKey, null),
        "endorsement should NOT be confirmed using appkey and no confirmation code");

    // reset principal - use USER role
    setupPrincipal(TEST_ADMIN, REGISTRY_ADMIN);
    OrganizationService adminService =
        getService(serviceType, organizationResource, adminOrganizationClient);

    assertThrows(
        AccessDeniedException.class,
        () -> adminService.confirmEndorsement(organizationKey, null),
        "endorsement should NOT be confirmed without confirmation code");

    // get the latest version (to get fields like modified)
    organization = service.get(organizationKey);
    organization.setEndorsementApproved(true);
    service.update(organization);

    assertEquals(
        Long.valueOf(1),
        nodeService
            .endorsedOrganizations(organization.getEndorsingNodeKey(), new PagingRequest())
            .getCount());
  }

  // TODO: 07/05/2020 client exception
  /**
   * Only Admin shall be allowed to set EndorsementApproved directly (without providing a
   * confirmationCode)
   */
  @ParameterizedTest
  @EnumSource(
      value = ServiceType.class,
      names = {"RESOURCE"})
  public void testSetEndorsementsByNonAdmin(ServiceType serviceType) {
    OrganizationService service = getService(serviceType, organizationResource, organizationClient);
    Organization organization =
        prepareOrganization(prepareNode(nodeService, testDataFactory), service, testDataFactory);

    // reset principal - use USER role
    setupPrincipal(TEST_USER, USER);
    OrganizationService userService =
        getService(serviceType, organizationResource, userOrganizationClient);

    Organization createdOrganization = userService.get(organization.getKey());
    createdOrganization.setEndorsementApproved(true);

    // make sure an app can not change the endorsementApproved directly
    assertThrows(AccessDeniedException.class, () -> userService.update(createdOrganization));
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
