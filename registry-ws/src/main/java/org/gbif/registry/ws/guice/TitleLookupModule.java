package org.gbif.registry.ws.guice;

import org.gbif.occurrence.query.TitleLookup;
import org.gbif.utils.HttpUtil;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.client.apache4.ApacheHttpClient4;
import com.sun.jersey.client.apache4.ApacheHttpClient4Handler;
import org.apache.http.client.HttpClient;

/**
 * Guice module providing a TitleLookup instance for the HumanFilterBuilder to lookup species and dataset titles.
 */
public class TitleLookupModule extends AbstractModule {
  private final boolean provideHttpClient;
  private final String apiRoot;

  /**
   * @param provideHttpClient if true the module creates a new default http client instance
   */
  public TitleLookupModule(boolean provideHttpClient, String apiRoot) {
    this.provideHttpClient = provideHttpClient;
    this.apiRoot = apiRoot;
  }

  @Override
  protected void configure() {
    if (provideHttpClient) {
      bind(HttpClient.class).toInstance(provideHttpClient());
    }
  }

  @Provides
  @Singleton
  @Inject
  private TitleLookup provideLookup(HttpClient hc) {
    ApacheHttpClient4Handler hch = new ApacheHttpClient4Handler(hc, null, false);
    ClientConfig clientConfig = new DefaultClientConfig();
    clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
    WebResource api = new ApacheHttpClient4(hch, clientConfig).resource(apiRoot);
    return new TitleLookup(api);
  }

  private HttpClient provideHttpClient() {
    return HttpUtil.newMultithreadedClient(5000, 10, 10);
  }
}
