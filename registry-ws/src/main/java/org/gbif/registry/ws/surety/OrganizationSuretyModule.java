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

import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.UUID;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Guice module for related to surety.
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
 *   - OrganizationEmailTemplateProcessor
 */
public class OrganizationSuretyModule extends AbstractModule {

  private static final Logger LOG = LoggerFactory.getLogger(OrganizationSuretyModule.class);

  private static final String ENDORSEMENT_EMAIL_MANAGER_KEY = "endorsementEmailManager";
  private static final String PROPERTY_PREFIX = "organization." + SuretyConstants.PROPERTY_PREFIX;
  private final Properties filteredProperties;

  public static final TypeLiteral<OrganizationEndorsementService<UUID>>
          ORGANIZATION_ENDORSEMENT_SERVICE_TYPE_REF = new TypeLiteral<OrganizationEndorsementService<UUID>>(){};

  private static final ResourceBundle SUBJECT_RESOURCE = ResourceBundle.getBundle(
          "email/subjects/email_subjects", Locale.ENGLISH);

  static final String CONFIRM_ORGANIZATION_URL_TEMPLATE = "mail.urlTemplate.confirmOrganization";
  static final String MAIL_ENABLED_PROPERTY = "mail.enable";
  static final String HELPDESK_EMAIL_PROPERTY = "helpdesk.email";

  static final String CONFIRM_ORGANIZATION_SUBJECT_KEY = "confirmOrganization";
  static final String CONFIRM_ORGANIZATION_FTL_TEMPLATE = "confirm_organization_en.ftl";

  public OrganizationSuretyModule(Properties properties) {
    this.filteredProperties = PropertiesUtil.filterProperties(properties, PROPERTY_PREFIX);
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

    InnerModule(Properties filteredProperties) {
      this.filteredProperties = filteredProperties;
    }

    @Override
    protected void configure() {
      // avoid sending emails unless the configuration says otherwise
      // (OrganizationEmailEndorsementService sends emails to NodeManagers)
      if(!BooleanUtils.toBoolean(filteredProperties.getProperty(MAIL_ENABLED_PROPERTY))) {
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
