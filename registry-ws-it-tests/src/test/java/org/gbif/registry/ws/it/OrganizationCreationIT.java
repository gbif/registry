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
import org.gbif.api.vocabulary.AppRole;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.persistence.ChallengeCodeSupportMapper;
import org.gbif.registry.persistence.mapper.surety.ChallengeCodeMapper;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.registry.ws.it.fixtures.TestConstants;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.validation.ValidationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the endorsement process of newly created Organization. Since the expected
 * behavior is different based on the credentials, the injectors are created inside the specific
 * tests.
 */
public class OrganizationCreationIT extends BaseItTest {

  private OrganizationService organizationService;
  private NodeService nodeService;
  private ChallengeCodeMapper challengeCodeMapper;
  private ChallengeCodeSupportMapper<UUID> challengeCodeSupportMapper;
  private TestDataFactory testDataFactory;

  @Autowired
  public OrganizationCreationIT(
      OrganizationService organizationService,
      NodeService nodeService,
      ChallengeCodeMapper challengeCodeMapper,
      ChallengeCodeSupportMapper<UUID> challengeCodeSupportMapper,
      TestDataFactory testDataFactory,
      SimplePrincipalProvider simplePrincipalProvider) {
    super(simplePrincipalProvider);
    this.organizationService = organizationService;
    this.nodeService = nodeService;
    this.challengeCodeMapper = challengeCodeMapper;
    this.challengeCodeSupportMapper = challengeCodeSupportMapper;
    this.testDataFactory = testDataFactory;
  }

  @BeforeEach
  public void setup() {
    // reset SimplePrincipleProvider, configured for web service client tests only
    setupPrincipal(TestConstants.TEST_ADMIN, UserRole.REGISTRY_ADMIN.name(), AppRole.APP.name());
  }

  private void setupPrincipal(String name, String... roles) {
    // reset SimplePrincipleProvider, configured for web service client tests only
    if (getSimplePrincipalProvider() != null) {
      getSimplePrincipalProvider().setPrincipal(name);
      SecurityContext ctx = SecurityContextHolder.createEmptyContext();
      SecurityContextHolder.setContext(ctx);
      ctx.setAuthentication(
          new UsernamePasswordAuthenticationToken(
              getSimplePrincipalProvider().get().getName(),
              "",
              Arrays.stream(roles).map(SimpleGrantedAuthority::new).collect(Collectors.toList())));
    }
  }

  /** It is not in the scope of this test to test the email bits. */
  @Test
  public void testEndorsements() {
    Organization organization =
        prepareOrganization(
            prepareNode(nodeService, testDataFactory), organizationService, testDataFactory);

    // reset principal - use APP role
    setupPrincipal(TestConstants.IT_APP_KEY, AppRole.APP.name());

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
        organizationService.confirmEndorsement(organization.getKey(), challengeCode),
        "endorsement should be confirmed");

    // We should have no more pending endorsement for this node
    assertEquals(
        Long.valueOf(0),
        nodeService
            .pendingEndorsements(organization.getEndorsingNodeKey(), new PagingRequest())
            .getCount());

    // We should also have a contact
    assertEquals(1, organizationService.get(organization.getKey()).getContacts().size());

    // and a comment
    assertEquals(1, organizationService.get(organization.getKey()).getComments().size());
  }

  @Test
  public void testEndorsementsByAdmin() {
    Organization organization =
        prepareOrganization(
            prepareNode(nodeService, testDataFactory), organizationService, testDataFactory);
    assertEquals(
        Long.valueOf(0),
        nodeService
            .endorsedOrganizations(organization.getEndorsingNodeKey(), new PagingRequest())
            .getCount());
    UUID organizationKey = organization.getKey();
    assertThrows(
        ValidationException.class,
        () -> organizationService.confirmEndorsement(organizationKey, null),
        "endorsement should NOT be confirmed using appkey and no confirmation code");

    assertThrows(
        ValidationException.class,
        () -> organizationService.confirmEndorsement(organizationKey, null),
        "endorsement should NOT be confirmed without confirmation code");

    // get the latest version (to get fields like modified)
    organization = organizationService.get(organizationKey);
    organization.setEndorsementApproved(true);
    organizationService.update(organization);

    assertEquals(
        Long.valueOf(1),
        nodeService
            .endorsedOrganizations(organization.getEndorsingNodeKey(), new PagingRequest())
            .getCount());
  }

  /**
   * Only Admin shall be allowed to set EndorsementApproved directly (without providing a
   * confirmationCode)
   */
  @Test
  public void testSetEndorsementsByNonAdmin() {
    Organization organization =
        prepareOrganization(
            prepareNode(nodeService, testDataFactory), organizationService, testDataFactory);

    // reset principal - use USER role
    setupPrincipal(TestConstants.TEST_USER, UserRole.USER.name());

    Organization createdOrganization = organizationService.get(organization.getKey());
    createdOrganization.setEndorsementApproved(true);

    // make sure an app can not change the endorsementApproved directly
    assertThrows(
        AccessDeniedException.class, () -> organizationService.update(createdOrganization));
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
