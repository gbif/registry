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
package org.gbif.registry.ws.aspect;

import org.gbif.registry.ws.annotation.ValidateReturnedValue;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class ValidateReturnValueAspect {

  private final Validator validator;

  public ValidateReturnValueAspect(Validator validator) {
    this.validator = validator;
  }

  @AfterReturning(
      pointcut = "@annotation(org.gbif.registry.ws.annotation.ValidateReturnedValue)",
      returning = "retVal")
  public void afterReturningAdvice(JoinPoint jp, Object retVal) {
    MethodSignature signature = (MethodSignature) jp.getSignature();
    Class<?>[] validationGroups =
        signature.getMethod().getAnnotation(ValidateReturnedValue.class).value();

    if (retVal instanceof Iterable) {
      Iterable iter = (Iterable) retVal;
      for (Object next : iter) {
        Set<ConstraintViolation<Object>> violations = validator.validate(next, validationGroups);
        if (!violations.isEmpty()) {
          throw new ConstraintViolationException(violations);
        }
      }
    } else if (retVal instanceof ResponseEntity) {
      ResponseEntity retValAsResponseEntity = (ResponseEntity) retVal;
      Set<ConstraintViolation<Object>> violations =
          validator.validate(retValAsResponseEntity.getBody(), validationGroups);
      if (!violations.isEmpty()) {
        throw new ConstraintViolationException(violations);
      }
    } else {
      Set<ConstraintViolation<Object>> violations = validator.validate(retVal, validationGroups);
      if (!violations.isEmpty()) {
        throw new ConstraintViolationException(violations);
      }
    }
  }
}
