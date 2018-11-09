package org.gbif.registry.events;

import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.service.collections.PersonService;
import org.gbif.registry.ws.resources.collections.CollectionResource;
import org.gbif.registry.ws.resources.collections.InstitutionResource;
import org.gbif.registry.ws.resources.collections.PersonResource;
import org.gbif.utils.HttpUtil;

import java.net.URI;
import java.util.Properties;

import com.google.inject.PrivateModule;
import com.google.inject.Scopes;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Guice module that sets up implementation of the varnish purging via the event bus.
 * If no or a bad api.cache.purge.url property is configured the listener will not be wired up.
 */
public class VarnishPurgeModule extends PrivateModule {

  public static final String API_CACHE_PURGE_URL_PROPERTY = "api.cache.purge.url";

  private static final Logger LOG = LoggerFactory.getLogger(VarnishPurgeModule.class);
  private static final int DEFAULT_HTTP_TIMEOUT_MSECS = 2000;
  private final int httpThreads;
  private final URI apiRoot;

  public VarnishPurgeModule(Properties properties) {
    String apiCachePurgeUrl = properties.getProperty(API_CACHE_PURGE_URL_PROPERTY);

    try {
      if(StringUtils.isNotBlank(apiCachePurgeUrl)) {
        apiRoot = URI.create(apiCachePurgeUrl);
      }
      else{
        apiRoot = null;
      }
      httpThreads = Integer.valueOf(properties.getProperty("purging.threads", "100").trim());
    } catch (RuntimeException e) {
      LOG.error("Failed to initialize varnish purger because of invalid properties", e.getMessage());
      throw e;
    }
  }

  @Override
  protected void configure() {
    if(apiRoot == null){
      LOG.warn("No varnish purging configured: {} not found or empty", API_CACHE_PURGE_URL_PROPERTY);
      return;
    }
    if (httpThreads > 0) {
      bind(URI.class).toInstance(apiRoot);
      bind(CloseableHttpClient.class).toInstance(HttpUtil.newMultithreadedClient(
        DEFAULT_HTTP_TIMEOUT_MSECS, httpThreads, httpThreads));
      bind(VarnishPurgeListener.class).asEagerSingleton();
      bind(InstitutionService.class).to(InstitutionResource.class).in(Scopes.SINGLETON);
      bind(CollectionService.class).to(CollectionResource.class).in(Scopes.SINGLETON);
      bind(PersonService.class).to(PersonResource.class).in(Scopes.SINGLETON);
      LOG.info("Varnish purging enabled with {} threads and API {}", httpThreads, apiRoot);
    } else {
      LOG.warn("No varnish purging configured. Please set purging.threads greater than zero if you want it");
    }
  }

}
