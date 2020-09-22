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
package org.gbif.registry.surety;

import org.gbif.api.model.directory.Person;
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.registry.mail.BaseEmailModel;
import org.gbif.registry.mail.config.MailConfigurationProperties;
import org.gbif.registry.mail.config.OrganizationSuretyMailConfigurationProperties;
import org.gbif.registry.mail.organization.OrganizationEmailManager;
import org.gbif.registry.mail.organization.OrganizationEmailTemplateProcessor;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.registry.ws.it.RegistryIntegrationTestsConfiguration;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** Unit tests related to {@link OrganizationEmailManager}. */
@SpringBootTest(
    classes =
        OrganizationEmailTemplateManagerIT.OrganizationEmailTemplateManagerTestConfiguration.class)
public class OrganizationEmailTemplateManagerIT extends BaseItTest {

  @Configuration
  public static class OrganizationEmailTemplateManagerTestConfiguration
      extends RegistryIntegrationTestsConfiguration {}

  private static final String TEST_NODE_MANAGER_EMAIL = "nodemanager@b.com";

  private final OrganizationSuretyMailConfigurationProperties
      organizationSuretyMailConfigurationProperties;

  private final MailConfigurationProperties mailConfigurationProperties;
  private OrganizationEmailManager organizationEmailTemplateManager;
  private final TestDataFactory testDataFactory;

  @Autowired
  public OrganizationEmailTemplateManagerIT(
      MailConfigurationProperties mailConfigurationProperties,
      OrganizationSuretyMailConfigurationProperties organizationSuretyMailConfigurationProperties,
      TestDataFactory testDataFactory,
      SimplePrincipalProvider simplePrincipalProvider,
      EsManageServer esServer) {
    super(simplePrincipalProvider, esServer);
    this.mailConfigurationProperties = mailConfigurationProperties;
    this.organizationSuretyMailConfigurationProperties =
        organizationSuretyMailConfigurationProperties;
    this.testDataFactory = testDataFactory;
  }

  @BeforeEach
  public void init() {
    OrganizationEmailTemplateProcessor organizationEmailTP =
        new OrganizationEmailTemplateProcessor();

    organizationEmailTemplateManager =
        new OrganizationEmailManager(
            organizationEmailTP,
            mailConfigurationProperties,
            organizationSuretyMailConfigurationProperties);
  }

  @Test
  public void testGenerateOrganizationEndorsementEmailModel() throws IOException {
    Node endorsingNode = testDataFactory.newNode();
    endorsingNode.setKey(UUID.randomUUID());
    Organization org = testDataFactory.newOrganization();
    org.setKey(UUID.randomUUID());

    org.getComments()
        .add(generateComment("This is a very important comment.\nI even include another line."));
    org.getComments().add(generateComment("This is a also important."));
    BaseEmailModel baseEmail =
        organizationEmailTemplateManager.generateOrganizationEndorsementEmailModel(
            org, null, UUID.randomUUID(), endorsingNode);

    assertNotNull(baseEmail, "We can generate the model from the template");
    // since there is no NodeManager we should not CC helpdesk (we send the email to helpdesk)
    assertTrue(baseEmail.getCcAddresses().isEmpty());

    // now try with a NodeManager
    Person nodeManager = new Person();
    nodeManager.setEmail(TEST_NODE_MANAGER_EMAIL);
    nodeManager.setFirstName("Lars");
    nodeManager.setSurname("Eller");
    org.getContacts().add(testDataFactory.newContact());
    baseEmail =
        organizationEmailTemplateManager.generateOrganizationEndorsementEmailModel(
            org, nodeManager, UUID.randomUUID(), endorsingNode);

    assertNotNull(baseEmail, "We can generate the model from the template");
    String emailAddress = new ArrayList<>(baseEmail.getEmailAddresses()).get(0);
    assertEquals(TEST_NODE_MANAGER_EMAIL, emailAddress);
    // we should have a CC to helpdesk
    assertNotNull(baseEmail.getCcAddresses());
  }

  private static Comment generateComment(String comment) {
    Comment myComment = new Comment();
    myComment.setContent(comment);
    return myComment;
  }

  @Test
  public void testGenerateOrganizationEndorsedEmailModel() {
    final String pocEmail = "point_of_contact@b.com";
    final Node endorsingNode = testDataFactory.newNode();
    endorsingNode.setKey(UUID.randomUUID());
    Organization org = testDataFactory.newOrganization(endorsingNode.getKey());
    Contact pointOfContact = testDataFactory.newContact();
    pointOfContact.setFirstName("First");
    pointOfContact.setLastName("Last");
    pointOfContact.setEmail(Collections.singletonList(pocEmail));
    pointOfContact.setType(ContactType.POINT_OF_CONTACT);

    org.setKey(UUID.randomUUID());
    org.getContacts().add(pointOfContact);

    try {
      List<BaseEmailModel> baseEmails =
          organizationEmailTemplateManager.generateOrganizationEndorsedEmailModel(
              org, endorsingNode);
      assertNotNull(baseEmails, "We can generate the model from the template");
      assertEquals(2, baseEmails.size());
      assertTrue(
          baseEmails.stream()
              .anyMatch(
                  be ->
                      organizationSuretyMailConfigurationProperties
                          .getHelpdesk()
                          .equals(be.getEmailAddresses())),
          "Email to Helpdesk is there");
      assertTrue(
          baseEmails.stream().anyMatch(be -> pocEmail.equals(be.getEmailAddresses())),
          "Email to Point of Contact is there");
      assertTrue(
          baseEmails.stream()
              .anyMatch(be -> be.getBody().contains(pointOfContact.computeCompleteName())),
          "Point of Contact name is there");
    } catch (IOException e) {
      fail(e.getMessage());
    }
  }
}
