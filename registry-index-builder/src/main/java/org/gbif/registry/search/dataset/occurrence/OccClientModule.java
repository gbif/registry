package org.gbif.registry.search.dataset.occurrence;

import org.gbif.ws.client.guice.GbifWsClientModule;

import java.util.Properties;

import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

/**
 * A guice module that exposes the simple OccSearchClient.
 */
public class OccClientModule extends GbifWsClientModule {

  /**
   * Default module with the complete API exposed.
   */
  public OccClientModule(Properties properties) {
    super(properties, OccSearchClient.class.getPackage());
  }

  @Provides
  @Singleton
  public WebResource provideWebResource(Client client, @Named("api.url") String url) {
    return client.resource(url);
  }

  @Override
  protected void configureClient() {
    bind(OccSearchClient.class).in(Scopes.SINGLETON);
    expose(OccSearchClient.class);
  }
}
