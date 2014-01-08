package org.gbif.registry.ws.util;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Contactable;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.ws.model.LegacyDataset;
import org.gbif.registry.ws.model.LegacyEndpoint;
import org.gbif.registry.ws.model.LegacyInstallation;

import javax.annotation.Nullable;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class containing utility methods used across legacy resources, in answering legacy web service requests (GBRDS/IPT).
 */
public class LegacyResourceUtils {

  private static final Logger LOG = LoggerFactory.getLogger(LegacyResourceUtils.class);

  /**
   * Checks if a field consists of the minimum number of characters. If it contains fewer characters than required,
   * a warning text is appended to the field text, padding it until it exceeds the minimum required size.
   *
   * @param text field text
   * @param size minimum field size (in number of characters)
   *
   * @return validated field meeting the minimum required length, or the original null or empty string
   */
  public static String validateField(@Nullable String text, int size) {
    String validated = Strings.emptyToNull(text);
    if (validated != null) {
      while (validated.length() < size) {
        validated += " [Field must be at least " + String.valueOf(size) + " characters long]";
      }
      return validated;
    }
    return text;
  }

  /**
   * Iterate through list of contacts, return first primary contact encountered, or null if none found.
   *
   * @param contactable contactable
   *
   * @return primary contact or null if none found
   */
  public static Contact getPrimaryContact(Contactable contactable) {
    for (Contact contact : contactable.getContacts()) {
      if (contact.isPrimary()) {
        return contact;
      }
    }
    return null;
  }

  /**
   * Check installation is valid before updating. Check the installation still exists, the hosting organization
   * still exists, and the same hosting organization is specified before updating.
   *
   * @param installation installation with fields injected from HTTP request
   *
   * @return true if the installation is valid for updating, or false otherwise
   */
  public static boolean isValidOnUpdate(LegacyInstallation installation, InstallationService installationService,
    OrganizationService organizationService) {
    Installation existing = installationService.get(installation.getKey());
    if (existing == null) {
      LOG.error("Installation update uses an installation that does not exist, key={}", installation.getKey());
      return false;
    }
    return isValid(installation, organizationService);
  }

  /**
   * Check installation is valid before persisting. Check required fields, and the hosting organization
   * still exists.
   *
   * @param installation installation with fields injected from HTTP request
   *
   * @return true if the installation is valid, or false otherwise
   */
  public static boolean isValid(LegacyInstallation installation, OrganizationService organizationService) {
    if (installation.getType() == null) {
      LOG.error("Installation is missing mandatory field type, key={}", installation.getKey());
      return false;
    }
    if (installation.getOrganizationKey() == null) {
      LOG.error("Hosting org key not included in HTTP parameters for installation, key={}", installation.getKey());
      return false;
    }
    if (organizationService.get(installation.getOrganizationKey()) == null) {
      LOG.error("Installation uses a hosting org that does not exist, key={}", installation.getOrganizationKey());
      return false;
    }
    return true;
  }

  /**
   * Check dataset is valid before persisting. Check required fields, the owning organization
   * still exists, and the installation still exists before updating.
   *
   * @param dataset dataset with fields injected from HTTP request
   *
   * @return true if the dataset is valid, or false otherwise
   */
  public static boolean isValid(LegacyDataset dataset, OrganizationService organizationService,
    InstallationService installationService) {
    // title is never null in IPT requests, but is mandatory in GBRDS Resource requests
    if (dataset.getTitle() == null) {
      LOG.error("Dataset is missing mandatory field title, key={}", dataset.getKey());
      return false;
    }
    if (dataset.getLanguage() == null) {
      LOG.error("Dataset is missing mandatory field language, key={}", dataset.getKey());
      return false;
    }
    if (dataset.getType() == null) {
      LOG.error("Dataset is missing mandatory field type, key={}", dataset.getKey());
      return false;
    }
    if (dataset.getOwningOrganizationKey() == null) {
      LOG.error("Owning org key not included in HTTP parameters for dataset, key={}", dataset.getKey());
      return false;
    }
    if (organizationService.get(dataset.getOwningOrganizationKey()) == null) {
      LOG.error("Dataset uses an owning org that does not exist, key={}", dataset.getOwningOrganizationKey());
      return false;
    }
    if (dataset.getInstallationKey() == null) {
      LOG.error("Installation key not included in HTTP parameters for dataset, or could not be inferred, key={}",
        dataset.getKey());
      return false;
    }
    if (installationService.get(dataset.getInstallationKey()) == null) {
      LOG.error("Dataset uses an IPT that does not exist, key={}", dataset.getInstallationKey());
      return false;
    }
    return true;
  }

  /**
   * Check dataset is valid before updating. Check the dataset still exists, the owning organization
   * still exists, the same owning organization is specified, and the installation still exists before updating.
   *
   * @param dataset dataset with fields injected from HTTP request
   *
   * @return true if the dataset is valid for updating, or false otherwise
   */
  public static boolean isValidOnUpdate(LegacyDataset dataset, DatasetService datasetService,
    OrganizationService organizationService, InstallationService installationService) {
    Dataset existing = datasetService.get(dataset.getKey());
    if (existing == null) {
      LOG.error("Dataset update uses a datast that does not exist, key={}", dataset.getKey());
      return false;
    }
    return isValid(dataset, organizationService, installationService);
  }

  /**
   * Check endpoint is valid before persisting. Check required fields, the access point URL, type, and dataset key, and
   * that the dataset still exists before updating.
   *
   * @param endpoint endpoint with fields injected from HTTP request
   *
   * @return true if the endpoint is valid, or false otherwise
   */
  public static boolean isValid(LegacyEndpoint endpoint, DatasetService datasetService) {
    if (endpoint.getType() == null) {
      LOG.error("type not included in HTTP parameters for LegacyEndpiont (Service)");
      return false;
    }
    if (endpoint.getUrl() == null) {
      LOG.error("accessPointURL not included in HTTP parameters for LegacyEndpiont (Service)");
      return false;
    }
    if (endpoint.getDatasetKey() == null) {
      LOG.error("Dataset key (resourceKey) not included in HTTP parameters for LegacyEndpiont (Service)");
      return false;
    }
    if (datasetService.get(endpoint.getDatasetKey()) == null) {
      LOG.error("LegacyEndpiont (Service) refers to a dataset that does not exist, key={}", endpoint.getDatasetKey());
      return false;
    }
    return true;
  }
}
