package org.gbif.registry.ws.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.gbif.registry.ws.annotation.ValidateReturnedValue;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import java.util.Set;

@Component
@Aspect
public class ValidateReturnValueAspect {

  private final Validator validator;

  public ValidateReturnValueAspect(Validator validator) {
    this.validator = validator;
  }

  @AfterReturning(pointcut = "@annotation(org.gbif.registry.ws.annotation.ValidateReturnedValue)",
      returning = "retVal")
  public void afterReturningAdvice(JoinPoint jp, Object retVal) {
    MethodSignature signature = (MethodSignature) jp.getSignature();
    Class<?>[] validationGroups = signature.getMethod().getAnnotation(ValidateReturnedValue.class).value();

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
      Set<ConstraintViolation<Object>> violations = validator.validate(retValAsResponseEntity.getBody(), validationGroups);
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
