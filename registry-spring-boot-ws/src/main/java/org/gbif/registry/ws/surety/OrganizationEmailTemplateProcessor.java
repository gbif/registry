package org.gbif.registry.ws.surety;

import org.gbif.registry.surety.email.EmailDataProvider;
import org.gbif.registry.surety.email.FreemarkerEmailTemplateProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("organizationEmailTemplateProcessor")
public class OrganizationEmailTemplateProcessor extends FreemarkerEmailTemplateProcessor {

  private final EmailDataProvider emailDataProvider;

  public OrganizationEmailTemplateProcessor(@Qualifier("organizationEmailDataProvider") EmailDataProvider emailDataProvider) {
    this.emailDataProvider = emailDataProvider;
  }

  @Override
  public EmailDataProvider getEmailDataProvider() {
    return emailDataProvider;
  }
}
