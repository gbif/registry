package org.gbif.registry.ws.advice;

import org.gbif.ws.WebApplicationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class RegistryControllerAdvice {

  @ExceptionHandler(WebApplicationException.class)
  public ResponseEntity<Void> handleWebApplicationException(final WebApplicationException e) {
    return ResponseEntity.status(e.getResponse().getStatusCode()).build();
  }
}
