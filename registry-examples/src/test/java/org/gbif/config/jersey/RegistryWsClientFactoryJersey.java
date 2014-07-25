package org.gbif.config.jersey;

import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.registry.ws.client.DatasetWsClient;
import org.gbif.registry.ws.client.InstallationWsClient;
import org.gbif.utils.HttpUtil;
import org.gbif.ws.json.JacksonJsonContextResolver;

import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.client.apache4.ApacheHttpClient4;
import com.sun.jersey.client.apache4.ApacheHttpClient4Handler;
import org.apache.http.client.HttpClient;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;

public class RegistryWsClientFactoryJersey {

  private static final int DEFAULT_HTTP_TIMEOUT_MSECS = 10000;
  private static final int DEFAULT_MAX_HTTP_CONNECTIONS = 100;
  private static final int DEFAULT_MAX_HTTP_CONNECTIONS_PER_ROUTE = 100;

  // GBIF account, with correct role and permissions set
  private static final String USERNAME = "ws_client_demo";
  private static final String PASSWORD = "Demo123";
  private static final HTTPBasicAuthFilter AUTH_FILTER = new HTTPBasicAuthFilter(USERNAME, PASSWORD);
  private static final String REGISTRY_API_BASE_URL = "http://registry-sandbox.gbif.org";

  private static ApacheHttpClient4 client;
  private static DatasetService datasetService;
  private static DatasetService datasetServiceReadOnly;
  private static InstallationService installationService;


  /**
   * @return Apache HTTP Client
   */
  private static HttpClient buildHttpClient() {
    return HttpUtil.newMultithreadedClient(DEFAULT_HTTP_TIMEOUT_MSECS, DEFAULT_MAX_HTTP_CONNECTIONS,
      DEFAULT_MAX_HTTP_CONNECTIONS_PER_ROUTE);
  }

  /**
   * @return Jersey client that utilizes the Apache HTTP Client to send and receive HTTP request and responses.
   */
  public static synchronized ApacheHttpClient4 buildJerseyClient() {
    if (client == null) {
        ApacheHttpClient4Handler hch = new ApacheHttpClient4Handler(buildHttpClient(), null, false);
        ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getClasses().add(JacksonJsonContextResolver.class);
        // this line is critical! Note that this is the jersey version of this class name!
        clientConfig.getClasses().add(JacksonJsonProvider.class);
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        client = new ApacheHttpClient4(hch, clientConfig);
    }
    return client;
  }

  /**
   * @return read-only DatasetService
   */
  public static synchronized DatasetService datasetServiceReadOnly() {
    if (datasetServiceReadOnly == null) {
      WebResource datasetResource = buildJerseyClient().resource(REGISTRY_API_BASE_URL);
      datasetServiceReadOnly = new DatasetWsClient(datasetResource, null);
    }
    return datasetServiceReadOnly;
  }

  /**
   * @return DatasetService with authentication
   */
  public static synchronized DatasetService datasetService() {
    if (datasetService == null) {
      WebResource datasetResource = buildJerseyClient().resource(REGISTRY_API_BASE_URL);
      datasetService = new DatasetWsClient(datasetResource, AUTH_FILTER);
    }
    return datasetService;
  }

  /**
   * @return InstallationService with authentication
   */
  public static synchronized InstallationService installationService() {
    if (installationService == null) {
      WebResource installationResource = buildJerseyClient().resource(REGISTRY_API_BASE_URL);
      installationService = new InstallationWsClient(installationResource, AUTH_FILTER);
    }
    return installationService;
  }

}
