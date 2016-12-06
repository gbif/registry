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
package org.gbif.registry.guice;

import org.gbif.registry.doi.DoiModule;
import org.gbif.registry.events.EventModule;
import org.gbif.registry.grizzly.RegistryServer;
import org.gbif.registry.persistence.guice.RegistryMyBatisModule;
import org.gbif.registry.search.DatasetIndexService;
import org.gbif.registry.search.guice.RegistrySearchModule;
import org.gbif.registry.utils.OaipmhTestConfiguration;
import org.gbif.registry.ws.guice.SecurityModule;
import org.gbif.registry.ws.guice.StringTrimInterceptor;
import org.gbif.registry.ws.security.LegacyAuthorizationFilter;
import org.gbif.utils.file.properties.PropertiesUtil;
import org.gbif.ws.server.guice.GbifServletListener;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import javax.servlet.ServletContextEvent;

import com.google.common.collect.Lists;
import com.google.inject.Module;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import org.apache.bval.guice.ValidationModule;

/**
 * The Registry WS module for testing in Grizzly.
 * This is the same as the production listener except that:
 * <ol>
 * <li>The registry-test.properties file is used</li>
 * <li>A life-cycle event monitor registers the SOLR server and Updater for JVM wide interaction</li>
 * </ol>
 */
public class TestRegistryWsServletListener extends GbifServletListener {

  public static final String APPLICATION_PROPERTIES = "registry-test.properties";
  private static final String SOLR_HOME_PROPERTY = "solr.dataset.home";

  @SuppressWarnings("unchecked")
  public final static List<Class<? extends ContainerRequestFilter>> requestFilters = Lists
    .<Class<? extends ContainerRequestFilter>>newArrayList(LegacyAuthorizationFilter.class);

  public TestRegistryWsServletListener() throws IOException {
    super(renameSolrHome(PropertiesUtil.loadProperties(APPLICATION_PROPERTIES)),
      "org.gbif.registry.ws,org.gbif.registry.ws.provider,org.gbif.registry.oaipmh", true, null, requestFilters);
  }

  /**
   * In integration tests we run 2 solr indices concurrently. One for webservice tests and one for ws-client tests within grizzly.
   * We need to use different solr homes for those 2 so they do not interfere with data & locking problems.
   * @param props
   */
  private static Properties renameSolrHome(Properties props) {
    props.setProperty(SOLR_HOME_PROPERTY, props.get(SOLR_HOME_PROPERTY)+"2");
    return props;
  }

  @Override
  protected List<Module> getModules(Properties props) {
    return Lists.<Module>newArrayList(new RegistryMyBatisModule(props),
      new DoiModule(props),
      new RabbitMockModule(),
      new DirectoryMockModule(),
      new DrupalMockModule(),
      StringTrimInterceptor.newMethodInterceptingModule(),
      new ValidationModule(),
      new EventModule(props),
      new RegistrySearchModule(props),
      new SecurityModule(props),
      new TitleLookupMockModule(),
      new OaipmhMockModule(OaipmhTestConfiguration.buildTestRepositoryConfiguration(props))
    );
  }

  /**
   * After startup registers the SOLR server and index updater, to allow WS client tests to interact.
   */
  @Override
  public void contextInitialized(ServletContextEvent servletContextEvent) {
    super.contextInitialized(servletContextEvent);
    RegistryServer.INSTANCE.setSolrClient(getInjector().getInstance(RegistrySearchModule.DATASET_KEY));
    RegistryServer.INSTANCE.setIndexService(getInjector().getInstance(DatasetIndexService.class));
  }

}
