package org.gbif.registry.gdpr.email;

import org.gbif.registry.surety.email.BaseTemplateDataModel;

import java.net.URL;
import java.util.List;
import javax.annotation.Nullable;

/**
 * TemplateDataModel for the gdpr notification email.
 */
public class GdprNotificationTemplateDataModel extends BaseTemplateDataModel {

  private final List<URL> sampleUrls;

  private GdprNotificationTemplateDataModel(@Nullable String name, URL url, List<URL> sampleUrls) {
    super(name, url);
    this.sampleUrls = sampleUrls;
  }

  public static GdprNotificationTemplateDataModel newInstance(URL informationPageUrl, List<URL> sampleUrls) {
    return new GdprNotificationTemplateDataModel(null, informationPageUrl, sampleUrls);
  }

  public List<URL> getSampleUrls() {
    return sampleUrls;
  }
}
