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
package org.gbif.registry.metasync;

import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.MetasyncHistoryService;
import org.gbif.registry.metasync.api.SyncResult;
import org.gbif.registry.metasync.protocols.biocase.BiocaseMetadataSynchroniser;
import org.gbif.registry.metasync.protocols.digir.DigirMetadataSynchroniser;
import org.gbif.registry.metasync.protocols.tapir.TapirMetadataSynchroniser;
import org.gbif.registry.metasync.resulthandler.DebugHandler;
import org.gbif.registry.metasync.resulthandler.RegistryUpdater;
import org.gbif.registry.metasync.util.HttpClientFactory;
import org.gbif.registry.ws.client.DatasetClient;
import org.gbif.registry.ws.client.InstallationClient;
import org.gbif.ws.client.ClientBuilder;
import org.gbif.ws.json.JacksonJsonObjectMapperProvider;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

public final class Runner {

  private static final Logger LOG = LoggerFactory.getLogger(Runner.class);

  public static void main(String[] args) {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();

    HttpClientFactory clientFactory = new HttpClientFactory(10, TimeUnit.SECONDS);

    ClientBuilder clientBuilder = new ClientBuilder();
    clientBuilder.withObjectMapper(
        JacksonJsonObjectMapperProvider.getObjectMapperWithBuilderSupport());
    clientBuilder
        .withUrl("http://localhost:8080")
        .withAppKeyCredentials("username", "username", "password");

    InstallationService installationService = clientBuilder.build(InstallationClient.class);

    MetadataSynchroniserImpl synchroniser = new MetadataSynchroniserImpl(installationService);

    synchroniser.registerProtocolHandler(
        new DigirMetadataSynchroniser(clientFactory.provideHttpClient()));
    synchroniser.registerProtocolHandler(
        new TapirMetadataSynchroniser(clientFactory.provideHttpClient()));
    synchroniser.registerProtocolHandler(
        new BiocaseMetadataSynchroniser(clientFactory.provideHttpClient()));

    DatasetService datasetService = clientBuilder.build(DatasetClient.class);

    List<SyncResult> syncResults = synchroniser.synchroniseAllInstallations(100);
    LOG.info("Done syncing. Processing results");
    DebugHandler.processResults(syncResults);

    RegistryUpdater updater =
        new RegistryUpdater(datasetService, ((MetasyncHistoryService) installationService));
    updater.saveSyncResultsToRegistry(syncResults);
  }

  private Runner() {
    throw new UnsupportedOperationException("Can't initialize class");
  }
}
