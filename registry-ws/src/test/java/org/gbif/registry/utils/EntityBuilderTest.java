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
package org.gbif.registry.utils;

import org.gbif.api.model.registry.PrePersist;

import java.util.Set;
import java.util.UUID;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.groups.Default;

import org.apache.bval.jsr303.ApacheValidationProvider;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests that the builders in this package provide valid objects.
 */
public class EntityBuilderTest {

  private static final Logger LOG = LoggerFactory.getLogger(EntityBuilderTest.class);

  @Test
  public void testBuilders() {
    test(Comments.newInstance());
    test(Contacts.newInstance());
    test(Datasets.newInstance(UUID.randomUUID(), UUID.randomUUID()));
    test(Endpoints.newInstance());
    test(Identifiers.newInstance());
    test(Installations.newInstance(UUID.randomUUID()));
    test(MachineTags.newInstance());
    test(Networks.newInstance());
    test(Nodes.newInstance());
    test(Organizations.newInstance(UUID.randomUUID()));
  }

  private <T> void test(T entity) {
    ValidatorFactory validatorFactory =
      Validation.byProvider(ApacheValidationProvider.class).configure().buildValidatorFactory();
    Validator validator = validatorFactory.getValidator();

    Set<ConstraintViolation<T>> violations = validator.validate(entity, PrePersist.class, Default.class);
    for (ConstraintViolation<T> cv : violations) {
      LOG.info("Class[{}] property[{}] failed validation with[{}]",
        entity.getClass().getSimpleName(),
        String.valueOf(cv.getPropertyPath()),
        cv.getMessage());
    }
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }

}
