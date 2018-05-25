package org.gbif.registry.dataprivacy.email;

import org.gbif.registry.surety.email.BaseTemplateDataModel;

import java.net.URL;
import java.util.List;
import javax.annotation.Nullable;

/**
 * TemplateDataModel for the dataprivacy notification email.
 */
public class DataPrivacyNotificationTemplateDataModel extends BaseTemplateDataModel {

  private final List<URL> sampleUrls;

  private DataPrivacyNotificationTemplateDataModel(@Nullable String name, URL url, List<URL> sampleUrls) {
    super(name, url);
    this.sampleUrls = sampleUrls;
  }

  public static DataPrivacyNotificationTemplateDataModel newInstance(URL informationPageUrl, List<URL> sampleUrls) {
    return new DataPrivacyNotificationTemplateDataModel(null, informationPageUrl, sampleUrls);
  }

  public List<URL> getSampleUrls() {
    return sampleUrls;
  }
}
