package org.gbif.registry.ws.surety;


import org.gbif.registry.surety.SuretyConstants;
import org.gbif.registry.surety.email.EmailManager;
import org.gbif.registry.surety.email.EmailManagerConfiguration;
import org.gbif.registry.surety.email.EmailTemplateProcessor;
import org.gbif.registry.surety.email.EmptyEmailManager;
import org.gbif.registry.surety.persistence.ChallengeCodeManager;
import org.gbif.registry.surety.persistence.ChallengeCodeMapper;
import org.gbif.registry.surety.persistence.ChallengeCodeSupportMapper;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.util.Properties;
import java.util.UUID;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.registry.ws.surety.OrganizationEmailEndorsementService.ENDORSEMENT_EMAIL_MANAGER_KEY;
import static org.gbif.registry.ws.surety.OrganizationEmailTemplateConfiguration.EmailType;
import static org.gbif.registry.ws.surety.OrganizationEmailTemplateConfiguration.MAIL_ENABLED_PROPERTY;
import static org.gbif.registry.ws.surety.OrganizationEmailTemplateConfiguration.from;


/**
 * Guice module for related to surety in the context of organization endorsement.
 *
 * Requires:
 *   - OrganizationMapper
 *   - NodeMapper
 *   - EmailManagerConfiguration
 *   - ChallengeCodeMapper
 *   - ChallengeCodeSupportMapper
 *   - EmailManager (unless the property organization.surety.mail.enable is set to false)
 *
 * Binds:
 *   - OrganizationEndorsementService<UUID>
 *   - ChallengeCodeManager<UUID>
 *   - OrganizationEmailTemplateManager
 */
public class OrganizationSuretyModule extends AbstractModule {

  private static final Logger LOG = LoggerFactory.getLogger(OrganizationSuretyModule.class);

  public static final String PROPERTY_PREFIX = "organization." + SuretyConstants.PROPERTY_PREFIX;
  private final Properties filteredProperties;
  private final OrganizationEmailTemplateConfiguration config;

  public static final TypeLiteral<OrganizationEndorsementService<UUID>>
          ORGANIZATION_ENDORSEMENT_SERVICE_TYPE_REF = new TypeLiteral<OrganizationEndorsementService<UUID>>(){};

  public OrganizationSuretyModule(Properties properties) {
    this.filteredProperties = PropertiesUtil.filterProperties(properties, PROPERTY_PREFIX);
    config = from(filteredProperties);
  }

  @Override
  protected void configure() {
    install(new InnerModule(config));
  }

  @Provides
  @Singleton
  private OrganizationEmailTemplateManager provideOrganizationEmailTemplateProcessor(EmailManagerConfiguration emailManagerConfiguration) {
    return new OrganizationEmailTemplateManager(
            provideEmailTemplateProcessor(EmailType.NEW_ORGANIZATION),
            provideEmailTemplateProcessor(EmailType.ENDORSEMENT_CONFIRMATION),
            config);
  }

  /**
   * Provides a {@link EmailTemplateProcessor} for a specific {@link EmailType}
   * @param organizationEmailType
   * @return
   */
  private EmailTemplateProcessor provideEmailTemplateProcessor(EmailType organizationEmailType) {
    return new EmailTemplateProcessor(
            //we only support one Locale at the moment
            locale -> config.getEmailSubject(organizationEmailType),
            locale -> organizationEmailType.getFtlTemplate());
  }

  private static class InnerModule extends AbstractModule {
    private final OrganizationEmailTemplateConfiguration config;

    InnerModule(OrganizationEmailTemplateConfiguration config) {
      this.config = config;
    }

    @Override
    protected void configure() {
      // avoid sending emails unless the configuration says otherwise
      // (OrganizationEmailEndorsementService sends emails to NodeManagers)
      if(!config.isEmailEnabled()) {
        LOG.info("email sending feature is disabled (" + PROPERTY_PREFIX + MAIL_ENABLED_PROPERTY + " is not set or false)");
        bind(EmailManager.class).annotatedWith(Names.named(ENDORSEMENT_EMAIL_MANAGER_KEY)).toInstance(new EmptyEmailManager());
      }
      else{
        bind(EmailManager.class).annotatedWith(Names.named(ENDORSEMENT_EMAIL_MANAGER_KEY));
      }
      bind(ORGANIZATION_ENDORSEMENT_SERVICE_TYPE_REF).to(OrganizationEmailEndorsementService.class).in(Scopes.SINGLETON);
    }

    @Provides
    @Singleton
    private ChallengeCodeManager<UUID> provideChallengeCodeManager(ChallengeCodeMapper challengeCodeMapper, ChallengeCodeSupportMapper<UUID> challengeCodeSupportMapper) {
      return new ChallengeCodeManager<>(challengeCodeMapper, challengeCodeSupportMapper);
    }

  }
}
