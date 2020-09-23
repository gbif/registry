package org.gbif.registry.domain.mail;

import java.net.URL;

/**
 * Base template used to generate an email of the type "Hello [name], please click [url]".
 */
public class ConfirmableTemplateDataModel extends BaseTemplateDataModel {

  private final URL url;

  public ConfirmableTemplateDataModel(String name, URL url) {
    super(name);
    this.url = url;
  }

  public URL getUrl() {
    return url;
  }
}
