package org.gbif.registry.metrics.guice;

import org.gbif.registry.metrics.OccurenceMetricsWsClient;
import org.gbif.registry.metrics.OccurrenceMetricsClient;
import org.gbif.ws.client.guice.GbifWsClientModule;

import java.util.Properties;

import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

/**
 *
 * This module exists to avoid circular dependencies between Occurrence and Registry projects.
 * It accesses Metrics from API using HTTP requests directly (instead of using CubeWsClient).
 *
 */
public class OccurrenceMetricsModule extends GbifWsClientModule {

  public OccurrenceMetricsModule(Properties properties, Package... clientPackages) {
    super(properties, clientPackages);
  }

  @Provides
  @Singleton
  public WebResource provideWebResource(Client client, @Named("metrics.ws.url") String url) {
    return client.resource(url);
  }

  @Override
  protected void configureClient() {
    bind(OccurrenceMetricsClient.class).to(OccurenceMetricsWsClient.class).in(Scopes.SINGLETON);
    expose(OccurrenceMetricsClient.class);
  }
}
