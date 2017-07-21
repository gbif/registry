package org.gbif.registry.ws.surety;

import org.gbif.registry.surety.SuretyConstants;
import org.gbif.registry.surety.email.EmailManagerConfiguration;
import org.gbif.registry.surety.email.EmailTemplateProcessor;
import org.gbif.registry.surety.persistence.ChallengeCodeManager;
import org.gbif.registry.surety.persistence.ChallengeCodeMapper;
import org.gbif.registry.surety.persistence.ChallengeCodeSupportMapper;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
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
 *   - ChallengeCodeMapper
 *   - ChallengeCodeSupportMapper
 *
 * Binds:
 *   - OrganizationEndorsementService<UUID>
 *   - ChallengeCodeManager<UUID>
 *   - OrganizationEmailTemplateProcessor
 */
public class OrganizationSuretyModule extends AbstractModule {

  private final Properties filteredProperties;

  public static final TypeLiteral<OrganizationEndorsementService<UUID>>
          ORGANIZATION_ENDORSEMENT_SERVICE_TYPE_REF = new TypeLiteral<OrganizationEndorsementService<UUID>>(){};

  private static final ResourceBundle SUBJECT_RESOURCE = ResourceBundle.getBundle(
          "email/subjects/email_subjects", Locale.ENGLISH);

  static final String CONFIRM_ORGANIZATION_URL_TEMPLATE = "mail.urlTemplate.confirmOrganization";
  static final String HELPDESK_EMAIL_PROPERTY = "helpdesk.email";

  static final String CONFIRM_ORGANIZATION_SUBJECT_KEY = "confirmOrganization";
  static final String CONFIRM_ORGANIZATION_FTL_TEMPLATE = "confirm_organization_en.ftl";

  public OrganizationSuretyModule(Properties properties) {
    this.filteredProperties = PropertiesUtil.filterProperties(properties, "organization." + SuretyConstants.PROPERTY_PREFIX);
  }

  @Override
  protected void configure() {
    install(new InnerModule(filteredProperties));
  }

  @Provides
  @Singleton
  private OrganizationEmailTemplateProcessor provideOrganizationEmailTemplateProcessor(EmailManagerConfiguration emailManagerConfiguration) {
    EmailTemplateProcessor emailTemplateProcessor = new EmailTemplateProcessor(
            //we only support one Locale at the moment
            (locale) -> SUBJECT_RESOURCE.getString(CONFIRM_ORGANIZATION_SUBJECT_KEY),
            (locale) -> CONFIRM_ORGANIZATION_FTL_TEMPLATE);
    return new OrganizationEmailTemplateProcessor(emailTemplateProcessor,
            filteredProperties.getProperty(CONFIRM_ORGANIZATION_URL_TEMPLATE),
            filteredProperties.getProperty(HELPDESK_EMAIL_PROPERTY));
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
    private ChallengeCodeManager<UUID> provideChallengeCodeManager(ChallengeCodeMapper challengeCodeMapper, ChallengeCodeSupportMapper<UUID> challengeCodeSupportMapper) {
      return new ChallengeCodeManager<>(challengeCodeMapper, challengeCodeSupportMapper);
    }

  }
}
