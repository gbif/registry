package org.gbif.ws.server.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.gbif.ws.NotFoundException;
import org.springframework.stereotype.Component;

/**
 * This aspect throws a {@link NotFoundException} for every {@code null} return value of a method.
 */
@Component
@Aspect
public class NullToNotFoundAspect {

  @AfterReturning(pointcut = "@annotation(org.gbif.ws.annotation.NullToNotFound)",
    returning = "retVal")
  public void afterReturningAdvice(JoinPoint jp, Object retVal) {
    if (retVal == null) {
      throw new NotFoundException();
    }
  }
}
