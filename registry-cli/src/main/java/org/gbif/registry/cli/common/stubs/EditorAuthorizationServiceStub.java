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
package org.gbif.registry.cli.common.stubs;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.model.registry.Organization;
import org.gbif.registry.security.EditorAuthorizationService;

import java.util.UUID;

/**
 * Stub class used to simplify binding, e.g. when this class must be bound but isn't actually used.
 */
public class EditorAuthorizationServiceStub implements EditorAuthorizationService {

  @Override
  public boolean allowedToModifyNamespace(String user, String ns) {
    return false;
  }

  @Override
  public boolean allowedToDeleteMachineTag(String user, int machineTagKey) {
    return false;
  }

  @Override
  public boolean allowedToCreateMachineTag(String name, UUID datasetKey, MachineTag machineTag) {
    return false;
  }

  @Override
  public boolean allowedToDeleteMachineTag(String name, UUID datasetKey, int machineTagKey) {
    return false;
  }

  @Override
  public boolean allowedToModifyEntity(String user, UUID key) {
    return false;
  }

  @Override
  public boolean allowedToModifyEntity(String name, NetworkEntity entity) {
    return false;
  }

  @Override
  public boolean allowedToModifyDataset(String user, UUID datasetKey) {
    return false;
  }

  @Override
  public boolean allowedToModifyDataset(String name, Dataset dataset) {
    return false;
  }

  @Override
  public boolean allowedToModifyOrganization(String user, UUID orgKey) {
    return false;
  }

  @Override
  public boolean allowedToModifyOrganization(String name, Organization organization) {
    return false;
  }

  @Override
  public boolean allowedToModifyInstallation(String user, UUID installationKey) {
    return false;
  }

  @Override
  public boolean allowedToModifyInstallation(String name, Installation installation) {
    return false;
  }
}
