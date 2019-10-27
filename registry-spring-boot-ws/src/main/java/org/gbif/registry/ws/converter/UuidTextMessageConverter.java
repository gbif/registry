package org.gbif.registry.ws.converter;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.UUID;

public class UuidTextMessageConverter extends AbstractHttpMessageConverter<UUID> {

  public UuidTextMessageConverter() {
    super(MediaType.TEXT_PLAIN);
  }

  @Override
  protected boolean supports(Class<?> clazz) {
    return UUID.class.isAssignableFrom(clazz);
  }

  @Override
  protected UUID readInternal(Class<? extends UUID> clazz, HttpInputMessage inputMessage) throws IOException {
    final String requestBody = toString(inputMessage.getBody());

    return UUID.fromString(requestBody);
  }

  @Override
  protected void writeInternal(UUID uuid, HttpOutputMessage outputMessage) throws IOException {
    try (OutputStream os = outputMessage.getBody()) {
      os.write(uuid.toString().getBytes());
    }
  }

  /**
   * '\A' matches the beginning of a string (but not an internal line).
   */
  private static String toString(InputStream inputStream) {
    final Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name());
    return scanner.useDelimiter("\\A").next();
  }
}
