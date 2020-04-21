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
package org.gbif.registry.metasync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Runner {

  private static final Logger LOG = LoggerFactory.getLogger(Runner.class);

  public static void main(String[] args) {
    //    SLF4JBridgeHandler.removeHandlersForRootLogger();
    //    SLF4JBridgeHandler.install();
    //
    //    ClientFactory clientFactory = new ClientFactory("username", "http://localhost:8080",
    // "username",
    //      "password");
    //    InstallationService installationService =
    // clientFactory.newInstance(InstallationClient.class);
    //
    //    MetadataSynchroniserImpl synchroniser = new MetadataSynchroniserImpl(installationService);
    //
    //    synchroniser
    //      .registerProtocolHandler(new
    // DigirMetadataSynchroniser(clientFactory.provideHttpClient()));
    //    synchroniser
    //      .registerProtocolHandler(new
    // TapirMetadataSynchroniser(clientFactory.provideHttpClient()));
    //    synchroniser
    //      .registerProtocolHandler(new
    // BiocaseMetadataSynchroniser(clientFactory.provideHttpClient()));
    //
    //    DatasetService datasetService = clientFactory.newInstance(DatasetClient.class);
    //
    //    List<SyncResult> syncResults = synchroniser.synchroniseAllInstallations(100);
    //    LOG.info("Done syncing. Processing results");
    //    DebugHandler.processResults(syncResults);
    //
    //    RegistryUpdater updater = new RegistryUpdater(datasetService,
    //      ((MetasyncHistoryService) installationService));
    //    updater.saveSyncResultsToRegistry(syncResults);
  }

  private Runner() {
    throw new UnsupportedOperationException("Can't initialize class");
  }
}
