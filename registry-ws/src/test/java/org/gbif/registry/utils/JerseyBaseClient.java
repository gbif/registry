package org.gbif.registry.utils;

import org.gbif.registry.ws.filter.CookieAuthFilter;

import java.util.function.Function;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

/**
 *
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

  public ClientResponse get(Function<WebResource,WebResource> configurer) {
    return configurer.apply(client.resource(wsBaseUrl).path(resourcePath))
            .get(ClientResponse.class);
  }

//  public ClientResponse get(String path, String identifier) {
//    return client.resource(wsBaseUrl)
//            .path(resourcePath).path(identifier.toString())
//            .get(ClientResponse.class);
//  }

  public Client getClient() {
    return client;
  }

  public ClientResponse list() {
    return client.resource(wsBaseUrl)
            .path(resourcePath)
            .get(ClientResponse.class);
  }

  public ClientResponse putWithSessionToken (String sessionToken, Object entity) {
    return client.resource(wsBaseUrl)
            .path(resourcePath)
            .header(CookieAuthFilter.GBIF_USER_HEADER, sessionToken)
            .type(MediaType.APPLICATION_JSON)
            .put(ClientResponse.class, entity);
  }

  public ClientResponse getWithSessionToken (String sessionToken, Function<WebResource,WebResource> configurer) {
    return configurer.apply(client.resource(wsBaseUrl).path(resourcePath))

            .header(CookieAuthFilter.GBIF_USER_HEADER, sessionToken)
            .type(MediaType.APPLICATION_JSON)
            .get(ClientResponse.class);
  }

  public ClientResponse getWithSessionToken (String sessionToken) {
    return client.resource(wsBaseUrl)
            .path(resourcePath)
            .header(CookieAuthFilter.GBIF_USER_HEADER, sessionToken)
            .type(MediaType.APPLICATION_JSON)
            .get(ClientResponse.class);
  }

  public ClientResponse list(MultivaluedMap<String, String> queryParams) {
    return client.resource(wsBaseUrl)
            .path(resourcePath)
            .queryParams(queryParams)
            .get(ClientResponse.class);
  }

  public ClientResponse post(Object entity) {
    return client.resource(wsBaseUrl).path(resourcePath)
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON)
            .post(ClientResponse.class, entity);
  }

  public ClientResponse put(Integer identifier, Object entity) {
    return client.resource(wsBaseUrl).path(resourcePath).path(identifier.toString())
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON)
            .put(ClientResponse.class, entity);
  }

  public ClientResponse delete(String identifier) {
    return client.resource(wsBaseUrl)
            .path(resourcePath).path(identifier)
            .delete(ClientResponse.class);
  }

  public void destroy(){
    client.destroy();
  }

}
