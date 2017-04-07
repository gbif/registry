package org.gbif.identity.email;

import java.io.IOException;
import java.net.URL;

import freemarker.template.TemplateException;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static org.gbif.identity.email.IdentityEmailManagerConfiguration.RESET_PASSWORD_TEMPLATE;
import static org.gbif.identity.email.IdentityEmailManagerConfiguration.USER_CREATE_TEMPLATE;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class IdentityEmailManagerImplTest {

  /**
   * Mostly test the Freemarker template to ensure it can produce text.
   * @throws IOException
   * @throws TemplateException
   */
  @Test
  public void testGenerateUserCreateEmailBody() throws IOException, TemplateException {
    IdentityEmailManagerImpl identityEmailManager = new IdentityEmailManagerImpl(new IdentityEmailManagerConfiguration());

    BaseEmailModel emailModel = new BaseEmailModel("thanos", new URL("http://localhost/click_here"));
    String emailBody = identityEmailManager.buildEmailBody(emailModel, USER_CREATE_TEMPLATE);
    assertTrue(StringUtils.isNotBlank(emailBody));
  }

  /**
   * Mostly test the Freemarker template to ensure it can produce text.
   * @throws IOException
   * @throws TemplateException
   */
  @Test
  public void testGenerateResetPasswordEmail() throws IOException, TemplateException {
    IdentityEmailManagerImpl identityEmailManager = new IdentityEmailManagerImpl(new IdentityEmailManagerConfiguration());

    BaseEmailModel emailModel = new BaseEmailModel("thanos", new URL("http://localhost/click_here"));
    String emailBody = identityEmailManager.buildEmailBody(emailModel, RESET_PASSWORD_TEMPLATE);
    assertTrue(StringUtils.isNotBlank(emailBody));
  }

  /**
   * This test ensures we can load the default ResourceBundle containing the different email subjects.
   */
  @Test
  public void testIdentityEmailManagerConfiguration() {
    IdentityEmailManagerConfiguration cfg = new IdentityEmailManagerConfiguration();
    assertNotNull(cfg.getDefaultEmailSubjects().getString(IdentityEmailManagerConfiguration.USER_CREATE_SUBJECT_KEY));
  }
}
