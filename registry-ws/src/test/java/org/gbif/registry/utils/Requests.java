package org.gbif.registry.utils;

import org.gbif.registry.grizzly.RegistryServer;
import org.gbif.utils.HttpUtil;
import org.gbif.utils.PreemptiveAuthenticationInterceptor;

import java.util.List;

import com.beust.jcommander.internal.Lists;
import org.apache.http.impl.client.DefaultHttpClient;

public class Requests {

  public static final String DATASET_NAME = "Test Dataset Registry2 Sjælland";
  public static final String DATASET_DESCRIPTION = "Description of Test Dataset";
  public static final String DATASET_HOMEPAGE_URL = "http://www.homepage.com";
  public static final String DATASET_LOGO_URL = "http://www.logo.com/1";
  public static final String DATASET_PRIMARY_CONTACT_TYPE = "administrative";
  public static final String DATASET_PRIMARY_CONTACT_NAME = "Jan Legind";
  public static final List<String> DATASET_PRIMARY_CONTACT_EMAIL = Lists.newArrayList("jlegind@gbif.org");
  public static final List<String> DATASET_PRIMARY_CONTACT_PHONE = Lists.newArrayList("90909090");
  public static final List<String> DATASET_PRIMARY_CONTACT_ADDRESS = Lists.newArrayList("Universitetsparken 15, 2100, Denmark");
  // GBRDS Datasets only
  public static final String DATASET_PRIMARY_CONTACT_DESCRIPTION = "Data manager";
  public static final String DATASET_NAME_LANGUAGE = "fr";
  public static final String DATASET_DESCRIPTION_LANGUAGE = "es";

  public static HttpUtil http;

  static {
    http = getHttp();
  }

  /**
   * Initializes and configures the HttpUtil instance, which is what the IPT/Legacy WS consumers use to issue HTTP
   * requests. This method copies the same configuration as the IPT uses, to ensure the exact same structured requests
   * are sent during unit testing.
   */
  public static HttpUtil getHttp() {
    // new threadsafe, multithreaded http client with support for http and https.
    DefaultHttpClient client = HttpUtil.newMultithreadedClient(1000000, 1, 1);
    // the registry requires Preemptive authentication, so make this the very first interceptor in the protocol chain
    client.addRequestInterceptor(new PreemptiveAuthenticationInterceptor(), 0);
    return new HttpUtil(client);
  }

  /**
   * Construct request URI, appending path to local Grizzly server address.
   *
   * @param path path
   *
   * @return request URI
   */
  public static String getRequestUri(String path) {
    StringBuilder uri = new StringBuilder();
    uri.append("http://localhost:");
    uri.append(RegistryServer.getPort());
    uri.append(path);
    return uri.toString();
  }
}
