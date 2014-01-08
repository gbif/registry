package org.gbif.registry.ws.security;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.persistence.mapper.UserRightsMapper;

import java.security.Principal;
import java.util.UUID;

import com.google.inject.Inject;

public class EditorAuthorizationServiceImpl implements EditorAuthorizationService {

  private UserRightsMapper userRightsMapper;
  private DatasetService datasetService;
  private InstallationService installationService;
  private OrganizationService organizationService;

  @Inject
  public EditorAuthorizationServiceImpl(DatasetService datasetService, InstallationService installationService,
    OrganizationService organizationService, UserRightsMapper userRightsMapper) {
    this.datasetService = datasetService;
    this.installationService = installationService;
    this.organizationService = organizationService;
    this.userRightsMapper = userRightsMapper;
  }

    @Override
    public boolean allowedToModifyNamespace(Principal user, String ns) {
        if (user == null) {
            return false;
        }
        return userRightsMapper.namespaceExistsForUser(user.getName(), ns);
    }

    @Override
    public boolean allowedToDeleteMachineTag(Principal user, int machineTagKey) {
        if (user == null) {
            return false;
        }
        return userRightsMapper.allowedToDeleteMachineTag(user.getName(), machineTagKey);
    }

    @Override
  public boolean allowedToModifyEntity(Principal user, UUID key) {
    if (user == null) {
      return false;
    }
    return userRightsMapper.keyExistsForUser(user.getName(), key);
  }

  @Override
  public boolean allowedToModifyDataset(Principal user, UUID datasetKey) {
    if (user == null) {
      return false;
    }
    if (allowedToModifyEntity(user, datasetKey)) {
      return true;
    }
    // try higher organization or node rights
    Dataset d = datasetService.get(datasetKey);
    return d == null ? false : allowedToModifyOrganization(user, d.getOwningOrganizationKey());
  }

  @Override
  public boolean allowedToModifyOrganization(Principal user, UUID orgKey) {
    if (user == null) {
      return false;
    }
    if (allowedToModifyEntity(user, orgKey)) {
      return true;
    }
    // try endorsing node
    Organization o = organizationService.get(orgKey);
    return o == null ? false : allowedToModifyEntity(user, o.getEndorsingNodeKey());
  }

  @Override
  public boolean allowedToModifyInstallation(Principal user, UUID installationKey) {
    if (user == null) {
      return false;
    }
    if (allowedToModifyEntity(user, installationKey)) {
      return true;
    }
    // try higher organization or node rights
    Installation inst = installationService.get(installationKey);
    return inst == null ? false : allowedToModifyOrganization(user, inst.getOrganizationKey());
  }
}
