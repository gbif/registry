package org.gbif.registry.dataprivacy.email;

import org.gbif.registry.surety.SuretyConstants;

import java.util.Objects;
import java.util.Properties;

import org.apache.commons.lang3.BooleanUtils;

/**
 * Configuration for dataprivacy emails.
 */
public class DataPrivacyEmailConfiguration {

  // property names
  private static final String DATA_PRIVACY_PREFIX = SuretyConstants.PROPERTY_PREFIX + "mail.enabled";
  private static final String SUBJECT_PROP = SuretyConstants.PROPERTY_PREFIX + "mail.subject";
  private static final String INFORMATION_PAGE_PROP = SuretyConstants.PROPERTY_PREFIX + "mail.informationPage";
  private static final String NODE_URL_TEMPLATE_PROP = SuretyConstants.PROPERTY_PREFIX + "mail.urlTemplate.node";
  private static final String ORGANIZATION_URL_TEMPLATE_PROP =
    SuretyConstants.PROPERTY_PREFIX + "mail.urlTemplate.organization";
  private static final String INSTALLATION_URL_TEMPLATE_PROP =
    SuretyConstants.PROPERTY_PREFIX + "mail.urlTemplate.installation";
  private static final String NETWORK_URL_TEMPLATE_PROP = SuretyConstants.PROPERTY_PREFIX + "mail.urlTemplate.network";
  private static final String DATASET_URL_TEMPLATE_PROP = SuretyConstants.PROPERTY_PREFIX + "mail.urlTemplate.dataset";

  private final boolean dataPrivacyMailEnabled;
  private final String subject;
  private final String informationPage;
  private final String nodeUrlTemplate;
  private final String organizationUrlTemplate;
  private final String installationUrlTemplate;
  private final String networkUrlTemplate;
  private final String datasetUrlTemplate;

  public static DataPrivacyEmailConfiguration from(Properties filteredProperties) {
    return new DataPrivacyEmailConfiguration(filteredProperties);
  }

  private DataPrivacyEmailConfiguration(Properties filteredProperties) {
    dataPrivacyMailEnabled = BooleanUtils.toBoolean(filteredProperties.getProperty(DATA_PRIVACY_PREFIX));
    subject = Objects.requireNonNull(filteredProperties.getProperty(SUBJECT_PROP));
    informationPage = Objects.requireNonNull(filteredProperties.getProperty(INFORMATION_PAGE_PROP));
    nodeUrlTemplate = Objects.requireNonNull(filteredProperties.getProperty(NODE_URL_TEMPLATE_PROP));
    organizationUrlTemplate = Objects.requireNonNull(filteredProperties.getProperty(ORGANIZATION_URL_TEMPLATE_PROP));
    installationUrlTemplate = Objects.requireNonNull(filteredProperties.getProperty(INSTALLATION_URL_TEMPLATE_PROP));
    networkUrlTemplate = Objects.requireNonNull(filteredProperties.getProperty(NETWORK_URL_TEMPLATE_PROP));
    datasetUrlTemplate = Objects.requireNonNull(filteredProperties.getProperty(DATASET_URL_TEMPLATE_PROP));
  }

  public boolean isDataPrivacyMailEnabled() {
    return dataPrivacyMailEnabled;
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
