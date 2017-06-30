package org.gbif.identity.service;

import org.gbif.api.service.common.IdentityService;
import org.gbif.api.service.common.UserService;
import org.gbif.identity.IdentityConstants;
import org.gbif.identity.mybatis.InternalIdentityMyBatisModule;
import org.gbif.identity.mybatis.UserServiceImpl;
import org.gbif.registry.surety.SuretyConstants;
import org.gbif.registry.surety.email.EmailTemplateProcessor;
import org.gbif.registry.surety.persistence.ChallengeCodeManager;
import org.gbif.registry.surety.persistence.ChallengeCodeMapper;
import org.gbif.registry.surety.persistence.ChallengeCodeSupportMapper;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import com.google.inject.Key;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.identity.IdentityConstants.CONFIRM_ORGANIZATION_URL_TEMPLATE;
import static org.gbif.identity.IdentityConstants.DB_PROPERTY_PREFIX;
import static org.gbif.identity.IdentityConstants.EMAIL_SUBJECTS_RESOURCE;
import static org.gbif.identity.IdentityConstants.RESET_PASSWORD_URL_TEMPLATE;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

/**
 * Identity Service Module using mybatis as source for data.
 * This module is private to avoid exposing the mybatis layer.
 *
 * Requires:
 * - properties identity.db.*, identity.appkeys.whitelist
 * Binds and Exposes: {@link IdentityService}, {@link #APPKEYS_WHITELIST}
 */
public class IdentityServiceModule extends PrivateModule {

  private static final Logger LOG = LoggerFactory.getLogger(IdentityServiceModule.class);

  public static final String CHALLENGE_CODE_SUPPORT_MAPPER_TYPE_NAME = "identityChallengeCodeSupportMapper";
  public static final TypeLiteral<ChallengeCodeSupportMapper<Integer>> CHALLENGE_CODE_SUPPORT_MAPPER_TYPE_LITERAL =
          new TypeLiteral<ChallengeCodeSupportMapper<Integer>>() {};

  public static final  String APPKEYS_WHITELIST = "identity.appkeys.whitelist";

  //identity (users)
  static final String USER_CREATE_SUBJECT_KEY = "createAccount";
  static final String USER_CREATE_FTL_TEMPLATE = "create_confirmation_en.ftl";

  static final String RESET_PASSWORD_SUBJECT_KEY = "resetPassword";
  static final String RESET_PASSWORD_FTL_TEMPLATE = "reset_password_en.ftl";

  private static final ResourceBundle SUBJECT_RESOURCE = ResourceBundle.getBundle(EMAIL_SUBJECTS_RESOURCE, Locale.ENGLISH);

  private final Properties rawProperties;
  private final Properties filteredProperties;
  private final List<String> appKeyWhitelist;

  public IdentityServiceModule(Properties properties) {
    rawProperties = properties;
    //the prefix is composed since we have surety within identity
    filteredProperties = PropertiesUtil.filterProperties(properties, IdentityConstants.PROPERTY_PREFIX + SuretyConstants.PROPERTY_PREFIX);

    String appkeysWl = properties.getProperty(APPKEYS_WHITELIST);
    if(StringUtils.isNotBlank(appkeysWl)){
      appKeyWhitelist = Arrays.stream(appkeysWl.split(","))
              .map(String::trim)
              .collect(collectingAndThen(toList(), Collections::unmodifiableList));
      LOG.info("appKeyWhitelist: " + appKeyWhitelist);
    }
    else{
      appKeyWhitelist = Collections.EMPTY_LIST;
      LOG.warn("No appKeyWhitelist found. No appKey will be accepted.");
    }
  }

  @Override
  protected void configure() {
    // bind classes
    install(new InternalIdentityMyBatisModule(PropertiesUtil.filterProperties(rawProperties, DB_PROPERTY_PREFIX)));

    bind(UserSuretyService.class).to(UserSuretyServiceImpl.class).in(Scopes.SINGLETON);
    bind(IdentityService.class).to(IdentityServiceImpl.class).in(Scopes.SINGLETON);
    bind(UserService.class).to(UserServiceImpl.class).in(Scopes.SINGLETON);

    expose(IdentityService.class);
    expose(UserService.class);

    bind(new TypeLiteral<List<String>>() {}).annotatedWith(Names.named(APPKEYS_WHITELIST)).toInstance(appKeyWhitelist);
    expose(Key.get(new TypeLiteral<List<String>>() {}, Names.named(APPKEYS_WHITELIST)));
  }

  @Provides
  @Singleton
  @Named("newUserEmailTemplateProcessor")
  private IdentityEmailTemplateProcessor provideNewUserEmailTemplateProcessor() {
    EmailTemplateProcessor emailTemplateProcessor = new EmailTemplateProcessor(
            //we only support one Locale at the moment
            (locale) -> SUBJECT_RESOURCE.getString(USER_CREATE_SUBJECT_KEY),
            (locale) -> USER_CREATE_FTL_TEMPLATE);
    return new IdentityEmailTemplateProcessor(emailTemplateProcessor,
            filteredProperties.getProperty(CONFIRM_ORGANIZATION_URL_TEMPLATE));
  }

  @Provides
  @Singleton
  @Named("resetPasswordEmailTemplateProcessor")
  private IdentityEmailTemplateProcessor provideResetPasswordEmailTemplateProcessor() {
    EmailTemplateProcessor emailTemplateProcessor = new EmailTemplateProcessor(
            //we only support one Locale at the moment
            (locale) -> SUBJECT_RESOURCE.getString(RESET_PASSWORD_SUBJECT_KEY),
            (locale) -> RESET_PASSWORD_FTL_TEMPLATE);
    return new IdentityEmailTemplateProcessor(emailTemplateProcessor,
            filteredProperties.getProperty(RESET_PASSWORD_URL_TEMPLATE));
  }

  @Provides
  @Singleton
  private ChallengeCodeManager<Integer> provideChallengeCodeManager(ChallengeCodeMapper challengeCodeMapper,
                                                                    @Named(CHALLENGE_CODE_SUPPORT_MAPPER_TYPE_NAME)
                                                                            ChallengeCodeSupportMapper<Integer> challengeCodeSupportMapper) {
    return new ChallengeCodeManager<>(challengeCodeMapper, challengeCodeSupportMapper);
  }

}
