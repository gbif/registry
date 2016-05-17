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

import org.gbif.registry.doi.generator.DoiGenerator;
import org.gbif.registry.doi.generator.DoiGeneratorMQ;
import org.gbif.registry.doi.handler.DataCiteDOIHandlerStrategy;
import org.gbif.registry.doi.handler.GbifDataCiteDOIHandlerStrategy;

import java.net.URI;
import java.util.Properties;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;

/**
 * Exposes a DoiGenerator service requiring an existing MessagePublisher and DoiMapper being bound.
 */
public class DoiModule extends AbstractModule {
  private final Properties properties;

  public DoiModule(Properties properties) {
    this.properties = properties;
  }

  @Override
  protected void configure() {
    bind(DoiGenerator.class).to(DoiGeneratorMQ.class).in(Scopes.SINGLETON);
    bind(DataCiteDOIHandlerStrategy.class).to(GbifDataCiteDOIHandlerStrategy.class).in(Scopes.SINGLETON);

    bind(String.class).annotatedWith(Names.named("doi.prefix")).toInstance(properties.getProperty("doi.prefix"));
    bind(URI.class).annotatedWith(Names.named("portal.url")).toInstance(URI.create(properties.getProperty("portal.url")));
  }

}
