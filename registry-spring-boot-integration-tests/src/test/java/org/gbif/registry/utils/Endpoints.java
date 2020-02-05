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

import org.gbif.api.model.registry.Endpoint;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.hibernate.validator.HibernateValidator;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;

import static org.junit.Assert.assertTrue;

public class Endpoints extends JsonBackedData<Endpoint> {

  private static final Endpoints INSTANCE = new Endpoints();

  public static Endpoint newInstance() {
    Endpoint endpoint = INSTANCE.newTypedInstance();
    // Endpoint is unique in that nested machine tags will be created
    endpoint.addMachineTag(MachineTags.newInstance());
    return endpoint;
  }

  public Endpoints() {
    super("data/endpoint.json", new TypeReference<Endpoint>() {});
  }

  @Test
  public void testConstraints() {
    ValidatorFactory validatorFactory =
        Validation.byProvider(HibernateValidator.class).configure().buildValidatorFactory();
    Validator validator = validatorFactory.getValidator();

    Set<ConstraintViolation<Endpoint>> violations = validator.validate(Endpoints.newInstance());
    assertTrue(violations.isEmpty());
  }
}
