/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.security;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.registry.domain.ws.util.LegacyResourceConstants;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.InstallationMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.ws.WebApplicationException;
import org.gbif.ws.security.LegacyRequestAuthorization;
import org.gbif.ws.util.CommonWsUtils;

import java.text.MessageFormat;
import java.util.Base64;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

/** Class providing temporary authorization for legacy web service requests (GBRDS/IPT). */
@Service
public class LegacyAuthorizationServiceImpl implements LegacyAuthorizationService {

  private static final Logger LOG = LoggerFactory.getLogger(LegacyAuthorizationServiceImpl.class);
  private static final Splitter COLON_SPLITTER = Splitter.on(":").limit(2);

  private final OrganizationMapper organizationMapper;
  private final DatasetMapper datasetMapper;
  private final InstallationMapper installationMapper;

  public LegacyAuthorizationServiceImpl(
      OrganizationMapper organizationMapper,
      DatasetMapper datasetMapper,
      InstallationMapper installationMapper) {
    this.organizationMapper = organizationMapper;
    this.datasetMapper = datasetMapper;
    this.installationMapper = installationMapper;
  }

  /**
   * Evaluates the Basic authentication header and verifies provided password matches with the
   * registered one. Checks both for organizations and installation keys.
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

      String organizationKeyStr =
          CommonWsUtils.getFirst(
              httpRequest.getParameterMap(), LegacyResourceConstants.ORGANIZATION_KEY_PARAM);
      final UUID organizationKey =
          organizationKeyStr != null ? UUID.fromString(organizationKeyStr) : null;

      return authenticateInternal(user, password, organizationKey);
    } catch (NoSuchElementException e) {
      LOG.warn("Invalid Basic Authentication syntax: {}", authentication);
      throw new WebApplicationException(
          "Invalid Basic Authentication syntax", HttpStatus.BAD_REQUEST);
    } catch (IllegalArgumentException e) {
      // no valid username UUID
      LOG.warn("Invalid username, UUID required: {}", decrypted);
      throw new WebApplicationException(
          MessageFormat.format("Invalid username, UUID required {0}", decrypted),
          HttpStatus.UNAUTHORIZED);
    }
  }

  private LegacyRequestAuthorization authenticateInternal(
      UUID user, String password, UUID organizationKey) {
    // try to validate organization key first
    Organization org = organizationMapper.get(user);
    if (org != null) {
      if (password.equals(org.getPassword())) {
        return new LegacyRequestAuthorization(user, organizationKey);
      } else {
        throw new WebApplicationException(
            "Organization password does not match", HttpStatus.UNAUTHORIZED);
      }
    } else {
      // maybe an installation?
      Installation installation = installationMapper.get(user);

      if (installation != null) {
        if (password.equals(installation.getPassword())) {
          return new LegacyRequestAuthorization(user, organizationKey);
        } else {
          throw new WebApplicationException(
              MessageFormat.format("Installation password {0} does not match", organizationKey),
              HttpStatus.UNAUTHORIZED);
        }
      } else {
        throw new WebApplicationException(
            MessageFormat.format("Organization or installation {0} was not found", organizationKey),
            HttpStatus.UNAUTHORIZED);
      }
    }
  }

  /**
   * Determine if HTTP request is authorized to modify Organization. The difference between this
   * method and isAuthorizedToModifyOrganization(organizationKey) is that the organizationKey must
   * first be parsed from the form parameters.
   *
   * @return true if the HTTP request is authorized to modify Organization
   * @see this#isAuthorizedToModifyOrganization(LegacyRequestAuthorization, UUID)
   */
  @Override
  public boolean isAuthorizedToModifyOrganization(LegacyRequestAuthorization authorization) {
    // retrieve HTTP param for hosting organization key and convert incoming key into UUID
    UUID organizationKey = authorization.getOrganizationKey();

    return isAuthorizedToModifyOrganization(authorization, organizationKey);
  }

  @Override
  public boolean isAuthorizedToModifyOrganization(
      LegacyRequestAuthorization authorization, UUID organizationKey) {
    if (organizationKey == null) {
      LOG.error("Organization key is null");
      return false;
    }
    // validate installation key belongs to an existing installation
    Organization org = organizationMapper.get(organizationKey);

    if (org == null) {
      LOG.error("Organization with key={} does not exist", organizationKey);
      return false;
    }

    return org.getKey() != null && org.getKey().equals(authorization.getUserKey());
  }

  /**
   * Determine if HTTP request is authorized to modify a Dataset belonging to an Organization. This
   * method checks that the same organizationKey is specified in the credentials and HTTP form
   * parameters, that the Organization corresponding to that organizationKey exists, that the
   * Dataset corresponding to the datasetKey exists, that the Dataset is owned by that Organization,
   * and that the correct organization password has been supplied.
   *
   * @param datasetKey Dataset key
   * @return true if the HTTP request is authorized to modify a Dataset belonging to Organization
   */
  @Override
  public boolean isAuthorizedToModifyOrganizationsDataset(
      LegacyRequestAuthorization authorization, UUID datasetKey) {
    if (datasetKey == null) {
      LOG.error("Dataset key was null");
      return false;
    }
    // retrieve dataset to ensure it exists
    Dataset dataset = datasetMapper.get(datasetKey);

    if (dataset == null) {
      LOG.error("Dataset with key={} does not exist", datasetKey);
      return false;
    }

    // check the dataset belongs to organization
    if (dataset.getPublishingOrganizationKey().compareTo(authorization.getUserKey()) != 0) {
      LOG.error("The Dataset is not owned by the organization specified in the credentials");
      return false;
    }

    // check if an organisationKey was included in form params, that the organization keys match
    UUID organizationKeyFromFormParams = authorization.getOrganizationKey();
    if (organizationKeyFromFormParams != null
        && organizationKeyFromFormParams.compareTo(authorization.getUserKey()) != 0) {
      LOG.error(
          "Different organization keys were specified in the form parameters and credentials");
      return false;
    }
    LOG.info(
        "Authorization succeeded, can modify dataset with key={} belonging to organization with key={}",
        datasetKey,
        authorization.getUserKey());
    return true;
  }

  /**
   * Determine if HTTP request is authorized to modify Installation.
   *
   * @param installationKey Installation key
   * @return true if the HTTP request is authorized to modify Installation
   */
  @Override
  public boolean isAuthorizedToModifyInstallation(
      LegacyRequestAuthorization authorization, UUID installationKey) {
    // retrieve path param for installation key
    if (installationKey == null) {
      return false;
    }
    // validate installation key belongs to an existing installation
    Installation installation = installationMapper.get(installationKey);

    if (installation == null) {
      LOG.error("Installation with key={} does not exist", installationKey);
      return false;
    }

    return installation.getKey() != null
        && installation.getKey().equals(authorization.getUserKey());
  }
}
