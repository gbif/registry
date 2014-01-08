/*
 * Copyright 2013 Global Biodiversity Information Facility (GBIF)
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
package org.gbif.registry.utils;

import org.gbif.api.model.registry.Installation;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.registry.guice.RegistryTestModules;
import org.gbif.registry.ws.resources.InstallationResource;

import java.util.UUID;

import com.google.inject.Injector;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.codehaus.jackson.type.TypeReference;

public class Installations extends JsonBackedData<Installation> {

  private static final Installations INSTANCE = new Installations();
  private static InstallationService installationService;

  private Installations() {
    super("data/installation.json", new TypeReference<Installation>() {});
    Injector i = RegistryTestModules.webservice();
    installationService = i.getInstance(InstallationResource.class);
  }

  public static Installation newInstance(UUID organizationKey) {
    Installation i = INSTANCE.newTypedInstance();
    i.setOrganizationKey(organizationKey);
    return i;
  }

  /**
   * Persist a new Installation associated to a hosting organization for use in Unit Tests.
   *
   * @param organizationKey hosting organization key
   *
   * @return persisted Installation
   */
  public static Installation newPersistedInstance(UUID organizationKey) {
    Installation i = Installations.newInstance(organizationKey);
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
   *
   * @return credentials
   */
  public static UsernamePasswordCredentials credentials(Installation installation) {
    return new UsernamePasswordCredentials(installation.getKey().toString(), installation.getPassword());
  }
}
