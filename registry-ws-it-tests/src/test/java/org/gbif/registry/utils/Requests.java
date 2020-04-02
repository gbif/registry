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
package org.gbif.registry.utils;

import org.gbif.utils.HttpUtil;
import org.gbif.utils.PreemptiveAuthenticationInterceptor;

import java.util.List;

import org.apache.http.impl.client.DefaultHttpClient;

import com.google.common.collect.Lists;

public class Requests {

  public static final String DATASET_NAME = "Test Dataset Registry2 Sjælland";
  public static final String DOI = "http://dx.doi.org/10.1234/timbo";
  public static final String DATASET_DESCRIPTION = "Description of Test Dataset";
  public static final String DATASET_HOMEPAGE_URL = "http://www.homepage.com";
  public static final String DATASET_LOGO_URL = "http://www.logo.com/1";
  public static final String DATASET_PRIMARY_CONTACT_TYPE = "administrative";
  public static final String DATASET_PRIMARY_CONTACT_NAME = "Jan Legind";
  public static final List<String> DATASET_PRIMARY_CONTACT_EMAIL =
      Lists.newArrayList("elyk-kaarb@euskadi.eus");
  public static final List<String> DATASET_PRIMARY_CONTACT_PHONE = Lists.newArrayList("90909090");
  public static final List<String> DATASET_PRIMARY_CONTACT_ADDRESS =
      Lists.newArrayList("Universitetsparken 15, 2100, Denmark");
  // GBRDS Datasets only
  public static final String DATASET_PRIMARY_CONTACT_DESCRIPTION = "Data manager";
  public static final String DATASET_NAME_LANGUAGE = "fr";
  public static final String DATASET_DESCRIPTION_LANGUAGE = "es";

  public static HttpUtil http;

  static {
    http = getHttp();
  }

  /**
   * Initializes and configures the HttpUtil instance, which is what the IPT/Legacy WS consumers use
   * to issue HTTP requests. This method copies the same configuration as the IPT uses, to ensure
   * the exact same structured requests are sent during unit testing.
   */
  public static HttpUtil getHttp() {
    // new threadsafe, multithreaded http client with support for http and https.
    DefaultHttpClient client = HttpUtil.newMultithreadedClient(1000000, 1, 1);
    // the registry requires Preemptive authentication, so make this the very first interceptor in
    // the protocol chain
    client.addRequestInterceptor(new PreemptiveAuthenticationInterceptor(), 0);
    return new HttpUtil(client);
  }

  /**
   * Construct request URI, appending path to local Grizzly server address.
   *
   * @param path path
   * @return request URI
   */
  public static String getRequestUri(String path, Integer port) {
    StringBuilder uri = new StringBuilder();
    uri.append("http://localhost:");
    uri.append(port);
    uri.append(path);
    return uri.toString();
  }
}
