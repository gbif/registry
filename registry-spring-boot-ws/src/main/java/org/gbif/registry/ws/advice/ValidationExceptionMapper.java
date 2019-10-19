package org.gbif.registry.ws.advice;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class ValidationExceptionMapper {

  private static final Logger LOG = LoggerFactory.getLogger(ValidationExceptionMapper.class);

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity toResponse(MethodArgumentNotValidException exception) {
    LOG.error(exception.getMessage(), exception);
    ImmutableList.Builder<String> b = ImmutableList.builder();
    exception.getBindingResult().getAllErrors().forEach(error -> {
      LOG.debug("Validation of [{}] failed: {}",
        ((FieldError) error).getField(), error.getDefaultMessage());
      b.add(String.format("Validation of [%s] failed: %s",
        ((FieldError) error).getField(), error.getDefaultMessage()));
    });
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
      .contentType(MediaType.TEXT_PLAIN)
      .body("<ul><li>" + Joiner.on("</li><li>").join(b.build()) + "</li></ul>");
  }
}
