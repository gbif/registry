package org.gbif.registry.ws.security;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.ws.util.LegacyResourceConstants;
import org.gbif.ws.NotFoundException;
import org.gbif.ws.WebApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Base64;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.gbif.ws.util.CommonWsUtils.getFirst;

/**
 * Class providing temporary authorization for legacy web service requests (GBRDS/IPT).
 */
@Service
public class LegacyAuthorizationServiceImpl implements LegacyAuthorizationService {

  private static final Logger LOG = LoggerFactory.getLogger(LegacyAuthorizationServiceImpl.class);
  private static final Splitter COLON_SPLITTER = Splitter.on(":").limit(2);

  private final OrganizationService organizationService;
  private final DatasetService datasetService;
  private final InstallationService installationService;

  // TODO: 06/09/2019 fix problem with services (circular dependency). Optional lazy dependency for now
  public LegacyAuthorizationServiceImpl(@Lazy @Autowired(required = false) OrganizationService organizationService,
                                        @Lazy @Autowired(required = false) DatasetService datasetService,
                                        @Lazy @Autowired(required = false) InstallationService installationService) {
    this.organizationService = organizationService;
    this.datasetService = datasetService;
    this.installationService = installationService;
  }

  /**
   * Evaluates the Basic authentication header and verifies provided password matches with the registered one.
   * Checks both for organizations and installation keys.
   *
   * @return the registry entity this request has authenticated with
   */
  @Override
  public LegacyRequestAuthorization authenticate(HttpServletRequest httpRequest) {
    String authentication = httpRequest.getHeader(HttpHeaders.AUTHORIZATION);
    if (Strings.isNullOrEmpty(authentication) || !authentication.startsWith("Basic ")) {
      LOG.info("No basic authorization header found in legacy ws request");
      return null;
    }

    byte[] decryptedBytes = Base64.getDecoder().decode(authentication.substring("Basic ".length()));
    String decrypted = new String(decryptedBytes);
    Iterator<String> iter = COLON_SPLITTER.split(decrypted).iterator();
    try {
      final UUID user = UUID.fromString(iter.next());
      final String password = iter.next();
      final UUID organizationKey = UUID.fromString(getFirst(httpRequest.getParameterMap(), LegacyResourceConstants.ORGANIZATION_KEY_PARAM));

      // try to validate organization key first
      try {
        Organization org = organizationService.get(user);
        if (password.equals(org.getPassword())) {
          return new LegacyRequestAuthorization(user, organizationKey);
        } else {
          throw new WebApplicationException(HttpStatus.UNAUTHORIZED);
        }
      } catch (NotFoundException e) {
        // maybe an installation?
        try {
          Installation installation = installationService.get(user);
          if (password.equals(installation.getPassword())) {
            return new LegacyRequestAuthorization(user, organizationKey);
          }
        } catch (NotFoundException e1) {
          // throw
        }
        throw new WebApplicationException(HttpStatus.UNAUTHORIZED);
      }
    } catch (NoSuchElementException e) {
      LOG.warn("Invalid Basic Authentication syntax: {}", authentication);
      throw new WebApplicationException(HttpStatus.BAD_REQUEST);
    } catch (IllegalArgumentException e) {
      // no valid username UUID
      LOG.warn("Invalid username, UUID required: {}", decrypted);
      throw new WebApplicationException(HttpStatus.UNAUTHORIZED);
    }
  }

  /**
   * Determine if HTTP request is authorized to modify Organization. The difference between this method and
   * isAuthorizedToModifyOrganization(organizationKey) is that the organizationKey must first be parsed from the
   * form parameters.
   *
   * @return true if the HTTP request is authorized to modify Organization
   * @see this#isAuthorizedToModifyOrganization(LegacyRequestAuthorization, UUID)
   */
  public boolean isAuthorizedToModifyOrganization(LegacyRequestAuthorization authorization) {
    // retrieve HTTP param for hosting organization key and convert incoming key into UUID
    UUID organizationKey = authorization.getOrganizationKey();

    return isAuthorizedToModifyOrganization(authorization, organizationKey);
  }

  @Override
  public boolean isAuthorizedToModifyOrganization(LegacyRequestAuthorization authorization, UUID organizationKey) {
    if (organizationKey == null) {
      return false;
    }
    // validate installation key belongs to an existing installation
    Organization org = organizationService.get(organizationKey);
    return org != null && org.getKey().equals(authorization.getUserKey());
  }

  /**
   * Determine if HTTP request is authorized to modify a Dataset belonging to an Organization.
   * This method checks that the same organizationKey is specified in the credentials and HTTP form parameters,
   * that the Organization corresponding to that organizationKey exists, that the Dataset corresponding to the
   * datasetKey exists, that the Dataset is owned by that Organization, and that the correct organization password has
   * been supplied.
   *
   * @param datasetKey Dataset key
   * @return true if the HTTP request is authorized to modify a Dataset belonging to Organization
   */
  @Override
  public boolean isAuthorizedToModifyOrganizationsDataset(LegacyRequestAuthorization authorization, UUID datasetKey) {
    if (datasetKey == null) {
      LOG.error("Dataset key was null");
      return false;
    }
    // retrieve dataset to ensure it exists
    Dataset dataset = datasetService.get(datasetKey);
    if (dataset == null) {
      LOG.error("Dataset with key={} does not exist", datasetKey.toString());
      return false;
    }

    // check the dataset belongs to organization
    if (dataset.getPublishingOrganizationKey().compareTo(authorization.getUserKey()) != 0) {
      LOG.error("The Dataset is not owned by the organization specified in the credentials");
      return false;
    }

    // check if an organisationKey was included in form params, that the organization keys match
    UUID organizationKeyFromFormParams = authorization.getOrganizationKey();
    if (organizationKeyFromFormParams != null) {
      if (organizationKeyFromFormParams.compareTo(authorization.getUserKey()) != 0) {
        LOG.error("Different organization keys were specified in the form parameters and credentials");
        return false;
      }
    }
    LOG.info("Authorization succeeded, can modify dataset with key={} belonging to organization with key={}",
        datasetKey.toString(), authorization.getUserKey().toString());
    return true;
  }

  /**
   * Determine if HTTP request is authorized to modify Installation.
   *
   * @param installationKey Installation key
   * @return true if the HTTP request is authorized to modify Installation
   */
  @Override
  public boolean isAuthorizedToModifyInstallation(LegacyRequestAuthorization authorization, UUID installationKey) {
    // retrieve path param for installation key
    if (installationKey == null) {
      return false;
    }
    // validate installation key belongs to an existing installation
    Installation installation = installationService.get(installationKey);
    return installation != null && installation.getKey() != null && installation.getKey().equals(authorization.getUserKey());
  }
}
