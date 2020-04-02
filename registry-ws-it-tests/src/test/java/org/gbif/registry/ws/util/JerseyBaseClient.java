/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.ws.util;

import java.util.function.Function;

import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

/** Basic Jersey {@link Client} with support for GBIF sessions and WebResource configurer. */
public class JerseyBaseClient {

  private final Client client;
  private final String wsBaseUrl;
  private final String resourcePath;

  public JerseyBaseClient(Client client, String wsBaseUrl, String resourcePath) {
    this.client = client;
    // client.
    this.wsBaseUrl = wsBaseUrl;
    this.resourcePath = resourcePath;
  }

  private WebResource getBaseWebResource() {
    return client.resource(wsBaseUrl).path(resourcePath);
  }

  /**
   * Issue a {@code GET} from the base URL using a provided configuration function ({@code
   * configurer}).
   *
   * @param configurer function allowing to add path, query parameters or headers to the base {@link
   *     WebResource}
   * @return {@link ClientResponse} as result of the call
   */
  public ClientResponse get(Function<WebResource, WebResource> configurer) {
    return configurer
        .apply(client.resource(wsBaseUrl).path(resourcePath))
        .get(ClientResponse.class);
  }

  public ClientResponse post(Function<WebResource, WebResource> configurer, Object entity) {
    return configurer
        .apply(client.resource(wsBaseUrl).path(resourcePath))
        .type(MediaType.APPLICATION_JSON)
        .post(ClientResponse.class, entity);
  }

  /**
   * Issue a {@code POST} from the base URL using a provided configuration function ({@code
   * configurer}).
   *
   * @param configurer function allowing to add path, query parameters or headers to the base {@link
   *     WebResource}
   * @param entity entity to post
   * @return {@link ClientResponse} as result of the call
   */
  public ClientResponse put(Function<WebResource, WebResource> configurer, Object entity) {
    return configurer
        .apply(client.resource(wsBaseUrl).path(resourcePath))
        .type(MediaType.APPLICATION_JSON)
        .put(ClientResponse.class, entity);
  }

  /**
   * Get underlying {@link Client}
   *
   * @return underlying {@link Client}
   */
  public Client getClient() {
    return client;
  }

  /** Destroy the underlying {@link Client} */
  public void destroy() {
    client.destroy();
  }
}
