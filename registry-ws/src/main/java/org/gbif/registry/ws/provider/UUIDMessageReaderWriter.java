package org.gbif.registry.ws.provider;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.UUID;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import com.sun.jersey.core.util.ReaderWriter;

@Provider
@Produces({MediaType.TEXT_PLAIN})
@Consumes({MediaType.TEXT_PLAIN})
public class UUIDMessageReaderWriter implements MessageBodyReader<UUID> {

  @Override
  public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return type == UUID.class;
  }

  @Override
  public UUID readFrom(
    Class<UUID> type,
    Type genericType,
    Annotation[] annotations,
    MediaType mediaType,
    MultivaluedMap<String, String> httpHeaders,
    InputStream entityStream
  ) throws IOException, WebApplicationException {
    return UUID.fromString(ReaderWriter.readFromAsString(entityStream, mediaType));
  }

}
