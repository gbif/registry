/*
 * Copyright 2013 Global Biodiversity Information Facility (GBIF)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.ws.guice;

import org.gbif.drupal.guice.DrupalMyBatisModule;
import org.gbif.occurrence.query.TitleLookupModule;
import org.gbif.registry.doi.DoiModule;
import org.gbif.registry.events.EventModule;
import org.gbif.registry.events.VarnishPurgeModule;
import org.gbif.registry.ims.ImsModule;
import org.gbif.registry.oaipmh.guice.OaipmhModule;
import org.gbif.registry.persistence.guice.RegistryMyBatisModule;
import org.gbif.registry.search.guice.RegistrySearchModule;
import org.gbif.registry.ws.filter.AuthResponseCodeOverwriteFilter;
import org.gbif.registry.ws.security.EditorAuthorizationFilter;
import org.gbif.registry.ws.security.LegacyAuthorizationFilter;
import org.gbif.utils.file.properties.PropertiesUtil;
import org.gbif.ws.app.ConfUtils;
import org.gbif.ws.server.guice.GbifServletListener;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import org.apache.bval.guice.ValidationModule;

/**
 * The Registry WS module.
 */
public class RegistryWsServletListener extends GbifServletListener {

  public static final String APP_CONF_FILE = "registry.properties";

  private static final String API_URL_PROPERTY = "api.url";
  private static final String OAIPMH_BASE_URL_PROPERTY = "oaipmh.baseUrl";
  private static final String OAIPMH_ADMIN_EMAIL_PROPERTY = "oaipmh.adminEmail";

  public static final List<Class<? extends ContainerRequestFilter>> requestFilters = Lists.newArrayList();
  public static final List<Class<? extends ContainerResponseFilter>> responseFilters = Lists.newArrayList();

  static {
    requestFilters.add(LegacyAuthorizationFilter.class);
    requestFilters.add(EditorAuthorizationFilter.class);
    responseFilters.add(AuthResponseCodeOverwriteFilter.class);

  }

  private static final String PACKAGES = "org.gbif.registry.ws.resources, org.gbif.registry.ws.provider, org.gbif.registry.oaipmh";

  public RegistryWsServletListener() throws IOException {
    super(PropertiesUtil.readFromFile(ConfUtils.getAppConfFile(APP_CONF_FILE)), PACKAGES, true, responseFilters, requestFilters);
  }

  @VisibleForTesting
  public RegistryWsServletListener(Properties properties) {
    super(properties, PACKAGES, true, null, requestFilters);
  }

  @Override
  protected List<Module> getModules(Properties properties) {
    return Lists.newArrayList(new DoiModule(properties),
                              new RegistryMyBatisModule(properties),
                              new DrupalMyBatisModule(properties),
                              new ImsModule(),
                              StringTrimInterceptor.newMethodInterceptingModule(),
                              new ValidationModule(),
                              new EventModule(properties),
                              new RegistrySearchModule(properties),
                              new SecurityModule(properties),
                              new VarnishPurgeModule(properties),
                              new TitleLookupModule(true, properties.getProperty(API_URL_PROPERTY)),
                              new OaipmhModule(properties.getProperty(OAIPMH_BASE_URL_PROPERTY), properties.getProperty(OAIPMH_ADMIN_EMAIL_PROPERTY)));
  }

  @VisibleForTesting
  @Override
  protected Injector getInjector() {
    return super.getInjector();
  }
}
