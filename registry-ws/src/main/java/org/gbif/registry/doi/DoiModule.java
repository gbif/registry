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
package org.gbif.registry.doi;

import org.gbif.api.service.registry.OccurrenceDownloadService;
import org.gbif.registry.doi.generator.DoiGenerator;
import org.gbif.registry.doi.generator.DoiGeneratorMQ;
import org.gbif.registry.doi.handler.DataCiteDoiHandlerStrategy;
import org.gbif.registry.ws.resources.OccurrenceDownloadResource;

import java.net.URI;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import com.google.common.collect.Lists;
import com.google.inject.PrivateModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import org.apache.commons.lang3.StringUtils;

/**
 * Assemble the classes related to DOI.
 * Requires an existing MessagePublisher, DoiPersistenceService, OrganizationService, OccurrenceDownloadResource,
 * TitleLookup being bound.
 */
public class DoiModule extends PrivateModule {

  private final Properties properties;

  public DoiModule(Properties properties) {
    this.properties = properties;
  }

  @Override
  protected void configure() {

    // Bind the OccurrenceDownloadResource as OccurrenceDownloadService
    bind(OccurrenceDownloadService.class).to(OccurrenceDownloadResource.class).in(Scopes.SINGLETON);

    bind(String.class).annotatedWith(Names.named("doi.prefix")).toInstance(properties.getProperty("doi.prefix"));
    bind(URI.class).annotatedWith(Names.named("portal.url")).toInstance(URI.create(properties.getProperty("portal.url")));

    bind(new TypeLiteral<List<UUID>>(){}).annotatedWith(Names.named("parentDatasetExcludeList"))
            .toInstance(extractList(properties.getProperty("doi.dataset.parentExcludeList")));

    bind(DoiGenerator.class).to(DoiGeneratorMQ.class).in(Scopes.SINGLETON);
    bind(DataCiteDoiHandlerStrategy.class).to(GbifDataCiteDoiHandlerStrategy.class).in(Scopes.SINGLETON);

    expose(DoiGenerator.class);
    expose(DataCiteDoiHandlerStrategy.class);
  }

  /**
   * Generate a list of dataset UUID from a comma separated list
   * @param propertyValue
   * @return a new list, never null
   * @throws IllegalArgumentException if an element is not a UUID
   */
  private List<UUID> extractList(String propertyValue){
    List<UUID> parentExcludeList = Lists.newArrayList();

    if (StringUtils.isBlank(propertyValue)) {
      return parentExcludeList;
    }

    String[] values = propertyValue.split(",");
    for(String value : values){
      parentExcludeList.add(UUID.fromString(value.trim()));
    }
    return parentExcludeList;
  }

}
