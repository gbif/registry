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
package org.gbif.registry.ws;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.elasticsearch.ElasticSearchRestHealthContributorAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {ElasticSearchRestHealthContributorAutoConfiguration.class})
@MapperScan("org.gbif.registry.persistence.mapper")
@EnableConfigurationProperties
@ComponentScan(
    basePackages = {
      "org.gbif.ws.server.interceptor",
      "org.gbif.ws.server.aspect",
      "org.gbif.ws.server.filter",
      "org.gbif.ws.server.advice",
      "org.gbif.ws.server.mapper",
      "org.gbif.ws.security",
      "org.gbif.registry.search.dataset.indexing",
      "org.gbif.registry.search.dataset.service",
      "org.gbif.registry.search",
      "org.gbif.registry.ws.advice",
      "org.gbif.registry.ws.config",
      "org.gbif.registry.ws.resources",
      "org.gbif.registry.ws.surety",
      "org.gbif.registry.security",
      "org.gbif.registry.persistence",
      "org.gbif.registry.identity",
      "org.gbif.registry.surety",
      "org.gbif.registry.mail",
      "org.gbif.registry.doi",
      "org.gbif.registry.pipelines",
      "org.gbif.registry.directory",
      "org.gbif.registry.events",
      "org.gbif.registry.messaging",
      "org.gbif.registry.oaipmh",
      "org.gbif.registry.service"
    },
    excludeFilters = {@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE)})
@EnableScheduling
@EnableFeignClients
public class RegistryWsApplication {
  public static void main(String[] args) {
    SpringApplication.run(RegistryWsApplication.class, args);
  }
}
