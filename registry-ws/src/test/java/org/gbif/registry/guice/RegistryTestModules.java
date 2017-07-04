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

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.common.GbifUserPrincipal;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.identity.guice.IdentityServiceTestModule;
import org.gbif.identity.service.IdentityServiceModule;
import org.gbif.registry.doi.DoiModule;
import org.gbif.registry.events.EventModule;
import org.gbif.registry.grizzly.RegistryServer;
import org.gbif.registry.persistence.guice.RegistryMyBatisModule;
import org.gbif.registry.search.guice.RegistrySearchModule;
import org.gbif.registry.surety.EmailManagerTestModule;
import org.gbif.registry.surety.EmptyEmailManager;
import org.gbif.registry.surety.email.EmailManager;
import org.gbif.registry.ws.client.guice.RegistryWsClientModule;
import org.gbif.registry.ws.fixtures.TestConstants;
import org.gbif.registry.ws.guice.SecurityModule;
import org.gbif.registry.ws.guice.TestValidateInterceptor;
import org.gbif.registry.ws.resources.DatasetResource;
import org.gbif.registry.ws.resources.DoiRegistrationResource;
import org.gbif.registry.ws.resources.InstallationResource;
import org.gbif.registry.ws.resources.NetworkResource;
import org.gbif.registry.ws.resources.NodeResource;
import org.gbif.registry.ws.resources.OrganizationResource;
import org.gbif.registry.ws.resources.legacy.IptResource;
import org.gbif.registry.ws.surety.SuretyModule;
import org.gbif.ws.client.guice.GbifApplicationAuthModule;
import org.gbif.ws.client.guice.SingleUserAuthModule;

import java.io.IOException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import javax.ws.rs.core.SecurityContext;

import com.google.common.base.Throwables;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import org.apache.bval.guice.ValidationModule;
import org.apache.ibatis.io.Resources;

/**
 * Utility to provide the different Guice configurations for:
 * <ol>
 * <li>The WS service layer</li>
 * <li>The WS service client layer</li>
 * </ol>
 * Everything is cached, and reused on subsequent calls.
 * This is used for Integration testing.
 */
public class RegistryTestModules {

  // cache everything, for reuse in repeated calls (e.g. IDE test everything)
  private static Injector mybatis;
  private static Injector webservice;
  private static Injector webserviceClient;
  private static Injector webserviceAppKeyClient;
  private static Injector webserviceBasicAuthClient;

  private static Injector identityMyBatis;

  public static final String WS_URL = "http://localhost:" + RegistryServer.getPort();

  /**
   * @return An injector that is bound for the mybatis layer and exposes mappers only.
   */
  public static synchronized Injector mybatis() {
    if (mybatis == null) {
      try {
        final Properties p = new Properties();
        p.load(Resources.getResourceAsStream(TestConstants.APPLICATION_PROPERTIES));
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
        p.load(Resources.getResourceAsStream(TestConstants.APPLICATION_PROPERTIES));
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
                  new IdentityMockModule(),
                  new RegistryMyBatisModule(p),
                  new DirectoryMockModule(),
                  new RegistrySearchModule(p),
                  new EmailManagerTestModule(),
                  new SuretyModule(p),
                  new EventModule(p),
                  new ValidationModule(),
                  new SecurityModule(p),
                  new DoiModule(p),
                  new RabbitMockModule(),
                  new TitleLookupMockModule());
      } catch (IOException e) {
        throw Throwables.propagate(e);
      }
    }
    return webservice;
  }

  /**
   * @return An injector that is bound for the Identity mybatis layer.
   */
  public static synchronized Injector identityMybatis() {
    if (identityMyBatis == null) {
      try {
        final Properties p = new Properties();
        p.load(Resources.getResourceAsStream(TestConstants.APPLICATION_PROPERTIES));
        identityMyBatis =
                Guice.createInjector(
                        newAbstractModule(EmailManager.class, EmptyEmailManager.class),
                        new RegistryMyBatisModule(p), //required for the ChallengeCodeMapper
                        new IdentityServiceTestModule(p));
      } catch (IOException e) {
        throw Throwables.propagate(e);
      }
    }
    return identityMyBatis;
  }

  private static <T> AbstractModule newAbstractModule(Class<T> interfaceClass, Class<? extends T> implementationClass) {
    return new AbstractModule(){
      @Override
      protected void configure () {
        bind(interfaceClass).to(implementationClass).in(Scopes.SINGLETON);
      }
    };
  }

  /**
   *
   * @return
   */
  private static SecurityContext mockAdmin() {
    GbifUser user = new GbifUser();
    user.setUserName("admin");
    user.setFirstName("Veronica");
    user.setLastName("Meier");
    user.setEmail("veronica@mailinator.com");
    user.setKey(1);
    Set<UserRole> roles = new HashSet<UserRole>();
    roles.add(UserRole.ADMIN);
    user.setRoles(roles);
    return new SecurityContext() {

      @Override
      public Principal getUserPrincipal() {
        return new GbifUserPrincipal(user);
      }

      @Override
      public boolean isUserInRole(String s) {
        return user.getRoles().stream()
                .filter(r -> r.toString().equalsIgnoreCase(s))
                .findFirst()
                .isPresent();
      }

      @Override
      public boolean isSecure() {
        return false;
      }

      @Override
      public String getAuthenticationScheme() {
        return null;
      }
    };
  }

  /**
   * TODO make it dryer
   * @return An injector that is bound for the webservice client layer using appKey only (no user).
   */
  public static synchronized Injector webserviceAppKeyClient() {
    if (webserviceAppKeyClient == null) {
      Properties props = new Properties();
      props.setProperty("registry.ws.url", "http://localhost:" + RegistryServer.getPort());
      props.setProperty("application.key", TestConstants.IT_APP_KEY);
      props.setProperty("application.secret", TestConstants.IT_APP_SECRET);
      GbifApplicationAuthModule auth = new GbifApplicationAuthModule(props);
      webserviceAppKeyClient = Guice.createInjector(new RegistryWsClientModule(props), auth);
    }
    return webserviceAppKeyClient;
  }

  /**
   * @return An injector that is bound for the webservice client layer.
   */
  public static synchronized Injector webserviceClient() {
    if (webserviceClient == null) {
      Properties props = new Properties();
      props.setProperty("registry.ws.url", "http://localhost:" + RegistryServer.getPort());
      props.setProperty("application.key", TestConstants.IT_APP_KEY);
      props.setProperty("application.secret", TestConstants.IT_APP_SECRET);
      // Create authentication module, and set principal name, equal to a GBIF User unique account name
      GbifApplicationAuthModule auth = new GbifApplicationAuthModule(props);
      auth.setPrincipal("Nicolas Cage");
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
      webserviceBasicAuthClient = Guice.createInjector(new RegistryWsClientModule(props), auth);
    }
    return webserviceBasicAuthClient;
  }

  /**
   * Override the {@link IdentityServiceModule} to expose the myBatis mapper for testing purpose ONLY.
   */
//  private static class IdentityServiceModuleMapper extends IdentityServiceModule {
//
//    /**
//     * Uses the given properties to configure the service.
//     *
//     * @param properties to use
//     */
//    public IdentityServiceModuleMapper(Properties properties) {
//      super(properties);
//    }
//
//    @Override
//    protected void configure() {
//      super.configure();
//      expose(UserMapper.class);
//    }
//  }


}
