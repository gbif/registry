package org.gbif.registry.metasync.util;

import java.util.concurrent.TimeUnit;

import org.apache.http.client.HttpClient;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DecompressingHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

/**
 * A factory that builds new Apache Http Clients to be used with the Metadata synchroniser.
 *
 * This allows for easy swapping of implementations to make testing easier.
 */
public class HttpClientFactory {

  private static final int HTTP_PORT = 80;
  private static final int HTTPS_PORT = 443;

  private static final int MAX_TOTAL_CONNECTIONS = 200;
  private static final int MAX_CONNECTIONS_PER_HOST = 20;

  private final ClientConnectionManager connectionManager;

  private final int timeout;

  /**
   * Builds a new Factory with a default timeout covering various things (connection timeout, read timeout, ...).
   */
  public HttpClientFactory(long timeout, TimeUnit timeUnit) {
    long millis = TimeUnit.MILLISECONDS.convert(timeout, timeUnit);

    System.setProperty("sun.net.client.defaultConnectTimeout", String.valueOf(millis));
    System.setProperty("sun.net.client.defaultReadTimeout", String.valueOf(millis));

    connectionManager = setupConnectionManager();
    this.timeout = (int) millis;
  }

  public HttpClient provideHttpClient() {
    HttpParams params = new BasicHttpParams();
    HttpConnectionParams.setConnectionTimeout(params, timeout);
    HttpConnectionParams.setSoTimeout(params, timeout);
    params.setLongParameter(ClientPNames.CONN_MANAGER_TIMEOUT, timeout);
    return new DecompressingHttpClient(new DefaultHttpClient(connectionManager, params));
  }

  private ClientConnectionManager setupConnectionManager() {
    SchemeRegistry schemeRegistry = new SchemeRegistry();
    schemeRegistry.register(new Scheme("http", HTTP_PORT, PlainSocketFactory.getSocketFactory()));
    schemeRegistry.register(new Scheme("https", HTTPS_PORT, PlainSocketFactory.getSocketFactory()));

    PoolingClientConnectionManager newConnectionManager = new PoolingClientConnectionManager(schemeRegistry);
    newConnectionManager.setMaxTotal(MAX_TOTAL_CONNECTIONS);
    newConnectionManager.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_HOST);
    return newConnectionManager;
  }

}
