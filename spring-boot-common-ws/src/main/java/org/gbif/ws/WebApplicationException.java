package org.gbif.ws;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * analogue of JAX-RS' one
 */
public class WebApplicationException extends RuntimeException {

  private static final long serialVersionUID = 11660101L;

  private final transient ResponseEntity response;

  /**
   * Construct a new instance with a blank message and default HTTP status code of 500
   */
  public WebApplicationException() {
    this(null, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /**
   * Construct a new instance using the supplied response
   *
   * @param response the response that will be returned to the client, a value
   *                 of null will be replaced with an internal server error response (status
   *                 code 500)
   */
  public WebApplicationException(ResponseEntity response) {
    this(null, response);
  }

  /**
   * Construct a new instance with a blank message and specified HTTP status code
   *
   * @param status the HTTP status code that will be returned to the client
   */
  public WebApplicationException(int status) {
    this(null, status);
  }

  /**
   * Construct a new instance with a blank message and specified HTTP status code
   *
   * @param status the HTTP status code that will be returned to the client
   * @throws IllegalArgumentException if status is null
   */
  public WebApplicationException(HttpStatus status) {
    this(null, status);
  }

  /**
   * Construct a new instance with a blank message and default HTTP status code of 500
   *
   * @param cause the underlying cause of the exception
   */
  public WebApplicationException(Throwable cause) {
    this(cause, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /**
   * Construct a new instance using the supplied response
   *
   * @param response the response that will be returned to the client, a value
   *                 of null will be replaced with an internal server error response (status
   *                 code 500)
   * @param cause    the underlying cause of the exception
   */
  public WebApplicationException(Throwable cause, ResponseEntity response) {
    super(cause);
    if (response == null)
      this.response = ResponseEntity.status(500).build();
    else
      this.response = response;
  }

  /**
   * Construct a new instance with a blank message and specified HTTP status code
   *
   * @param status the HTTP status code that will be returned to the client
   * @param cause  the underlying cause of the exception
   */
  public WebApplicationException(Throwable cause, int status) {
    this(cause, ResponseEntity.status(status).build());
  }

  /**
   * Construct a new instance with a blank message and specified HTTP status code
   *
   * @param status the HTTP status code that will be returned to the client
   * @param cause  the underlying cause of the exception
   * @throws IllegalArgumentException if status is null
   */
  public WebApplicationException(Throwable cause, HttpStatus status) {
    this(cause, ResponseEntity.status(status).build());
  }

  /**
   * Get the HTTP response.
   *
   * @return the HTTP response.
   */
  public ResponseEntity getResponse() {
    return response;
  }
}
