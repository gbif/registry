package org.gbif.registry.ws.security;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.persistence.mapper.UserRightsMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.UUID;

@Service
@Primary
public class EditorAuthorizationServiceImpl implements EditorAuthorizationService {

  private static final Logger LOG = LoggerFactory.getLogger(EditorAuthorizationServiceImpl.class);

  private UserRightsMapper userRightsMapper;
  private DatasetService datasetService;
  private InstallationService installationService;
  private OrganizationService organizationService;

  public EditorAuthorizationServiceImpl(@Lazy DatasetService datasetService,
                                        @Lazy InstallationService installationService,
                                        @Lazy OrganizationService organizationService,
                                        UserRightsMapper userRightsMapper) {
    this.datasetService = datasetService;
    this.installationService = installationService;
    this.organizationService = organizationService;
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
    Dataset d = datasetService.get(datasetKey);
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
    Organization o = organizationService.get(orgKey);
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
    Installation inst = installationService.get(installationKey);
    return inst != null && allowedToModifyOrganization(name, inst.getOrganizationKey());
  }
}
