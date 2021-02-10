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
package org.gbif.registry.security;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.vocabulary.TagNamespace;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.InstallationMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.persistence.mapper.UserRightsMapper;

import java.util.UUID;

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
  private final MachineTagMapper machineTagMapper;

  public EditorAuthorizationServiceImpl(
      OrganizationMapper organizationMapper,
      DatasetMapper datasetMapper,
      InstallationMapper installationMapper,
      UserRightsMapper userRightsMapper,
      MachineTagMapper machineTagMapper) {
    this.organizationMapper = organizationMapper;
    this.datasetMapper = datasetMapper;
    this.installationMapper = installationMapper;
    this.userRightsMapper = userRightsMapper;
    this.machineTagMapper = machineTagMapper;
  }

  @Deprecated
  @Override
  public boolean allowedToModifyNamespace(String name, String ns) {
    if (name == null) {
      return false;
    }
    return userRightsMapper.namespaceExistsForUser(name, ns);
  }

  @Deprecated
  @Override
  public boolean allowedToDeleteMachineTag(String name, int machineTagKey) {
    if (name == null) {
      return false;
    }
    return userRightsMapper.allowedToDeleteMachineTag(name, machineTagKey);
  }

  @Override
  public boolean allowedToCreateMachineTag(String name, UUID datasetKey, MachineTag machineTag) {
    if (name == null || datasetKey == null || machineTag == null) {
      return false;
    }

    return TagNamespace.GBIF_DEFAULT_TERM.getNamespace().equals(machineTag.getNamespace())
        && allowedToModifyDataset(name, datasetKey);
  }

  @Override
  public boolean allowedToDeleteMachineTag(String name, UUID datasetKey, int machineTagKey) {
    if (name == null || datasetKey == null) {
      return false;
    }

    MachineTag machineTag = machineTagMapper.get(machineTagKey);
    if (machineTag == null) {
      return false;
    }

    return TagNamespace.GBIF_DEFAULT_TERM.getNamespace().equals(machineTag.getNamespace())
        && allowedToModifyDataset(name, datasetKey);
  }

  @Override
  public boolean allowedToModifyEntity(String name, UUID key) {
    if (name == null || key == null) {
      return false;
    }
    boolean allowed = userRightsMapper.keyExistsForUser(name, key);
    LOG.debug("User {} {} allowed to edit entity {}", name, allowed ? "is" : "is not", key);
    return allowed;
  }

  @Override
  public boolean allowedToModifyEntity(String name, NetworkEntity entity) {
    if (name == null || entity == null) {
      return false;
    }
    UUID key = entity.getKey();
    boolean allowed = key != null && userRightsMapper.keyExistsForUser(name, key);
    LOG.debug("User {} {} allowed to edit entity {}", name, allowed ? "is" : "is not", key);
    return allowed;
  }

  @Override
  public boolean allowedToModifyDataset(String name, UUID datasetKey) {
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
  public boolean allowedToModifyDataset(String name, Dataset dataset) {
    if (name == null || dataset == null) {
      return false;
    }
    UUID key = dataset.getKey();
    if (key != null && allowedToModifyEntity(name, key)) {
      return true;
    }
    // try installation rights
    if (allowedToModifyInstallation(name, dataset.getInstallationKey())) {
      return true;
    }
    // try higher organization or node rights
    return allowedToModifyOrganization(name, dataset.getPublishingOrganizationKey());
  }

  @Override
  public boolean allowedToModifyOrganization(String name, UUID orgKey) {
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
  public boolean allowedToModifyOrganization(String name, Organization organization) {
    if (name == null || organization == null) {
      return false;
    }
    UUID key = organization.getKey();
    if (key != null && allowedToModifyEntity(name, key)) {
      return true;
    }
    // try endorsing node
    return allowedToModifyEntity(name, organization.getEndorsingNodeKey());
  }

  @Override
  public boolean allowedToModifyInstallation(String name, UUID installationKey) {
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

  @Override
  public boolean allowedToModifyInstallation(String name, Installation installation) {
    if (name == null || installation == null) {
      return false;
    }
    UUID key = installation.getKey();
    if (key != null && allowedToModifyEntity(name, key)) {
      return true;
    }
    // try higher organization or node rights
    return allowedToModifyOrganization(name, installation.getOrganizationKey());
  }
}
