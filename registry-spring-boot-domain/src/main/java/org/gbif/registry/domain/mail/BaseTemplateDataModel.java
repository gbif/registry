package org.gbif.registry.domain.mail;

import java.net.URL;

/**
 * Base template used to generate an email of the type "Hello [name], please click [url].
 */
public class BaseTemplateDataModel {

  private final String name;
  private final URL url;

  public BaseTemplateDataModel(String name, URL url) {
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
