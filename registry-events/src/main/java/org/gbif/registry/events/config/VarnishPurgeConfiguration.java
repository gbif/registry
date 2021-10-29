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
package org.gbif.registry.events.config;

import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.service.collections.PersonService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.events.VarnishPurgeListener;
import org.gbif.utils.HttpUtil;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VarnishPurgeConfiguration {

  private static final int DEFAULT_HTTP_TIMEOUT_MSECS = 2000;

  private final URI purgeUrl;
  private final Integer purgingThreads;

  public VarnishPurgeConfiguration(
      @Value("${api.cache.purge.url}") String purgeUrl,
      @Value("${api.cache.purge.threads}") Integer purgingThreads) {
    this.purgeUrl = URI.create(purgeUrl);
    this.purgingThreads = purgingThreads;
  }

  @Bean
  public VarnishPurgeListener varnishPurgeListener(
      EventManager eventManager,
      OrganizationService organizationService,
      InstallationService installationService,
      DatasetService datasetService,
      InstitutionService institutionService,
      CollectionService collectionService,
      PersonService personService) {
    return new VarnishPurgeListener(
        HttpUtil.newMultithreadedClient(DEFAULT_HTTP_TIMEOUT_MSECS, purgingThreads, purgingThreads),
        eventManager,
        purgeUrl,
        organizationService,
        installationService,
        datasetService,
        institutionService,
        collectionService,
        personService);
  }
}
