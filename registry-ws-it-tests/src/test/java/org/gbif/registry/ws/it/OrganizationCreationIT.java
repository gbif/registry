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
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.persistence.ChallengeCodeSupportMapper;
import org.gbif.registry.persistence.mapper.surety.ChallengeCodeMapper;
import org.gbif.registry.test.TestDataFactory;

import java.security.AccessControlException;
import java.util.UUID;

import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;

import io.zonky.test.db.postgres.embedded.LiquibasePreparer;
import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension;
import io.zonky.test.db.postgres.junit5.PreparedDbExtension;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for the endorsement process of newly created Organization. Since the expected
 * behavior is different based on the credentials, the injectors are created inside the specific
 * tests.
 */
public class OrganizationCreationIT {

  @RegisterExtension
  static PreparedDbExtension database =
      EmbeddedPostgresExtension.preparedDatabase(
          LiquibasePreparer.forClasspathLocation("liquibase/master.xml"));;

  @RegisterExtension
  public final DatabaseInitializer databaseRule =
      new DatabaseInitializer(database.getTestDatabase());

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
      TestDataFactory testDataFactory) {
    this.organizationService = organizationService;
    this.nodeService = nodeService;
    this.challengeCodeMapper = challengeCodeMapper;
    this.challengeCodeSupportMapper = challengeCodeSupportMapper;
    this.testDataFactory = testDataFactory;
  }

  /** It is not in the scope of this test to test the email bits. */
  @Test
  public void testEndorsements() {

    Organization organization =
        prepareOrganization(
            prepareNode(nodeService, testDataFactory), organizationService, testDataFactory);

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
        "Paging is not returning the correct count",
        Long.valueOf(1),
        nodeService.pendingEndorsements(new PagingRequest()).getCount());

    Integer challengeCodeKey =
        challengeCodeSupportMapper.getChallengeCodeKey(organization.getKey());
    UUID challengeCode = challengeCodeMapper.getChallengeCode(challengeCodeKey);
    assertTrue(
        "endorsement should be confirmed",
        organizationService.confirmEndorsement(organization.getKey(), challengeCode));

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
    assertFalse(
        "endorsement should NOT be confirmed using appkey and no confirmation code",
        organizationService.confirmEndorsement(organization.getKey(), null));

    assertFalse(
        "endorsement should NOT be confirmed without confirmation code",
        organizationService.confirmEndorsement(organization.getKey(), null));

    // get the latest version (to get fields like modified)
    organization = organizationService.get(organization.getKey());
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
  @Test(expected = AccessControlException.class)
  public void testSetEndorsementsByNonAdmin() {

    Organization organization =
        prepareOrganization(
            prepareNode(nodeService, testDataFactory), organizationService, testDataFactory);
    organization = organizationService.get(organization.getKey());
    organization.setEndorsementApproved(true);

    // make sure an app can not change the endorsementApproved directly
    organizationService.update(organization);
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

    Comment comment = new Comment();
    comment.setContent("I would like to comment on that.");
    o.getComments().add(comment);

    UUID newOrganizationKey = organizationService.create(o);
    o.setKey(newOrganizationKey);
    assertNotNull("The new organization should be created", newOrganizationKey);
    return o;
  }
}
