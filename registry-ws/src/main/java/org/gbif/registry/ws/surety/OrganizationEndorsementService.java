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
package org.gbif.registry.ws.surety;

import org.gbif.api.model.registry.EndorsementStatus;
import org.gbif.api.model.registry.Organization;

import java.util.UUID;

/**
 * Service responsible to handle the business logic of creating and confirming {@link Organization}.
 */
public interface OrganizationEndorsementService<T> {

  /**
   * Handles the logic on new organization.
   *
   * @param newOrganization new organization
   */
  void onNewOrganization(Organization newOrganization);

  /**
   * Confirm the endorsement of an organization using confirmation object.
   *
   * @param organizationKey organization key
   * @param confirmationObject object used to handle confirmation by the implementation.
   * @return the organization endorsement was approved or not
   */
  boolean confirmEndorsement(UUID organizationKey, T confirmationObject);

  /**
   * Confirm the endorsement of an organization without a confirmation object.
   *
   * @param organizationKey organization key
   * @return the organization endorsement was approved or not
   */
  boolean confirmEndorsement(UUID organizationKey);

  /**
   * Revoke the endorsement of the organization.
   *
   * @param organizationKey organization key
   * @return the organization endorsement was revoked or not
   */
  boolean revokeEndorsement(UUID organizationKey);

  /**
   * Change the endorsement status of the organization.
   *
   * @param organizationKey organization key
   * @param status new endorsement status
   */
  void changeEndorsementStatus(UUID organizationKey, EndorsementStatus status);
}
