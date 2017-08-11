package org.gbif.identity.surety;

import org.gbif.registry.surety.email.EmailTemplateProcessor;

import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

/**
 * Internal Guice Module to wire classes related to the surety of the identity module.
 * Requires:
 * - filtered properties (identity.surety prefix removed)
 * Exposes:
 * - {@link IdentityEmailManager}
 */
public class InternalIdentitySuretyModule extends PrivateModule {

  private final IdentityEmailConfiguration identityEmailConfiguration;

  public InternalIdentitySuretyModule(Properties filteredProperties) {
    identityEmailConfiguration = IdentityEmailConfiguration.from(filteredProperties);
  }

  @Override
  protected void configure() {
    expose(IdentityEmailManager.class);
  }

  @Provides
  @Singleton
  private IdentityEmailManager provideIdentityEmailManager() {
    // create a EmailTemplateProcessor for each EmailType
    Map<IdentityEmailConfiguration.EmailType, EmailTemplateProcessor> templateProcessors
            = new EnumMap<>(IdentityEmailConfiguration.EmailType.class);
    for(IdentityEmailConfiguration.EmailType emailType : IdentityEmailConfiguration.EmailType.values()){
      templateProcessors.put(emailType, buildTemplateProcessor(emailType));
    }
    return new IdentityEmailManager(identityEmailConfiguration, templateProcessors);
  }

  private EmailTemplateProcessor buildTemplateProcessor(IdentityEmailConfiguration.EmailType emailType) {
    return new EmailTemplateProcessor(
            //we only support one Locale at the moment
            locale -> identityEmailConfiguration.getEmailSubject(emailType),
            locale -> emailType.getFtlTemplate());
  }
}
