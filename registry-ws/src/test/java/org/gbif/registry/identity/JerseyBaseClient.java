package org.gbif.registry.identity;

import org.gbif.registry.ws.filter.CookieAuthFilter;

import java.util.function.Function;
import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

/**
 * Basic Jersey {@link Client} with support for GBIF sessions and WebResource configurer.
 */
public class JerseyBaseClient {

  private final Client client;
  private final String wsBaseUrl;
  private final String resourcePath;

  public JerseyBaseClient(Client client, String wsBaseUrl, String resourcePath) {
    this.client = client;
    //client.
    this.wsBaseUrl = wsBaseUrl;
    this.resourcePath = resourcePath;
  }

  private WebResource getBaseWebResource() {
    return client.resource(wsBaseUrl)
            .path(resourcePath);
  }

  private WebResource.Builder getBaseWebResourceForSession(WebResource webResource, String sessionToken) {
    WebResource.Builder builder = webResource
            .type(MediaType.APPLICATION_JSON)
            .header(CookieAuthFilter.GBIF_USER_HEADER, sessionToken);

    return builder;
  }

  /**
   * Issue a {@code GET} from the base URL using a provided configuration function ({@code configurer}).
   *
   * @param configurer function allowing to add path, query parameters or headers to the base {@link WebResource}
   *
   * @return {@link ClientResponse} as result of the call
   */
  public ClientResponse get(Function<WebResource, WebResource> configurer) {
    return configurer.apply(client.resource(wsBaseUrl).path(resourcePath))
            .get(ClientResponse.class);
  }

  /**
   * Get underlying {@link Client}
   *
   * @return underlying {@link Client}
   */
  public Client getClient() {
    return client;
  }

  public ClientResponse putWithSessionToken(String sessionToken, Object entity) {
    return getBaseWebResourceForSession(getBaseWebResource(), sessionToken)
            .put(ClientResponse.class, entity);
  }

  /**
   * Issue a {@code GET} from the base URL using a provided configuration function ({@code configurer}) and
   * a GBIF session token.
   *
   * @param sessionToken
   * @param configurer
   *
   * @return {@link ClientResponse} as result of the call
   */
  public ClientResponse getWithSessionToken(String sessionToken, Function<WebResource, WebResource> configurer) {
    return getBaseWebResourceForSession(configurer.apply(getBaseWebResource()), sessionToken)
            .get(ClientResponse.class);
  }

  /**
   * Issue a {@code GET} from the base URL using a GBIF session token.
   *
   * @param sessionToken
   *
   * @return {@link ClientResponse} as result of the call
   */
  public ClientResponse getWithSessionToken(String sessionToken) {
    return getWithSessionToken(sessionToken, Function.identity());
  }

  /**
   * Destroy the underlying {@link Client}
   */
  public void destroy() {
    client.destroy();
  }

}
