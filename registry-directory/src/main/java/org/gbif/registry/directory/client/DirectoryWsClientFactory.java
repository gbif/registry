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
package org.gbif.registry.directory.client;

import org.gbif.api.service.directory.NodePersonService;
import org.gbif.api.service.directory.NodeService;
import org.gbif.api.service.directory.ParticipantPersonService;
import org.gbif.api.service.directory.ParticipantService;
import org.gbif.api.service.directory.PersonRoleService;
import org.gbif.api.service.directory.PersonService;
import org.gbif.directory.client.guice.DirectoryWsClientModule;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class DirectoryWsClientFactory {

  private final Injector injector;

  public DirectoryWsClientFactory(Properties properties) {
    injector = Guice.createInjector(new DirectoryWsClientModule(properties));
  }

  public DirectoryWsClientFactory(
      @Value("${directory.app.key}") String directoryAppKey,
      @Value("${directory.app.secret}") String directorySecret,
      @Value("${directory.ws.url}") String directoryUrl) {
    Properties properties = new Properties();
    properties.put(DirectoryWsClientModule.DIRECTORY_APP_KEY, directoryAppKey);
    properties.put(DirectoryWsClientModule.DIRECTORY_SECRET, directorySecret);
    properties.put(DirectoryWsClientModule.DIRECTORY_URL_KEY, directoryUrl);
    injector = Guice.createInjector(new DirectoryWsClientModule(properties));
  }

  public ParticipantService provideParticipantService() {
    return injector.getInstance(ParticipantService.class);
  }

  public NodeService provideNodeService() {
    return injector.getInstance(NodeService.class);
  }

  public PersonService providePersonService() {
    return injector.getInstance(PersonService.class);
  }

  public ParticipantPersonService provideParticipantPersonService() {
    return injector.getInstance(ParticipantPersonService.class);
  }

  public NodePersonService provideNodePersonService() {
    return injector.getInstance(NodePersonService.class);
  }

  public PersonRoleService providePersonRoleService() {
    return injector.getInstance(PersonRoleService.class);
  }
}
