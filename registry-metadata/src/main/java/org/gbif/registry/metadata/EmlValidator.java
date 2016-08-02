package org.gbif.registry.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * GBIF Metadata Profile schema validator utility.
 * Warning: this class should only be used in unit tests, because the validator is not thread safe.
 */
public class EmlValidator {
  private static final Logger LOG = LoggerFactory.getLogger(EmlValidator.class);
  private static final String SCHEMA_LANG = "http://www.w3.org/2001/XMLSchema";
  private static Validator EML_VALIDATOR;
  private static EMLProfileVersion VERSION;

  /**
   * Validate source against latest version of GBIF Metadata Profile: 1.1.
   */
  public static void validate(String xml, EMLProfileVersion version) throws InvalidEmlException {
    validate(new StreamSource(new StringReader(xml)), version);
  }

  /**
   * Validate source, using specified version of GBIF Metadata Profile.
   */
  public static void validate(InputStream xml, EMLProfileVersion version) throws InvalidEmlException {
    validate(new StreamSource(xml), version);
  }

  /**
   * Validate source, using specified version of GBIF Metadata Profile.
   */
  public static void validate(Source source, EMLProfileVersion version) throws InvalidEmlException {
    try {
      getValidator(version).validate(source);
      LOG.debug("EML XML passed validation");
    } catch (Exception e) {
      throw new InvalidEmlException(e);
    }
  }

  /**
   * @return Validator using specified version of GBIF Metadata Profile.
   */
  private static Validator getValidator(EMLProfileVersion version) throws IOException, SAXException {
    if (EML_VALIDATOR == null || VERSION.compareTo(version) != 0) {
      LOG.info("New validator instance using version {}", version.getVersion());
      // define the type of schema - we use W3C:
      // resolve validation driver:
      SchemaFactory factory = SchemaFactory.newInstance(SCHEMA_LANG);
      // create schema by reading it from gbif online resources:
      Schema schema = factory.newSchema(new StreamSource(version.getSchemaLocation()));
      VERSION = version;
      EML_VALIDATOR = schema.newValidator();
    }
    return EML_VALIDATOR;
  }
}
