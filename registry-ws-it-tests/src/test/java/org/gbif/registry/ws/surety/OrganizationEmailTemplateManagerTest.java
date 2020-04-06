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
package org.gbif.registry.ws.surety;

import org.gbif.api.model.directory.Person;
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.registry.domain.mail.BaseEmailModel;
import org.gbif.registry.mail.config.IdentitySuretyMailConfigurationProperties;
import org.gbif.registry.mail.config.MailConfigurationProperties;
import org.gbif.registry.mail.config.OrganizationSuretyMailConfigurationProperties;
import org.gbif.registry.mail.identity.IdentityEmailDataProvider;
import org.gbif.registry.mail.identity.IdentityEmailTemplateProcessor;
import org.gbif.registry.mail.organization.OrganizationEmailDataProvider;
import org.gbif.registry.mail.organization.OrganizationEmailManager;
import org.gbif.registry.mail.organization.OrganizationEmailTemplateProcessor;
import org.gbif.registry.test.TestDataFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Unit tests related to {@link OrganizationEmailManager}. */
public class OrganizationEmailTemplateManagerTest {

  private static final String TEST_NODE_MANAGER_EMAIL = "nodemanager@b.com";

  private OrganizationSuretyMailConfigurationProperties
      organizationSuretyMailConfigurationProperties;

  private IdentitySuretyMailConfigurationProperties identitySuretyMailConfigurationProperties;

  private MailConfigurationProperties mailConfigurationProperties;

  private OrganizationEmailManager organizationEmailTemplateManager;

  private OrganizationEmailDataProvider organizationEmailDataProvider;

  private IdentityEmailDataProvider identityEmailDataProvider;

  private final TestDataFactory testDataFactory;

  @Autowired
  public OrganizationEmailTemplateManagerTest(
      MailConfigurationProperties mailConfigurationProperties,
      OrganizationSuretyMailConfigurationProperties organizationSuretyMailConfigurationProperties,
      IdentitySuretyMailConfigurationProperties identitySuretyMailConfigurationProperties,
      OrganizationEmailDataProvider organizationEmailDataProvider,
      IdentityEmailDataProvider identityEmailDataProvider,
      TestDataFactory testDataFactory) {
    this.mailConfigurationProperties = mailConfigurationProperties;
    this.organizationSuretyMailConfigurationProperties =
        organizationSuretyMailConfigurationProperties;
    this.identitySuretyMailConfigurationProperties = identitySuretyMailConfigurationProperties;
    this.organizationEmailDataProvider = organizationEmailDataProvider;
    this.identityEmailDataProvider = identityEmailDataProvider;
    this.testDataFactory = testDataFactory;
  }

  @Before
  public void setup() throws IOException {

    IdentityEmailTemplateProcessor newOrganizationTP =
        new IdentityEmailTemplateProcessor(identityEmailDataProvider);

    OrganizationEmailTemplateProcessor endorsementConfirmationTP =
        new OrganizationEmailTemplateProcessor(organizationEmailDataProvider);

    organizationEmailTemplateManager =
        new OrganizationEmailManager(
            newOrganizationTP,
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

    assertNotNull("We can generate the model from the template", baseEmail);
    // since there is no NodeManager we should not CC helpdesk (we send the email to helpdesk)
    assertNull(baseEmail.getCcAddress());

    // now try with a NodeManager
    Person nodeManager = new Person();
    nodeManager.setEmail(TEST_NODE_MANAGER_EMAIL);
    nodeManager.setFirstName("Lars");
    nodeManager.setSurname("Eller");
    org.getContacts().add(testDataFactory.newContact());
    baseEmail =
        organizationEmailTemplateManager.generateOrganizationEndorsementEmailModel(
            org, nodeManager, UUID.randomUUID(), endorsingNode);

    assertNotNull("We can generate the model from the template", baseEmail);
    assertEquals(TEST_NODE_MANAGER_EMAIL, baseEmail.getEmailAddress());
    // we should have a CC to helpdesk
    assertNotNull(baseEmail.getCcAddress());
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
      assertNotNull("We can generate the model from the template", baseEmails);
      assertEquals(2, baseEmails.size());
      assertTrue(
          "Email to Helpdesk is there",
          baseEmails.stream()
              .anyMatch(
                  be ->
                      organizationSuretyMailConfigurationProperties
                          .getHelpdesk()
                          .equals(be.getEmailAddress())));
      assertTrue(
          "Email to Point of Contact is there",
          baseEmails.stream().anyMatch(be -> pocEmail.equals(be.getEmailAddress())));
      assertTrue(
          "Point of Contact name is there",
          baseEmails.stream()
              .anyMatch(be -> be.getBody().contains(pointOfContact.computeCompleteName())));
    } catch (IOException e) {
      fail(e.getMessage());
    }
  }
}
