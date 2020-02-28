/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.registry.ws.security;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.InstallationMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.persistence.mapper.UserRightsMapper;

import java.util.UUID;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class EditorAuthorizationServiceImpl implements EditorAuthorizationService {

  private static final Logger LOG = LoggerFactory.getLogger(EditorAuthorizationServiceImpl.class);

  private final UserRightsMapper userRightsMapper;
  private final OrganizationMapper organizationMapper;
  private final DatasetMapper datasetMapper;
  private final InstallationMapper installationMapper;

  public EditorAuthorizationServiceImpl(
      OrganizationMapper organizationMapper,
      DatasetMapper datasetMapper,
      InstallationMapper installationMapper,
      UserRightsMapper userRightsMapper) {
    this.organizationMapper = organizationMapper;
    this.datasetMapper = datasetMapper;
    this.installationMapper = installationMapper;
    this.userRightsMapper = userRightsMapper;
  }

  @Override
  public boolean allowedToModifyNamespace(@Nullable String name, String ns) {
    if (name == null) {
      return false;
    }
    return userRightsMapper.namespaceExistsForUser(name, ns);
  }

  @Override
  public boolean allowedToDeleteMachineTag(@Nullable String name, int machineTagKey) {
    if (name == null) {
      return false;
    }
    return userRightsMapper.allowedToDeleteMachineTag(name, machineTagKey);
  }

  @Override
  public boolean allowedToModifyEntity(@Nullable String name, UUID key) {
    if (name == null) {
      return false;
    }
    boolean allowed = userRightsMapper.keyExistsForUser(name, key);
    LOG.debug("User {} {} allowed to edit entity {}", name, allowed ? "is" : "is not", key);
    return allowed;
  }

  @Override
  public boolean allowedToModifyDataset(@Nullable String name, UUID datasetKey) {
    if (name == null) {
      return false;
    }
    if (allowedToModifyEntity(name, datasetKey)) {
      return true;
    }
    Dataset d = datasetMapper.get(datasetKey);
    // try installation rights
    if (d != null && allowedToModifyInstallation(name, d.getInstallationKey())) {
      return true;
    }
    // try higher organization or node rights
    return d != null && allowedToModifyOrganization(name, d.getPublishingOrganizationKey());
  }

  @Override
  public boolean allowedToModifyOrganization(@Nullable String name, UUID orgKey) {
    if (name == null) {
      return false;
    }
    if (allowedToModifyEntity(name, orgKey)) {
      return true;
    }
    // try endorsing node
    Organization o = organizationMapper.get(orgKey);
    return o != null && allowedToModifyEntity(name, o.getEndorsingNodeKey());
  }

  @Override
  public boolean allowedToModifyInstallation(@Nullable String name, UUID installationKey) {
    if (name == null) {
      return false;
    }
    if (allowedToModifyEntity(name, installationKey)) {
      return true;
    }
    // try higher organization or node rights
    Installation inst = installationMapper.get(installationKey);
    return inst != null && allowedToModifyOrganization(name, inst.getOrganizationKey());
  }
}
