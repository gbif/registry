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
package org.gbif.registry.utils;

import org.gbif.api.model.registry.PrePersist;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.registry.ws.it.RegistryIntegrationTestsConfiguration;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.groups.Default;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

/** Tests that the builders in this package provide valid objects. */
@SpringBootTest(classes = EntityBuilderTest.EntityBuilderTestConfiguration.class)
public class EntityBuilderTest extends BaseItTest {

  @Configuration
  static class EntityBuilderTestConfiguration extends RegistryIntegrationTestsConfiguration {}

  private static final Logger LOG = LoggerFactory.getLogger(EntityBuilderTest.class);

  private final TestDataFactory testDataFactory;

  @Autowired
  public EntityBuilderTest(
      TestDataFactory testDataFactory, @Nullable SimplePrincipalProvider simplePrincipalProvider) {
    super(simplePrincipalProvider);
    this.testDataFactory = testDataFactory;
  }

  @Test
  public void testBuilders() {
    test(testDataFactory.newComment());
    test(testDataFactory.newContact());
    test(testDataFactory.newDataset(UUID.randomUUID(), UUID.randomUUID()));
    test(testDataFactory.newEndpoint());
    test(testDataFactory.newIdentifier());
    test(testDataFactory.newInstallation(UUID.randomUUID()));
    test(testDataFactory.newMachineTag());
    test(testDataFactory.newNetwork());
    test(testDataFactory.newNode());
    test(testDataFactory.newOrganization(UUID.randomUUID()));
  }

  private <T> void test(T entity) {
    ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
    Validator validator = validatorFactory.getValidator();

    Set<ConstraintViolation<T>> violations =
        validator.validate(entity, PrePersist.class, Default.class);
    for (ConstraintViolation<T> cv : violations) {
      LOG.info(
          "Class[{}] property[{}] failed validation with[{}]",
          entity.getClass().getSimpleName(),
          cv.getPropertyPath(),
          cv.getMessage());
    }
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }
}
