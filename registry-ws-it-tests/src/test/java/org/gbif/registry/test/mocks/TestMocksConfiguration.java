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
package org.gbif.registry.test.mocks;

import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.occurrence.query.TitleLookupService;
import org.gbif.registry.directory.Augmenter;
import org.gbif.registry.mail.EmailSender;
import org.gbif.registry.search.dataset.indexing.checklistbank.ChecklistbankPersistenceService;
import org.gbif.registry.ws.surety.OrganizationEmailEndorsementService;

import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/** */
@Configuration
public class TestMocksConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(TestMocksConfiguration.class);

  // use InMemoryEmailSender if devemail is disabled
  @Bean
  @ConditionalOnProperty(value = "mail.devemail.enabled", havingValue = "false")
  public EmailSender emailSender() {
    LOG.info("ImMemoryEmailSender (stub) activated");
    return new InMemoryEmailSender();
  }

  // use stub instead of rabbit MQ if message is disabled
  @Bean
  @ConditionalOnProperty(value = "message.enabled", havingValue = "false")
  public MessagePublisher testMessagePublisher() {
    LOG.info("MessagePublisherStub activated");
    return new MessagePublisherStub();
  }

  @Bean
  @Primary
  public Augmenter augmenter() {
    return new AugmenterStub();
  }

  @Bean
  public InMemoryEmailSender inMemoryEmailSender() {
    return new InMemoryEmailSender();
  }

  @Bean
  public TitleLookupService titleLookupServiceMock() {
    return new TitleLookupServiceMock();
  }

  @Bean
  public OrganizationEmailEndorsementService organizationEndorsementServiceMock() {
    return Mockito.mock(OrganizationEmailEndorsementService.class);
  }

  @Bean
  public ChecklistbankPersistenceService checklistbankPersistenceService() {
    return new ChecklistbankPersistenceService() {
      @Override
      public Integer[] getTaxonKeys(String datasetKey) {
        return new Integer[0];
      }
    };
  }
}
