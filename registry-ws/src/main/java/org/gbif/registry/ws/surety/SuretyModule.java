package org.gbif.registry.ws.surety;

import org.gbif.registry.surety.email.EmailManagerConfiguration;
import org.gbif.registry.surety.email.EmailTemplateProcessor;
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

/**
 * Guice module for related to surety.
 *
 * Requires:
 *   - OrganizationMapper
 *   - NodeMapper
 *   - EmailManagerConfiguration
 *
 * Binds:
 *   - OrganizationEndorsementService<UUID>
 *   - ChallengeCodeManager<UUID>
 *   - OrganizationEmailTemplateProcessor
 */
public class SuretyModule extends AbstractModule {

  private static final String PROPERTY_PREFIX = "surety.";
  private final Properties filteredProperties;

  public static final TypeLiteral<OrganizationEndorsementService<UUID>>
          ORGANIZATION_ENDORSEMENT_SERVICE_TYPE_REF = new TypeLiteral<OrganizationEndorsementService<UUID>>(){};

  static final String CONFIRM_ORGANIZATION_URL_TEMPLATE = "mail.urlTemplate.confirmOrganization";
  static final String HELPDESK_EMAIL_PROPERTY = "helpdesk.email";

  static final String CONFIRM_ORGANIZATION_SUBJECT_KEY = "confirmOrganization";
  static final String CONFIRM_ORGANIZATION_FTL_TEMPLATE = "confirm_organization_en.ftl";

  public SuretyModule(Properties properties) {
    this.filteredProperties = PropertiesUtil.filterProperties(properties, PROPERTY_PREFIX);
  }

  @Override
  protected void configure() {
    install(new InnerModule(filteredProperties));
  }

  private static class InnerModule extends AbstractModule {
    private Properties filteredProperties;

    public InnerModule(Properties filteredProperties) {
      this.filteredProperties = filteredProperties;
    }

    @Override
    protected void configure() {
      bind(ORGANIZATION_ENDORSEMENT_SERVICE_TYPE_REF).to(OrganizationEmailEndorsementService.class).in(Scopes.SINGLETON);
    }

    @Provides
    @Singleton
    private OrganizationEmailTemplateProcessor provideOrganizationEmailTemplateProcessor(EmailManagerConfiguration emailManagerConfiguration) {
      EmailTemplateProcessor emailTemplateProcessor = new EmailTemplateProcessor(emailManagerConfiguration,
              CONFIRM_ORGANIZATION_SUBJECT_KEY, CONFIRM_ORGANIZATION_FTL_TEMPLATE);

      return new OrganizationEmailTemplateProcessor(emailTemplateProcessor,
              filteredProperties.getProperty(CONFIRM_ORGANIZATION_URL_TEMPLATE),
              filteredProperties.getProperty(HELPDESK_EMAIL_PROPERTY));
    }

    @Provides
    @Singleton
    private ChallengeCodeManager<UUID> provideChallengeCodeManager(ChallengeCodeMapper challengeCodeMapper, ChallengeCodeSupportMapper<UUID> challengeCodeSupportMapper) {
      return new ChallengeCodeManager<>(challengeCodeMapper, challengeCodeSupportMapper);
    }

  }
}
