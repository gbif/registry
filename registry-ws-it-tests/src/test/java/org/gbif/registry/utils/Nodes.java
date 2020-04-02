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

import org.gbif.api.model.registry.Node;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.bval.jsr303.ApacheValidationProvider;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.junit.Assert.assertTrue;

@Component
public class Nodes extends JsonBackedData2<Node> {

  private final ObjectMapper objectMapper;

  private static Nodes INSTANCE;

  public static Node newInstance(ObjectMapper objectMapper) {
    if (INSTANCE == null) {
      INSTANCE = new Nodes(objectMapper);
    }
    return INSTANCE.newTypedInstance();
  }

  @Autowired
  public Nodes(ObjectMapper objectMapper) {
    super("data/node.json", new TypeReference<Node>() {}, objectMapper);
    this.objectMapper = objectMapper;
  }

  @Test
  public void testConstraints() {
    ValidatorFactory validatorFactory =
        Validation.byProvider(ApacheValidationProvider.class).configure().buildValidatorFactory();
    Validator validator = validatorFactory.getValidator();

    Set<ConstraintViolation<Node>> violations = validator.validate(Nodes.newInstance(objectMapper));
    assertTrue(violations.isEmpty());
  }
}
