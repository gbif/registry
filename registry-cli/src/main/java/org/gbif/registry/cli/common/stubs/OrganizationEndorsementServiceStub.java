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
package org.gbif.registry.cli.common.stubs;

import org.gbif.api.model.registry.EndorsementStatus;
import org.gbif.api.model.registry.Organization;
import org.gbif.registry.ws.surety.OrganizationEndorsementService;

import java.util.UUID;

/**
 * Stub class used to simplify binding, e.g. when this class must be bound but isn't actually used.
 */
public class OrganizationEndorsementServiceStub implements OrganizationEndorsementService<UUID> {

  @Override
  public void onNewOrganization(Organization newOrganization) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean confirmEndorsement(UUID organizationKey, UUID confirmationObject) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean confirmEndorsement(UUID organizationKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean revokeEndorsement(UUID organizationKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void changeEndorsementStatus(UUID organizationKey, EndorsementStatus status) {
    throw new UnsupportedOperationException();
  }
}
