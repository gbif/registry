package org.gbif.registry.events;

import org.gbif.utils.HttpUtil;

import java.net.URI;
import java.util.Properties;

import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A guice module that sets up implementation of the varnish purging via the event bus.
 * If no or a bad apiRoot URL property is configured the listener will not be wired up.
 */
public class VarnishPurgeModule extends PrivateModule {
  private static final Logger LOG = LoggerFactory.getLogger(VarnishPurgeModule.class);
  protected static final int DEFAULT_HTTP_TIMEOUT_MSECS = 5000;
  protected static final int DEFAULT_MAX_HTTP_CONNECTIONS = 25;
  protected static final int DEFAULT_MAX_HTTP_CONNECTIONS_PER_ROUTE = 25;
  private final URI apiRoot;

  public VarnishPurgeModule(Properties properties) {
    URI uri = null;
    try {
      uri = URI.create(properties.getProperty("api.url"));
    } catch (RuntimeException e) {
      LOG.warn("No varnish purging configured. Please set api.url if you want it");
    }
    apiRoot = uri;
  }

  @Override
  protected void configure() {
    if (apiRoot != null) {
      bind(URI.class).toInstance(apiRoot);
      bind(VarnishPurgeListener.class).asEagerSingleton();
      LOG.info("Varnish purging enabled with api root {}", apiRoot);
    }
  }

  @Provides
  @Singleton
  public HttpClient provideHttpClient() {
    return HttpUtil.newMultithreadedClient(DEFAULT_HTTP_TIMEOUT_MSECS, DEFAULT_MAX_HTTP_CONNECTIONS,
            DEFAULT_MAX_HTTP_CONNECTIONS_PER_ROUTE);
  }
}
