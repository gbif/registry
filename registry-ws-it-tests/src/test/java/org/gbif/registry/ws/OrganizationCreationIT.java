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
package org.gbif.registry.ws;

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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.zonky.test.db.postgres.embedded.LiquibasePreparer;
import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension;
import io.zonky.test.db.postgres.junit5.PreparedDbExtension;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the endorsement process of newly created Organization. Since the expected
 * behavior is different based on the credentials, the injectors are created inside the specific
 * tests.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = RegistryIntegrationTestsConfiguration.class)
@ContextConfiguration(initializers = {OrganizationCreationIT.ContextInitializer.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class OrganizationCreationIT {

  @RegisterExtension
  static PreparedDbExtension database =
      EmbeddedPostgresExtension.preparedDatabase(
          LiquibasePreparer.forClasspathLocation("liquibase/master.xml"));

  @RegisterExtension
  public final DatabaseInitializer databaseRule =
      new DatabaseInitializer(database.getTestDatabase());

  static class ContextInitializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
      TestPropertyValues.of(dbTestPropertyPairs())
          .applyTo(configurableApplicationContext.getEnvironment());
      withSearchEnabled(false, configurableApplicationContext.getEnvironment());
    }

    protected static void withSearchEnabled(
        boolean enabled, ConfigurableEnvironment configurableEnvironment) {
      TestPropertyValues.of("searchEnabled=" + enabled).applyTo(configurableEnvironment);
    }

    protected String[] dbTestPropertyPairs() {
      return new String[] {
        "registry.datasource.url=jdbc:postgresql://localhost:"
            + database.getConnectionInfo().getPort()
            + "/"
            + database.getConnectionInfo().getDbName(),
        "registry.datasource.username=" + database.getConnectionInfo().getUser(),
        "registry.datasource.password="
      };
    }
  }

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
    assertFalse(
        organizationService.confirmEndorsement(organization.getKey(), null),
        "endorsement should NOT be confirmed using appkey and no confirmation code");

    assertFalse(
        organizationService.confirmEndorsement(organization.getKey(), null),
        "endorsement should NOT be confirmed without confirmation code");

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
  @Test
  public void testSetEndorsementsByNonAdmin() {

    Organization organization =
        prepareOrganization(
            prepareNode(nodeService, testDataFactory), organizationService, testDataFactory);
    Organization createdOrganization = organizationService.get(organization.getKey());
    createdOrganization.setEndorsementApproved(true);

    // make sure an app can not change the endorsementApproved directly
    assertThrows(
        AccessControlException.class, () -> organizationService.update(createdOrganization));
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
