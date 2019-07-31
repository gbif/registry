package org.gbif.registry.ws.resources;

import org.gbif.registry.ws.security.WebApplicationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

// TODO: 2019-07-30 rename and move
@ControllerAdvice
public class RegistryControllerAdvice {

  @ExceptionHandler(WebApplicationException.class)
  public ResponseEntity<Void> handleWebApplicationException(final WebApplicationException e) {
    return ResponseEntity.status(e.getResponse().getStatusCode()).build();
  }
}
