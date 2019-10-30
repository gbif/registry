package org.gbif.ws.mixin;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.TimeZone;

/**
 * Jackson {@link JsonSerializer} classes for {@link Date}s with specified formats.
 */
public class DateSerde {

  private static SimpleDateFormat noTimezoneFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

  static {
    noTimezoneFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  private DateSerde() {
  }

  /**
   * Jackson {@link JsonSerializer} for {@link Date}.
   */
  public static class NoTimezoneDateJsonSerializer extends JsonSerializer<Date> {

    @Override
    public void serialize(Date value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
      // Empty fields aren't included in the JSON.
      if (value != null) {
        jgen.writeString(noTimezoneFormat.format(value));
      }
    }
  }

  /**
   * Jackson {@link JsonDeserializer} for {@link Date}s formatted above, falling back to the Jackson way.
   */
  public static class FlexibleDateJsonDeserializer extends DateDeserializers.DateDeserializer {

    @Override
    public Date deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
      if (jp.getCurrentToken() == JsonToken.VALUE_STRING) {
        String text = jp.getText();
        if (text.length() == 19) {
          return Date.from(Instant.parse(text + "Z"));
        } else {
          return super.deserialize(jp, ctxt);
        }
      }
      throw ctxt.mappingException("Expected String");
    }
  }
}
