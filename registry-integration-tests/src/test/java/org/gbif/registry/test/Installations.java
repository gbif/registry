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
package org.gbif.registry.test;

import org.gbif.api.model.registry.Installation;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.UUID;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class Installations extends JsonBackedData<Installation> {

  private InstallationService installationService;

  @Autowired
  public Installations(
      InstallationService installationService,
      ObjectMapper objectMapper,
      SimplePrincipalProvider simplePrincipalProvider) {
    super(
        "data/installation.json",
        new TypeReference<Installation>() {},
        objectMapper,
        simplePrincipalProvider);
    this.installationService = installationService;
  }

  public Installation newInstance(UUID organizationKey) {
    Installation i = super.newInstance();
    i.setOrganizationKey(organizationKey);
    return i;
  }

  /**
   * Persist a new Installation associated to a hosting organization for use in Unit Tests.
   *
   * @param organizationKey hosting organization key
   * @return persisted Installation
   */
  public Installation newPersistedInstance(UUID organizationKey) {
    Installation i = newInstance(organizationKey);
    // password was not included in installation.json, so set it here
    i.setPassword("password");
    UUID key = installationService.create(i);
    // some properties like created, modified are only set when the installation is retrieved anew
    return installationService.get(key);
  }

  /**
   * Populate credentials used in Installation update ws request.
   *
   * @param installation Installation
   * @return credentials
   */
  public UsernamePasswordCredentials credentials(Installation installation) {
    return new UsernamePasswordCredentials(
        installation.getKey().toString(), installation.getPassword());
  }
}
