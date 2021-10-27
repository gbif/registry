/*
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
package org.gbif.registry.metasync.util;

import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

/**
 * A factory that builds new Apache HTTP Clients to be used with the Metadata synchroniser.
 *
 * <p>This allows for easy swapping of implementations to make testing easier.
 */
public class HttpClientFactory {

  private static final int MAX_TOTAL_CONNECTIONS = 200;
  private static final int MAX_CONNECTIONS_PER_HOST = 20;

  private final HttpClientConnectionManager connectionManager;

  private final int timeout;

  /**
   * Builds a new Factory with a default timeout covering various things (connection timeout, read
   * timeout, ...).
   */
  public HttpClientFactory(long timeout, TimeUnit timeUnit) {
    long millis = TimeUnit.MILLISECONDS.convert(timeout, timeUnit);

    System.setProperty("sun.net.client.defaultConnectTimeout", String.valueOf(millis));
    System.setProperty("sun.net.client.defaultReadTimeout", String.valueOf(millis));

    connectionManager = setupConnectionManager();
    this.timeout = (int) millis;
  }

  public HttpClient provideHttpClient() {
    RequestConfig defaultRequestConfig =
        RequestConfig.custom().setConnectTimeout(timeout).setSocketTimeout(timeout).build();

    CloseableHttpClient httpClient =
        HttpClients.custom()
            .setUserAgent("GBIF-Registry")
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(defaultRequestConfig)
            .build();

    return httpClient;
  }

  private HttpClientConnectionManager setupConnectionManager() {
    SSLContext sslcontext = SSLContexts.createSystemDefault();

    Registry<ConnectionSocketFactory> socketFactoryRegistry =
        RegistryBuilder.<ConnectionSocketFactory>create()
            .register("http", PlainConnectionSocketFactory.INSTANCE)
            .register("https", new SSLConnectionSocketFactory(sslcontext))
            .build();

    PoolingHttpClientConnectionManager connectionManager =
        new PoolingHttpClientConnectionManager(socketFactoryRegistry);
    connectionManager.setMaxTotal(MAX_TOTAL_CONNECTIONS);
    connectionManager.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_HOST);

    return connectionManager;
  }
}
