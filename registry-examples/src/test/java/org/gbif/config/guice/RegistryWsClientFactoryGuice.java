package org.gbif.config.guice;

import org.gbif.registry.ws.client.guice.RegistryWsClientModule;
import org.gbif.ws.client.guice.AnonymousAuthModule;
import org.gbif.ws.client.guice.SingleUserAuthModule;

import java.io.IOException;
import java.util.Properties;

import com.google.common.base.Throwables;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class RegistryWsClientFactoryGuice {

  // cache everything, for reuse in repeated calls
  private static Injector webserviceClient;
  private static Injector webserviceClientReadOnly;
  private static Properties properties;

  // GBIF account
  private static final String USERNAME = "ws_client_demo";
  private static final String PASSWORD = "Demo123";

  /**
   * Load the Properties needed to configure the webservice client from the registry.properties file.
   */
  public static synchronized Properties properties() {
    if (properties == null) {
      Properties p = new Properties();
      try {
        p.load(RegistryWsClientFactoryGuice.class.getResourceAsStream("/registry.properties"));
      } catch (IOException e) {
        throw Throwables.propagate(e);
      } finally {
        properties = p;
      }
    }
    return properties;
  }

  /**
   * @return An injector that is bound for the webservice client layer.
   */
  public static synchronized Injector webserviceClient() {
    if (webserviceClient == null) {
      // IMPORTANT: GBIF account must exist and must have correct permissions set
      SingleUserAuthModule auth = new SingleUserAuthModule(USERNAME, PASSWORD);
      webserviceClient = Guice.createInjector(new RegistryWsClientModule(properties()), auth);
    }
    return webserviceClient;
  }

  /**
   * @return An injector that is bound for the webservice client layer but read-only.
   */
  public static synchronized Injector webserviceClientReadOnly() {
    if (webserviceClientReadOnly == null) {
      // Anonymous authentication module used, webservice client will be read-only
      webserviceClientReadOnly =
        Guice.createInjector(new RegistryWsClientModule(properties()), new AnonymousAuthModule());
    }
    return webserviceClientReadOnly;
  }
}
