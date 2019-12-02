package org.gbif.registry.metadata;

import org.xml.sax.SAXException;

import javax.annotation.concurrent.NotThreadSafe;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * GBIF Metadata Profile schema validator utility.
 */
@NotThreadSafe
public class EmlValidator {

  private static final String SCHEMA_LANG = "http://www.w3.org/2001/XMLSchema";

  private final Validator validator;

  /**
   * Return an instance of {@link EmlValidator} for a specific {@link EMLProfileVersion}.
   * {@link EmlValidator} instances are NOT thread safe.
   *
   * @param version
   * @return
   * @throws SAXException
   */
  public static EmlValidator newValidator(EMLProfileVersion version) throws SAXException {
    SchemaFactory factory = SchemaFactory.newInstance(SCHEMA_LANG);

    // disable external entities
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);

    Schema schema = factory.newSchema(new StreamSource(version.getSchemaLocation()));
    return new EmlValidator(schema.newValidator());
  }

  /**
   * Private constructor, use {@link #newValidator(EMLProfileVersion)}
   */
  private EmlValidator(Validator validator) {
    this.validator = validator;
  }

  /**
   * Validate a EML document provided as String.
   *
   * @param emlAsString
   * @throws InvalidEmlException
   */
  public void validate(String emlAsString) throws InvalidEmlException {
    StreamSource streamSource = toSourceStream(emlAsString);
    validate(streamSource);
  }

  /**
   * Validate a EML document provided as InputStream.
   *
   * @param inputStream
   * @throws InvalidEmlException
   */
  public void validate(InputStream inputStream) throws InvalidEmlException {
    validate(new StreamSource(inputStream));
  }

  /**
   * Validate a EML document provided as StreamSource.
   *
   * @param streamSource
   * @throws InvalidEmlException
   */
  public void validate(StreamSource streamSource) throws InvalidEmlException {
    try {
      validator.validate(streamSource);
    } catch (Exception e) {
      throw new InvalidEmlException(e);
    }
  }

  private StreamSource toSourceStream(String xmlAsString) {
    return new StreamSource(new ByteArrayInputStream(xmlAsString.getBytes(StandardCharsets.UTF_8)));
  }
}
