package org.gbif.registry.identity.surety;

import org.gbif.registry.surety.email.EmailDataProvider;
import org.gbif.registry.surety.email.FreemarkerEmailTemplateProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("identityEmailTemplateProcessor")
public class IdentityEmailTemplateProcessor extends FreemarkerEmailTemplateProcessor {

  private EmailDataProvider emailDataProvider;

  /**
   * @param emailDataProvider provides subject and template (depends on locale).
   */
  public IdentityEmailTemplateProcessor(@Qualifier("identityEmailDataProvider") EmailDataProvider emailDataProvider) {
    this.emailDataProvider = emailDataProvider;
  }

  @Override
  public EmailDataProvider getEmailDataProvider() {
    return emailDataProvider;
  }
}
