package org.gbif.identity.email;

import java.net.URL;

/**
 * Very basic email model that holds
 */
public class BaseEmailModel {

  private final String name;
  private final URL url;

  public BaseEmailModel(String name, URL url) {
    this.name = name;
    this.url = url;
  }

  public String getName() {
    return name;
  }

  public URL getUrl() {
    return url;
  }
}
