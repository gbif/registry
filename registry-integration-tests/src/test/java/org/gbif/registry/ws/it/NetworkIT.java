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
package org.gbif.registry.ws.it;

import org.gbif.api.model.registry.Network;
import org.gbif.api.service.registry.NetworkService;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.UUID;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

/**
 * This is parameterized to run the same test routines for the following:
 *
 * <ol>
 *   <li>The persistence layer
 *   <li>The WS service layer
 *   <li>The WS service client layer
 * </ol>
 */
@ContextConfiguration(initializers = {BaseItTest.ContextInitializer.class})
public class NetworkIT extends NetworkEntityIT<Network> {

  private final TestDataFactory testDataFactory;

  @Autowired
  public NetworkIT(
      NetworkService service,
      @Nullable SimplePrincipalProvider pp,
      TestDataFactory testDataFactory) {
    super(service, pp, testDataFactory);
    this.testDataFactory = testDataFactory;
  }

  @Override
  protected Network newEntity() {
    return testDataFactory.newNetwork();
  }

  /** Test doesn't make sense for a network. */
  @Override
  public void testCreateAsEditor() {}

  @Override
  protected Network duplicateForCreateAsEditorTest(Network entity) throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  protected UUID keyForCreateAsEditorTest(Network entity) {
    return null;
  }
}
