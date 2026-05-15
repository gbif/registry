package org.gbif.registry.ws.exception;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Global handler for exceptions propagated from async controller methods returning CompletableFuture.
 */
@ControllerAdvice
public class AsyncExceptionHandler {

  private final MeterRegistry meterRegistry;

  public AsyncExceptionHandler(@Autowired(required = false) MeterRegistry meterRegistry) {
    // fall back to a simple noop registry when none is provided to avoid failing bean creation
    this.meterRegistry = meterRegistry != null ? meterRegistry : new SimpleMeterRegistry();
  }

  @ExceptionHandler(CompletionException.class)
  public ResponseEntity<String> handleCompletionException(CompletionException ex) {
    Throwable cause = ex.getCause();
    String causeClass = cause == null ? "unknown" : cause.getClass().getSimpleName();
    if (cause instanceof IllegalArgumentException) {
      meterRegistry.counter("registry.async.exceptions", "type", "completion", "cause", causeClass, "status", "400").increment();
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(cause.getMessage());
    }
    meterRegistry.counter("registry.async.exceptions", "type", "completion", "cause", causeClass, "status", "503").increment();
    String message = cause == null ? ex.getMessage() : cause.getMessage();
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(message);
  }

  @ExceptionHandler(ExecutionException.class)
  public ResponseEntity<String> handleExecutionException(ExecutionException ex) {
    Throwable cause = ex.getCause();
    String causeClass = cause == null ? "unknown" : cause.getClass().getSimpleName();
    if (cause instanceof IllegalArgumentException) {
      meterRegistry.counter("registry.async.exceptions", "type", "execution", "cause", causeClass, "status", "400").increment();
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(cause.getMessage());
    }
    meterRegistry.counter("registry.async.exceptions", "type", "execution", "cause", causeClass, "status", "503").increment();
    String message = cause == null ? ex.getMessage() : cause.getMessage();
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(message);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
    meterRegistry.counter("registry.async.exceptions", "type", "illegal_argument", "cause", ex.getClass().getSimpleName(), "status", "400").increment();
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
  }
}

