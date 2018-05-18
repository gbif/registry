package org.gbif.registry.gdpr.email;

import org.gbif.registry.surety.SuretyConstants;

import java.util.Properties;

import org.apache.commons.lang3.BooleanUtils;

/**
 * Configuration for gdpr emails.
 */
public class GdprEmailConfiguration {

  // property names
  private static final String GDPR_MAIL_ENABLED_PROP = SuretyConstants.PROPERTY_PREFIX + "mail.enable";
  private static final String SUBJECT_PROP = SuretyConstants.PROPERTY_PREFIX + "mail.subject";
  private static final String INFORMATION_PAGE_PROP = SuretyConstants.PROPERTY_PREFIX + "mail.informationPage";
  private static final String NODE_URL_TEMPLATE_PROP = SuretyConstants.PROPERTY_PREFIX + "mail.urlTemplate.node";
  private static final String ORGANIZATION_URL_TEMPLATE_PROP =
    SuretyConstants.PROPERTY_PREFIX + "mail.urlTemplate.organization";
  private static final String INSTALLATION_URL_TEMPLATE_PROP =
    SuretyConstants.PROPERTY_PREFIX + "mail.urlTemplate.installation";
  private static final String NETWORK_URL_TEMPLATE_PROP = SuretyConstants.PROPERTY_PREFIX + "mail.urlTemplate.network";
  private static final String DATASET_URL_TEMPLATE_PROP = SuretyConstants.PROPERTY_PREFIX + "mail.urlTemplate.dataset";

  private final boolean gdprMailEnabled;
  private final String subject;
  private final String informationPage;
  private final String nodeUrlTemplate;
  private final String organizationUrlTemplate;
  private final String installationUrlTemplate;
  private final String networkUrlTemplate;
  private final String datasetUrlTemplate;

  public static GdprEmailConfiguration from(Properties filteredProperties) {
    return new GdprEmailConfiguration(filteredProperties);
  }

  private GdprEmailConfiguration(Properties filteredProperties) {
    gdprMailEnabled = BooleanUtils.toBoolean(filteredProperties.getProperty(GDPR_MAIL_ENABLED_PROP));
    subject = filteredProperties.getProperty(SUBJECT_PROP);
    informationPage = filteredProperties.getProperty(INFORMATION_PAGE_PROP);
    nodeUrlTemplate = filteredProperties.getProperty(NODE_URL_TEMPLATE_PROP);
    organizationUrlTemplate = filteredProperties.getProperty(ORGANIZATION_URL_TEMPLATE_PROP);
    installationUrlTemplate = filteredProperties.getProperty(INSTALLATION_URL_TEMPLATE_PROP);
    networkUrlTemplate = filteredProperties.getProperty(NETWORK_URL_TEMPLATE_PROP);
    datasetUrlTemplate = filteredProperties.getProperty(DATASET_URL_TEMPLATE_PROP);
  }

  public boolean isGdprMailEnabled() {
    return gdprMailEnabled;
  }

  public String getSubject() {
    return subject;
  }

  public String getInformationPage() {
    return informationPage;
  }

  public String getNodeUrlTemplate() {
    return nodeUrlTemplate;
  }

  public String getOrganizationUrlTemplate() {
    return organizationUrlTemplate;
  }

  public String getInstallationUrlTemplate() {
    return installationUrlTemplate;
  }

  public String getNetworkUrlTemplate() {
    return networkUrlTemplate;
  }

  public String getDatasetUrlTemplate() {
    return datasetUrlTemplate;
  }
}
