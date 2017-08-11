package org.gbif.registry.ws.surety;


import org.gbif.registry.surety.SuretyConstants;
import org.gbif.registry.surety.email.EmailSender;
import org.gbif.registry.surety.email.EmailManagerConfiguration;
import org.gbif.registry.surety.email.EmailTemplateProcessor;
import org.gbif.registry.surety.email.EmptyEmailSender;
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
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.registry.ws.surety.OrganizationEmailConfiguration.EmailType;
import static org.gbif.registry.ws.surety.OrganizationEmailConfiguration.MAIL_ENABLED_PROPERTY;
import static org.gbif.registry.ws.surety.OrganizationEmailConfiguration.from;
import static org.gbif.registry.ws.surety.OrganizationEmailEndorsementService.ENDORSEMENT_EMAIL_MANAGER_KEY;


/**
 * Guice module for related to surety in the context of organization endorsement.
 *
 * Requires:
 *   - OrganizationMapper
 *   - NodeMapper
 *   - EmailManagerConfiguration
 *   - ChallengeCodeMapper
 *   - ChallengeCodeSupportMapper
 *   - EmailSender (unless the property organization.surety.mail.enable is set to false)
 *
 * Binds:
 *   - OrganizationEndorsementService<UUID> (ORGANIZATION_ENDORSEMENT_SERVICE_TYPE_REF)
 *   - ChallengeCodeManager<UUID>
 *   - OrganizationEmailManager
 */
public class OrganizationSuretyModule extends AbstractModule {

  private static final Logger LOG = LoggerFactory.getLogger(OrganizationSuretyModule.class);

  public static final String PROPERTY_PREFIX = "organization." + SuretyConstants.PROPERTY_PREFIX;
  private final Properties filteredProperties;
  private final OrganizationEmailConfiguration config;

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
  private OrganizationEmailManager provideOrganizationEmailTemplateProcessor(EmailManagerConfiguration emailManagerConfiguration) {
    return new OrganizationEmailManager(
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
    private final OrganizationEmailConfiguration config;

    InnerModule(OrganizationEmailConfiguration config) {
      this.config = config;
    }

    @Override
    protected void configure() {
      bind(ORGANIZATION_ENDORSEMENT_SERVICE_TYPE_REF).to(OrganizationEmailEndorsementService.class).in(Scopes.SINGLETON);
    }

    /**
     * We want to avoid sending emails unless the configuration says otherwise.
     * (OrganizationEmailEndorsementService sends emails to NodeManagers)
     * The idea here is to bind the {@link EmailSender} annotated with {@link OrganizationEmailEndorsementService#ENDORSEMENT_EMAIL_MANAGER_KEY}
     * depending on the configuration. It is not recommended to have conditional logic in modules so we added it to test coverage.
     *
     * @param emailManager shared emailManager instance coming from registry-surety module
     * @return the provided {@link EmailSender} if the configuration says emailEnabled or {@link EmptyEmailSender} if not.
     */
    @Provides
    @Singleton
    @Named(ENDORSEMENT_EMAIL_MANAGER_KEY)
    private EmailSender provideEndorsementEmailManager(EmailSender emailManager) {
      if(!config.isEmailEnabled()) {
        LOG.info("email sending feature is disabled (" + PROPERTY_PREFIX + MAIL_ENABLED_PROPERTY + " is not set or false)");
        return new EmptyEmailSender();
      }
      return emailManager;
    }

    @Provides
    @Singleton
    private ChallengeCodeManager<UUID> provideChallengeCodeManager(ChallengeCodeMapper challengeCodeMapper, ChallengeCodeSupportMapper<UUID> challengeCodeSupportMapper) {
      return new ChallengeCodeManager<>(challengeCodeMapper, challengeCodeSupportMapper);
    }

  }
}
