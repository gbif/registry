package org.gbif.ws.mixin;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.base.Strings;
import org.gbif.api.vocabulary.License;

import java.io.IOException;
import java.util.Optional;

/**
 * Jackson {@link JsonSerializer} and Jackson {@link JsonDeserializer} classes for {@link License}.
 */
public final class LicenseSerde {

  private LicenseSerde() {
  }

  /**
   * Jackson {@link JsonSerializer} for {@link License}.
   */
  public static class LicenseJsonSerializer extends JsonSerializer<License> {

    @Override
    public void serialize(License value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
      if (value == null) {
        jgen.writeNull();
        return;
      }

      if (value.getLicenseUrl() != null) {
        jgen.writeString(value.getLicenseUrl());
        return;
      }

      //in last resort (UNSPECIFIED,UNSUPPORTED), we use the enum name
      jgen.writeString(value.name().toLowerCase());
    }
  }

  /**
   * Jackson {@link JsonDeserializer} for {@link License}.
   * If the value is empty, License.UNSPECIFIED will be returned.
   * If the value can not be transformed into a License, License.UNSUPPORTED will be returned.
   * This deserializer a little bit lenient, names of licenses (in the License enum) also understood.
   */
  public static class LicenseJsonDeserializer extends JsonDeserializer<License> {
    @Override
    public License deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
      if (jp.getCurrentToken() == JsonToken.VALUE_STRING) {
        if (Strings.isNullOrEmpty(jp.getText())) {
          return License.UNSPECIFIED;
        }
        //first, try by url
        Optional<License> license = License.fromLicenseUrl(jp.getText());
        if (license.isPresent()) {
          return license.get();
        }
        //then, try by name
        license = License.fromString(jp.getText());
        return license.orElse(License.UNSUPPORTED);
      }
      throw ctxt.mappingException("Expected String");
    }
  }
}
