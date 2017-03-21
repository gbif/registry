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

import org.gbif.api.model.common.User;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.doi.DoiModule;
import org.gbif.registry.events.EventModule;
import org.gbif.registry.grizzly.RegistryServer;
import org.gbif.registry.persistence.guice.RegistryMyBatisModule;
import org.gbif.registry.search.guice.RegistrySearchModule;
import org.gbif.registry.ws.client.guice.RegistryWsClientModule;
import org.gbif.registry.ws.guice.SecurityModule;
import org.gbif.registry.ws.guice.TestValidateInterceptor;
import org.gbif.registry.ws.resources.DatasetResource;
import org.gbif.registry.ws.resources.DoiRegistrationResource;
import org.gbif.registry.ws.resources.InstallationResource;
import org.gbif.registry.ws.resources.NetworkResource;
import org.gbif.registry.ws.resources.NodeResource;
import org.gbif.registry.ws.resources.OrganizationResource;
import org.gbif.registry.ws.resources.legacy.IptResource;
import org.gbif.ws.client.guice.GbifApplicationAuthModule;
import org.gbif.ws.client.guice.SingleUserAuthModule;
import org.gbif.ws.server.filter.AuthFilter;

import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import javax.ws.rs.core.SecurityContext;

import com.google.common.base.Throwables;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.bval.guice.ValidationModule;
import org.apache.ibatis.io.Resources;

/**
 * Utility to provide the different Guice configurations for:
 * <ol>
 * <li>The WS service layer</li>
 * <li>The WS service client layer</li>
 * <li>A management configuration to allow utilities to manipulate the database (Liquibase etc)</li>
 * </ol>
 * Everything is cached, and reused on subsequent calls.
 * This is used for Integration testing.
 */
public class RegistryTestModules {

  // cache everything, for reuse in repeated calls (e.g. IDE test everything)
  private static Injector mybatis;
  private static Injector webservice;
  private static Injector webserviceClient;
  private static Injector webserviceBasicAuthClient;
  private static Injector management;
  private static HikariDataSource managementDatasource;

  public static final String WS_URL = "http://localhost:" + RegistryServer.getPort();

  /**
   * @return An injector that is bound for the mybatis layer and exposes mappers.
   */
  public static synchronized Injector mybatis() {
    if (mybatis == null) {
      try {
        final Properties p = new Properties();
        p.load(Resources.getResourceAsStream("registry-test.properties"));
        mybatis =
          Guice.createInjector(new RegistryMyBatisModule(p));
      } catch (IOException e) {
        throw Throwables.propagate(e);
      }
    }
    return mybatis;
  }

  /**
   * @return An injector that is bound for the webservice layer without SOLR capabilities.
   */
  public static synchronized Injector webservice() {
    //Setting this property because the default value in the solrconfig.xml is solr.lock.type=hdfs
    System.setProperty("solr.lock.type", "native");

    if (webservice == null) {
      try {
        final Properties p = new Properties();
        p.load(Resources.getResourceAsStream("registry-test.properties"));
        webservice =
          Guice.createInjector(new AbstractModule() {

            @Override
            protected void configure() {
              bind(NodeResource.class);
              bind(OrganizationResource.class);
              bind(InstallationResource.class);
              bind(DatasetResource.class);
              bind(NetworkResource.class);
              bind(IptResource.class);
              bind(DoiRegistrationResource.class);
              bind(SecurityContext.class).annotatedWith(Names.named("guiceInjectedSecurityContext")).toInstance(mockAdmin());
            }
          }, TestValidateInterceptor.newMethodInterceptingModule(),
            new IdentityMockModule(), new RegistryMyBatisModule(p), new DirectoryMockModule(), new RegistrySearchModule(p),
            new EventModule(p), new ValidationModule(), new SecurityModule(p), new DoiModule(p), new RabbitMockModule(),
            new TitleLookupMockModule());
      } catch (IOException e) {
        throw Throwables.propagate(e);
      }
    }
    return webservice;
  }

  private static SecurityContext mockAdmin() {
    User user = new User();
    user.setUserName("admin");
    user.setFirstName("Veronica");
    user.setLastName("Meier");
    user.setEmail("veronica@mailinator.com");
    user.setKey(1);
    Set<UserRole> roles = new HashSet<UserRole>();
    roles.add(UserRole.ADMIN);
    user.setRoles(roles);
    AuthFilter af = new AuthFilter(null, null);
    return af.new Authorizer(user, "");
  }

  /**
   * @return An injector that is bound for the webservice client layer.
   */
  public static synchronized Injector webserviceClient() {
    if (webserviceClient == null) {
      Properties props = new Properties();
      props.setProperty("registry.ws.url", "http://localhost:" + RegistryServer.getPort());
      props.setProperty("application.key", "gbif.registry-ws-client-it");
      props.setProperty("application.secret", "6a55ca16c053e269a9602c02922b30ce49c49be3a68bb2d8908b24d7c1");
      // Create authentication module, and set principal name, equal to a GBIF User unique account name
      GbifApplicationAuthModule auth = new GbifApplicationAuthModule(props);
      auth.setPrincipal("admin");
      webserviceClient = Guice.createInjector(new RegistryWsClientModule(props), auth);
    }
    return webserviceClient;
  }


  /**
   * @return An injector that is bound for the webservice client layer.
   */
  public static synchronized Injector webserviceBasicAuthClient(String username, String password) {
    if (webserviceBasicAuthClient == null) {
      Properties props = new Properties();
      props.setProperty("registry.ws.url", "http://localhost:" + RegistryServer.getPort());
      // Create authentication module, and set principal name, equal to a GBIF User unique account name
      SingleUserAuthModule auth = new SingleUserAuthModule(username, password);
      webserviceClient = Guice.createInjector(new RegistryWsClientModule(props), auth);
    }
    return webserviceClient;
  }

  /**
   * @return A datasource that is for use in management activities such as Liquibase, or cleaning between tests.
   */
  public static HikariDataSource database() {
    if (managementDatasource == null) {
      managementDatasource = RegistryTestModules.management().getInstance(HikariDataSource.class);
    }
    return managementDatasource;
  }

  /**
   * @return An injector configured to issue a Datasource suitable for database management activities (Liquibase etc).
   */
  private static synchronized Injector management() {
    if (management == null) {
      try {
        final Properties p = new Properties();
        p.load(Resources.getResourceAsStream("registry-test.properties"));
        management = Guice.createInjector(new AbstractModule() {

          @Override
          protected void configure() {
            //Names.bindProperties(binder(), p);
            //bind(DataSource.class).toProvider(new ManagementProvider());
          }

          @Provides
          @Singleton
          /**
           * Provides a hikari datasource that can issue connections for management activities, such as Liquibase or
           * clearing tables before tests run etc.
           * We provide an implementation specific datasource here so the pool can be properly closed at the end!
           */
          public HikariDataSource provideDs() {
            HikariConfig config = new HikariConfig(filterProperties(p, "registry.db."));
            config.setConnectionTimeout(5000);
            config.setMaximumPoolSize(1);
            return new HikariDataSource(config);
          }
        });
      } catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }
    return management;
  }

  private static Properties filterProperties(Properties properties, final String prefix) {
    Properties filtered = new Properties();
    for(String key : properties.stringPropertyNames()) {
      if (key.startsWith(prefix)) {
        filtered.setProperty(key.substring(prefix.length()), properties.getProperty(key));
      }
    }
    return filtered;
  }

}
