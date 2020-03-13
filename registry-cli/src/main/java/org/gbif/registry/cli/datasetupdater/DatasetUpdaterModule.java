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
package org.gbif.registry.cli.datasetupdater;

import org.gbif.registry.cli.common.spring.SpringContextBuilder;
import org.gbif.registry.cli.common.stubs.DoiGeneratorStub;
import org.gbif.registry.cli.common.stubs.DoiHandlerStrategyStub;
import org.gbif.registry.cli.common.stubs.EditorAuthorizationServiceStub;
import org.gbif.registry.cli.common.stubs.EventManagerStub;
import org.gbif.registry.cli.common.stubs.SearchServiceStub;
import org.gbif.registry.service.RegistryDatasetServiceImpl;
import org.gbif.registry.ws.resources.DatasetResource;

import org.springframework.context.ApplicationContext;

/** A Spring module used exclusively by DatasetUpdater, to use DatasetResource directly. */
public class DatasetUpdaterModule {

  private final DatasetUpdaterConfiguration config;

  public DatasetUpdaterModule(DatasetUpdaterConfiguration config) {
    this.config = config;
  }

  public ApplicationContext getContext() {

    return SpringContextBuilder.create()
        .withDbConfiguration(config.db)
        .withComponents(
            SearchServiceStub.class,
            DoiGeneratorStub.class,
            DoiHandlerStrategyStub.class,
            EditorAuthorizationServiceStub.class,
            EventManagerStub.class,
            RegistryDatasetServiceImpl.class,
            DatasetResource.class)
        .build();
  }
}
