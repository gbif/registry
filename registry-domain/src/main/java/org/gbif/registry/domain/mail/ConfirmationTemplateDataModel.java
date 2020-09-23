package org.gbif.registry.domain.mail;

import java.net.URL;

/**
 * Base template used to generate an email of the type "Hello [name], please click [url]".
 */
public class ConfirmationTemplateDataModel extends BaseTemplateDataModel {

  private final URL url;

  public ConfirmationTemplateDataModel(String name, URL url) {
    super(name);
    this.url = url;
  }

  public URL getUrl() {
    return url;
  }
}
