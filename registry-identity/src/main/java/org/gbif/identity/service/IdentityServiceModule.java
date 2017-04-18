package org.gbif.identity.service;

import org.gbif.api.service.common.IdentityService;
import org.gbif.api.service.common.UserService;
import org.gbif.identity.mybatis.InternalIdentityMyBatisModule;
import org.gbif.identity.mybatis.UserServiceImpl;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.google.inject.Key;
import com.google.inject.PrivateModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

/**
 * Identity Service Module using mybatis as source for data.
 * This module is private to avoid exposing the mybatis layer.
 *
 * Requires: properties identity.db.*, identity.appkeys.whitelist
 * Binds and Exposes: {@link IdentityService}, {@link #APPKEYS_WHITELIST}
 */
public class IdentityServiceModule extends PrivateModule {

  private static final Logger LOG = LoggerFactory.getLogger(IdentityServiceModule.class);

  public static final String PROPERTY_PREFIX = "identity.db.";
  public static final  String APPKEYS_WHITELIST = "identity.appkeys.whitelist";

  private final Properties filteredProperties;
  private final List<String> appKeyWhitelist;

  public IdentityServiceModule(Properties properties) {
    filteredProperties = PropertiesUtil.filterProperties(properties, PROPERTY_PREFIX);

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
    install(new InternalIdentityMyBatisModule(filteredProperties));
    bind(IdentityService.class).to(IdentityServiceImpl.class).in(Scopes.SINGLETON);
    bind(UserService.class).to(UserServiceImpl.class).in(Scopes.SINGLETON);

    expose(IdentityService.class);
    expose(UserService.class);

    bind(new TypeLiteral<List<String>>() {}).annotatedWith(Names.named(APPKEYS_WHITELIST)).toInstance(appKeyWhitelist);
    expose(Key.get(new TypeLiteral<List<String>>() {}, Names.named(APPKEYS_WHITELIST)));
  }

}
