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

import org.gbif.identity.guice.IdentityMyBatisModule;
import org.gbif.occurrence.query.TitleLookupModule;
import org.gbif.registry.directory.DirectoryModule;
import org.gbif.registry.doi.DoiModule;
import org.gbif.registry.events.EventModule;
import org.gbif.registry.events.VarnishPurgeModule;
import org.gbif.registry.metrics.guice.OccurrenceMetricsModule;
import org.gbif.registry.oaipmh.guice.OaipmhModule;
import org.gbif.registry.persistence.guice.RegistryMyBatisModule;
import org.gbif.registry.search.guice.RegistrySearchModule;
import org.gbif.registry.ws.filter.AuthResponseCodeOverwriteFilter;
import org.gbif.registry.ws.security.EditorAuthorizationFilter;
import org.gbif.registry.ws.security.LegacyAuthorizationFilter;
import org.gbif.utils.file.properties.PropertiesUtil;
import org.gbif.ws.app.ConfUtils;
import org.gbif.ws.client.guice.GbifWsClientModule;
import org.gbif.ws.mixin.Mixins;
import org.gbif.ws.server.guice.GbifServletListener;

import java.io.IOException;
import java.util.List;
import java.util.Map;
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
  // fail fast by designed, see CubeService usage
  private static final String METRICS_WS_HTTP_TIMEOUT = "100";

  public static final List<Class<? extends ContainerRequestFilter>> requestFilters = Lists.newArrayList();
  public static final List<Class<? extends ContainerResponseFilter>> responseFilters = Lists.newArrayList();

  static {
    requestFilters.add(LegacyAuthorizationFilter.class);
    requestFilters.add(EditorAuthorizationFilter.class);
    responseFilters.add(AuthResponseCodeOverwriteFilter.class);
  }

  private static final String PACKAGES = "org.gbif.registry.ws.resources, org.gbif.registry.ws.provider, org.gbif.registry.oaipmh";

  /**
   * Get a subset of properties related to metrics.
   * Uses the api.url to fill metrics.ws.url and set a small timeout (100 ms) for http
   * This methods only exists because {@link org.gbif.ws.client.guice.GbifWsClientModule} uses Names.bindProperties(binder(), properties);
   * which would lead to multiple bindings for the same property.
   *
   * @param properties
   * @return
   */
  private static Properties getMetricsProperties(Properties properties){
    Properties metricsProperties = new Properties();
    metricsProperties.setProperty("metrics.ws.url", properties.getProperty(API_URL_PROPERTY));
    metricsProperties.setProperty(GbifWsClientModule.HttpClientConnParams.HTTP_TIMEOUT, METRICS_WS_HTTP_TIMEOUT);
    return metricsProperties;
  }

  public RegistryWsServletListener() throws IOException {
    super(PropertiesUtil.readFromFile(ConfUtils.getAppConfFile(APP_CONF_FILE)), PACKAGES, true, responseFilters, requestFilters);
  }

  @VisibleForTesting
  public RegistryWsServletListener(Properties properties) {
    super(properties, PACKAGES, true, null, requestFilters);
  }

  @Override
  protected Map<Class<?>, Class<?>> getMixIns() {
    return Mixins.getPredefinedMixins();
  }

  @Override
  protected List<Module> getModules(Properties properties) {
    return Lists.newArrayList(new DoiModule(properties),
                              new RegistryMyBatisModule(properties),
                              new IdentityMyBatisModule(properties),
                              new DirectoryModule(properties),
                              StringTrimInterceptor.newMethodInterceptingModule(),
                              new ValidationModule(),
                              new EventModule(properties),
                              new RegistrySearchModule(properties),
                              new SecurityModule(properties),
                              new VarnishPurgeModule(properties),
                              new TitleLookupModule(true, properties.getProperty(API_URL_PROPERTY)),
                              new OccurrenceMetricsModule(getMetricsProperties(properties)),
                              new OaipmhModule(properties));
  }

  @VisibleForTesting
  @Override
  protected Injector getInjector() {
    return super.getInjector();
  }
}
