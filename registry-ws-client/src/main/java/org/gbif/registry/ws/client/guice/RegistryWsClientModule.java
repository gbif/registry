/*
 * Copyright 2013 Global Biodiversity Information Facility (GBIF)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.ws.client.guice;

import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.service.collections.PersonService;
import org.gbif.api.service.common.IdentityAccessService;
import org.gbif.api.service.registry.DatasetOccurrenceDownloadUsageService;
import org.gbif.api.service.registry.DatasetProcessStatusService;
import org.gbif.api.service.registry.DatasetSearchService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.MetasyncHistoryService;
import org.gbif.api.service.registry.NetworkService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OccurrenceDownloadService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.doi.registration.DoiRegistrationService;
import org.gbif.registry.ws.client.DatasetOccurrenceDownloadUsageWsClient;
import org.gbif.registry.ws.client.DatasetSearchWsClient;
import org.gbif.registry.ws.client.DatasetWsClient;
import org.gbif.registry.ws.client.DoiRegistrationWsClient;
import org.gbif.registry.ws.client.InstallationWsClient;
import org.gbif.registry.ws.client.NetworkWsClient;
import org.gbif.registry.ws.client.NodeWsClient;
import org.gbif.registry.ws.client.OccurrenceDownloadWsClient;
import org.gbif.registry.ws.client.OrganizationWsClient;
import org.gbif.registry.ws.client.collections.CollectionWsClient;
import org.gbif.registry.ws.client.collections.InstitutionWsClient;
import org.gbif.registry.ws.client.collections.PersonWsClient;
import org.gbif.service.guice.PrivateServiceModule;
import org.gbif.ws.client.guice.AnonymousAuthModule;
import org.gbif.ws.client.guice.GbifApplicationAuthModule;
import org.gbif.ws.client.guice.GbifWsClientModule;
import org.gbif.ws.mixin.Mixins;

import java.util.Map;
import java.util.Properties;

import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

/**
 * A Module for Guice doing all the necessary wiring with the exception of the authentication filters.
 * In order to use this module an authentication module needs to be installed first, such as {@link AnonymousAuthModule}
 * for anonymous access or {@link GbifApplicationAuthModule} for trusted applications.
 *
 * With an authentication module installed, the only thing left for clients to do is to provide a property
 * to be bound to {@code registry.ws.url}.
 * If you want to use this module remember to also depend on Guice, jersey-apache-client4 and jersey-json in your
 * pom.xml file as they are declared as optional dependencies in this project.
 *
 * This module will NOT expose {@link IdentityAccessService}.
 */
public class RegistryWsClientModule extends GbifWsClientModule {

  public RegistryWsClientModule(Properties properties) {
    super(properties, NodeWsClient.class.getPackage(), CollectionWsClient.class.getPackage());
  }

  @Override
  protected void configureClient() {
    install(new InternalRegistryWsClientModule(getProperties()));

    expose(NodeService.class);
    expose(OrganizationService.class);
    expose(InstallationService.class);
    expose(DatasetService.class);
    expose(DatasetSearchService.class);
    expose(NetworkService.class);
    expose(OccurrenceDownloadService.class);
    expose(DatasetOccurrenceDownloadUsageService.class);
    expose(DatasetProcessStatusService.class);
    expose(MetasyncHistoryService.class);
    expose(DoiRegistrationService.class);
    expose(InstitutionService.class);
    expose(CollectionService.class);
    expose(PersonService.class);
  }

  @Override
  protected Map<Class<?>, Class<?>> getMixIns() {
    return Mixins.getPredefinedMixins();
  }

  // To allow the prefixing of the properties
  private class InternalRegistryWsClientModule extends PrivateServiceModule {

    private static final String PREFIX = "registry.";

    private InternalRegistryWsClientModule(Properties properties) {
      super(PREFIX, properties);
    }

    @Override
    protected void configureService() {

      bind(NodeService.class).to(NodeWsClient.class).in(Scopes.SINGLETON);
      bind(OrganizationService.class).to(OrganizationWsClient.class).in(Scopes.SINGLETON);
      bind(InstallationService.class).to(InstallationWsClient.class).in(Scopes.SINGLETON);
      bind(MetasyncHistoryService.class).to(InstallationWsClient.class).in(Scopes.SINGLETON);
      bind(DatasetService.class).to(DatasetWsClient.class).in(Scopes.SINGLETON);
      bind(DatasetSearchService.class).to(DatasetSearchWsClient.class).in(Scopes.SINGLETON);
      bind(NetworkService.class).to(NetworkWsClient.class).in(Scopes.SINGLETON);
      bind(OccurrenceDownloadService.class).to(OccurrenceDownloadWsClient.class).in(Scopes.SINGLETON);
      bind(DatasetOccurrenceDownloadUsageService.class).to(DatasetOccurrenceDownloadUsageWsClient.class).in(
        Scopes.SINGLETON);
      bind(DoiRegistrationService.class).to(DoiRegistrationWsClient.class).in(Scopes.SINGLETON);
      bind(DatasetProcessStatusService.class).to(DatasetWsClient.class).in(Scopes.SINGLETON);
      bind(InstitutionService.class).to(InstitutionWsClient.class).in(Scopes.SINGLETON);
      bind(CollectionService.class).to(CollectionWsClient.class).in(Scopes.SINGLETON);
      bind(PersonService.class).to(PersonWsClient.class).in(Scopes.SINGLETON);

      expose(NodeService.class);
      expose(OrganizationService.class);
      expose(InstallationService.class);
      expose(DatasetService.class);
      expose(DatasetSearchService.class);
      expose(NetworkService.class);
      expose(OccurrenceDownloadService.class);
      expose(DatasetOccurrenceDownloadUsageService.class);
      expose(DatasetProcessStatusService.class);
      expose(MetasyncHistoryService.class);
      expose(DoiRegistrationService.class);
      expose(InstitutionService.class);
      expose(CollectionService.class);
      expose(PersonService.class);
    }

    @Provides
    @Singleton
    @RegistryWs
    private WebResource provideBaseWsWebResource(Client client, @Named("ws.url") String url) {
      return client.resource(url);
    }
  }
}
