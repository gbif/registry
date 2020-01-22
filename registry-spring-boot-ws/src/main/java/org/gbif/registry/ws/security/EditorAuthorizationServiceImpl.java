package org.gbif.registry.ws.security;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.InstallationMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.persistence.mapper.UserRightsMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.UUID;

@Service
@Primary
public class EditorAuthorizationServiceImpl implements EditorAuthorizationService {

  private static final Logger LOG = LoggerFactory.getLogger(EditorAuthorizationServiceImpl.class);

  private final UserRightsMapper userRightsMapper;
  private final OrganizationMapper organizationMapper;
  private final DatasetMapper datasetMapper;
  private final InstallationMapper installationMapper;

  public EditorAuthorizationServiceImpl(OrganizationMapper organizationMapper,
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
