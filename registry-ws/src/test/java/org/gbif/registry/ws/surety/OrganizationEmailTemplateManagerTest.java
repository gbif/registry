package org.gbif.registry.ws.surety;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.registry.surety.email.BaseEmailModel;
import org.gbif.registry.surety.email.EmailTemplateProcessor;
import org.gbif.registry.utils.Contacts;
import org.gbif.registry.utils.Nodes;
import org.gbif.registry.utils.Organizations;
import org.gbif.registry.ws.fixtures.TestConstants;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import org.apache.ibatis.io.Resources;
import org.junit.Before;
import org.junit.Test;

import static org.gbif.registry.ws.surety.OrganizationSuretyModule.PROPERTY_PREFIX;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Unit tests related to {@link OrganizationEmailTemplateManager}.
 */
public class OrganizationEmailTemplateManagerTest {

  private OrganizationEmailTemplateConfiguration config;
  private OrganizationEmailTemplateManager organizationEmailTemplateManager;

  @Before
  public void setup() throws IOException {
    final Properties p = new Properties();
    p.load(Resources.getResourceAsStream(TestConstants.APPLICATION_PROPERTIES));
    Properties filteredProperties = PropertiesUtil.filterProperties(p, PROPERTY_PREFIX);
    config = OrganizationEmailTemplateConfiguration.from(filteredProperties);

    EmailTemplateProcessor newOrganizationTP = new EmailTemplateProcessor(
            locale -> config.getEmailSubject(OrganizationEmailTemplateConfiguration.EmailType.NEW_ORGANIZATION),
            locale -> OrganizationEmailTemplateConfiguration.EmailType.NEW_ORGANIZATION.getFtlTemplate());

    EmailTemplateProcessor endorsementConfirmationTP = new EmailTemplateProcessor(
            locale -> config.getEmailSubject(OrganizationEmailTemplateConfiguration.EmailType.ENDORSEMENT_CONFIRMATION),
            locale -> OrganizationEmailTemplateConfiguration.EmailType.ENDORSEMENT_CONFIRMATION.getFtlTemplate());

    organizationEmailTemplateManager =
            new OrganizationEmailTemplateManager(newOrganizationTP,
                    endorsementConfirmationTP, config);
  }

  @Test
  public void testGenerateOrganizationEndorsementEmailModel() {
    Node endorsingNode = Nodes.newInstance();
    endorsingNode.setKey(UUID.randomUUID());
    Organization org = Organizations.newInstance(endorsingNode.getKey());
    org.setKey(UUID.randomUUID());
    BaseEmailModel baseEmail = organizationEmailTemplateManager.generateOrganizationEndorsementEmailModel(
            org, null, UUID.randomUUID(), endorsingNode);

    assertNotNull("We can generate the model from the template", baseEmail);
    //since there is no NodeManager we should not CC helpdesk (we send the email to helpdesk)
    assertNull(baseEmail.getCcAddress());

    //now try with a NodeManager
    Contact c = Contacts.newInstance();
    c.setType(ContactType.NODE_MANAGER);
    baseEmail = organizationEmailTemplateManager.generateOrganizationEndorsementEmailModel(
            org, c, UUID.randomUUID(), endorsingNode);

    assertNotNull("We can generate the model from the template", baseEmail);
    // we should have a CC to helpdesk
    assertNotNull(baseEmail.getCcAddress());
  }

  @Test
  public void testGenerateOrganizationEndorsedEmailModel() {
    Node endorsingNode = Nodes.newInstance();
    endorsingNode.setKey(UUID.randomUUID());
    Organization org = Organizations.newInstance(endorsingNode.getKey());
    org.setKey(UUID.randomUUID());
    BaseEmailModel baseEmail = organizationEmailTemplateManager.generateOrganizationEndorsedEmailModel(
            org, endorsingNode);
    assertNotNull("We can generate the model from the template", baseEmail);
  }
}
