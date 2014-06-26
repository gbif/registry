package org.gbif.registry.ws.security;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.ws.util.LegacyResourceConstants;

import java.security.Principal;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.UUID;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.core.util.Base64;
import com.sun.jersey.spi.container.ContainerRequest;
import org.apache.http.auth.BasicUserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class providing temporary authorization for legacy web service requests (GBRDS/IPT).
 */
public class LegacyRequestAuthorization implements SecurityContext {

  private static final Logger LOG = LoggerFactory.getLogger(LegacyRequestAuthorization.class);
  private static final Splitter COLON_SPLITTER = Splitter.on(":").limit(2);

  private final OrganizationService organizationService;
  private final InstallationService installationService;
  private final DatasetService datasetService;

  private final UUID userKey;
  private final HttpContext httpContext;

  @Inject
  public LegacyRequestAuthorization(HttpContext httpContext, OrganizationService organizationService, DatasetService datasetService, InstallationService installationService) {
    this.httpContext = httpContext;
    this.organizationService = organizationService;
    this.datasetService = datasetService;
    this.installationService = installationService;
    userKey = authenticateUser(httpContext);
  }

  /**
   * Evaluates the Basic authentication header and verifies provided password matches with the registered one.
   * Checks both for organizations and installation keys.
   * @return the registry entity this request has authenticated with
   */
  private UUID authenticateUser(HttpContext httpContext) {
    String authentication = httpContext.getRequest().getHeaderValue(ContainerRequest.AUTHORIZATION);
    if (Strings.isNullOrEmpty(authentication) || !authentication.startsWith("Basic ")) {
      LOG.info("No basic authorization header found in legacy ws request");
      return null;
    }

    String decrypted = Base64.base64Decode(authentication.substring("Basic ".length()));
    Iterator<String> iter = COLON_SPLITTER.split(decrypted).iterator();
    try {
      UUID user = UUID.fromString(iter.next());
      String password = iter.next();

      // try to validate organization key first
      try {
        Organization org = organizationService.get(user);
        if (password.equals(org.getPassword())) {
          return user;
        } else {
          throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }

      } catch (NotFoundException e) {
        // maybe an installation?
        try {
          Installation installation = installationService.get(user);
          if (password.equals(installation.getPassword())) {
            return user;
          }
        } catch (NotFoundException e1) {
          // throw
        }
        throw new WebApplicationException(Response.Status.UNAUTHORIZED);
      }


    } catch (NoSuchElementException e) {
      LOG.warn("Invalid Basic Authentication syntax: {}", authentication);
      throw new WebApplicationException(Response.Status.BAD_REQUEST);

    } catch (IllegalArgumentException e) {
      // no valid username UUID
      LOG.warn("Invalid username, UUID required: {}", decrypted);
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
  }

  /**
   * Determine if HTTP request is authorized to modify Organization. The difference between this method and
   * isAuthorizedToModifyOrganization(organizationKey) is that the organizationKey must first be parsed from the
   * form parameters.
   *
   * @return true if the HTTP request is authorized to modify Organization
   *
   * @see LegacyRequestAuthorization#isAuthorizedToModifyOrganization(UUID)
   */
  public boolean isAuthorizedToModifyOrganization() {
    // retrieve HTTP param for hosting organization key and convert incoming key into UUID
    UUID organizationKey = getOrganizationKeyFromParams();

    return isAuthorizedToModifyOrganization(organizationKey);
  }

  public boolean isAuthorizedToModifyOrganization(UUID organizationKey) {
    if (organizationKey == null) {
      return false;
    }
    // validate installation key belongs to an existing installation
    Organization org = organizationService.get(organizationKey);
    return org != null && org.getKey().equals(userKey);
  }


  /**
   * Retrieves the organizationKey from the requests' form parameters.
   *
   * @return key or null if not found
   */
  public UUID getOrganizationKeyFromParams() {
    MultivaluedMap<String, String> params = httpContext.getRequest().getFormParameters();
    if (params == null) {
      return null;
    }

    // retrieve HTTP param for hosting organization key
    String organizationKeyFormParam = params.getFirst(LegacyResourceConstants.ORGANIZATION_KEY_PARAM);
    if (Strings.isNullOrEmpty(organizationKeyFormParam)) {
      return null;
    }
    // convert incoming key into UUID
    try {
      return UUID.fromString(organizationKeyFormParam);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * Determine if HTTP request is authorized to modify a Dataset belonging to an Organization.
   * This method checks that the same organizationKey is specified in the credentials and HTTP form parameters,
   * that the Organization corresponding to that organizationKey exists, that the Dataset corresponding to the
   * datasetKey exists, that the Dataset is owned by that Organization, and that the correct organization password has
   * been supplied.
   *
   * @param datasetKey Dataset key
   *
   * @return true if the HTTP request is authorized to modify a Dataset belonging to Organization
   */
  public boolean isAuthorizedToModifyOrganizationsDataset(UUID datasetKey) {
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
    if (dataset.getPublishingOrganizationKey().compareTo(userKey) != 0) {
      LOG.error("The Dataset is not owned by the organization specified in the credentials");
      return false;
    }

    // check if an organisationKey was included in form params, that the organization keys match
    UUID organizationKeyFromFormParams = getOrganizationKeyFromParams();
    if (organizationKeyFromFormParams != null) {
      if (organizationKeyFromFormParams.compareTo(userKey) != 0) {
        LOG.error("Different organization keys were specified in the form parameters and credentials");
        return false;
      }
    }
    LOG.info("Authorization succeeded, can modify dataset with key={} belonging to organization with key={}",
      datasetKey.toString(), userKey.toString());
    return true;
  }

  /**
   * Determine if HTTP request is authorized to modify Installation.
   *
   * @param installationKey Installation key
   *
   * @return true if the HTTP request is authorized to modify Installation
   */
  public boolean isAuthorizedToModifyInstallation(UUID installationKey) {
    // retrieve path param for installation key
    if (installationKey == null) {
      return false;
    }
    // validate installation key belongs to an existing installation
    Installation installation = installationService.get(installationKey);
    return installation != null && installation.getKey().equals(userKey);
  }

  @Override
  public Principal getUserPrincipal() {
    return new BasicUserPrincipal(userKey.toString());
  }

  @Override
  public boolean isUserInRole(String role) {
    return false;
  }

  @Override
  public boolean isSecure() {
    return false;
  }

  @Override
  public String getAuthenticationScheme() {
    return SecurityContext.BASIC_AUTH;
 }

  public UUID getUserKey() {
    return userKey;
  }
}
