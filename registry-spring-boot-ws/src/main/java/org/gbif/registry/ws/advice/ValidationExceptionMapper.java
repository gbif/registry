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

import java.util.Comparator;

@ControllerAdvice
public class ValidationExceptionMapper {

  private static final Logger LOG = LoggerFactory.getLogger(ValidationExceptionMapper.class);

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity toResponse(MethodArgumentNotValidException exception) {
    LOG.error(exception.getMessage(), exception);
    ImmutableList.Builder<String> builder = ImmutableList.builder();

    exception.getBindingResult()
      .getAllErrors()
      .stream()
      .map(error -> ((FieldError) error))
      .sorted(Comparator.comparing(FieldError::getField, Comparator.naturalOrder()))
      .forEach(error -> {
        LOG.debug("Validation of [{}] failed: {}", error.getField(), error.getDefaultMessage());

        builder.add(String.format("Validation of [%s] failed: %s", error.getField(), error.getDefaultMessage()));
      });

    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
      .contentType(MediaType.TEXT_PLAIN)
      .body("<ul><li>" + Joiner.on("</li><li>").join(builder.build()) + "</li></ul>");
  }
}
