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
package org.gbif.registry.guice;

import org.gbif.common.messaging.api.MessagePublisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;

import static org.mockito.Mockito.mock;

/** Provides a mock doi service */
public class RabbitMockModule extends AbstractModule {
  private static final Logger LOG = LoggerFactory.getLogger(RabbitMockModule.class);

  @Override
  protected void configure() {
    MessagePublisher mp = mock(MessagePublisher.class);
    bind(MessagePublisher.class).toInstance(mp);
    LOG.info("Using a mocked Message Publisher");
  }
}
