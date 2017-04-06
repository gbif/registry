package org.gbif.identity.email;

import java.io.IOException;
import java.net.URL;

import freemarker.template.TemplateException;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

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
    IdentityEmailManagerImpl identityEmailManager = new IdentityEmailManagerImpl(null, null);

    BaseEmailModel emailModel = new BaseEmailModel("thanos", new URL("http://localhost/click_here"));
    String emailBody = identityEmailManager.buildEmailBody(emailModel, IdentityEmailManagerImpl.USER_CREATE_TEMPLATE);
    assertTrue(StringUtils.isNotBlank(emailBody));
  }

  /**
   * Mostly test the Freemarker template to ensure it can produce text.
   * @throws IOException
   * @throws TemplateException
   */
  @Test
  public void testGenerateResetPasswordEmail() throws IOException, TemplateException {
    IdentityEmailManagerImpl identityEmailManager = new IdentityEmailManagerImpl(null, null);

    BaseEmailModel emailModel = new BaseEmailModel("thanos", new URL("http://localhost/click_here"));
    String emailBody = identityEmailManager.buildEmailBody(emailModel, IdentityEmailManagerImpl.RESET_PASSWORD_TEMPLATE);
    assertTrue(StringUtils.isNotBlank(emailBody));
  }
}
