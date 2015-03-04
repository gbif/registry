package org.gbif.registry.metadata;

public class InvalidEmlException extends Exception {

  public InvalidEmlException() {
  }

  public InvalidEmlException(String message) {
    super(message);
  }

  public InvalidEmlException(Throwable cause) {
    super(cause);
  }

  public InvalidEmlException(String message, Throwable cause) {
    super(message, cause);
  }
}
