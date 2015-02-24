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
 * EML gbif profile schema validator utility
 */
public class EmlValidator {
  private static final Logger LOG = LoggerFactory.getLogger(EmlValidator.class);
  private static final String SCHEMA_LANG = "http://www.w3.org/2001/XMLSchema";
  private static final String EML_SCHEMA = "http://rs.gbif.org/schema/eml-gbif-profile/1.1/eml.xsd";
  private static Validator EML_VALIDATOR;

  public static void validate(String xml) throws InvalidEmlException {
    validate(new StreamSource(new StringReader(xml)));
  }

  public static void validate(InputStream xml) throws InvalidEmlException {
    validate(new StreamSource(xml));
  }

  public static void validate(Source source) throws InvalidEmlException {
    try {
      getValidator().validate(source);
      LOG.debug("EML XML passed validation");

    } catch (Exception e) {
      throw new InvalidEmlException(e);
    }
  }

  private static Validator getValidator() throws IOException, SAXException {
    if (EML_VALIDATOR == null) {
      // define the type of schema - we use W3C:
      // resolve validation driver:
      SchemaFactory factory = SchemaFactory.newInstance(SCHEMA_LANG);
      // create schema by reading it from gbif online resources:
      Schema schema = factory.newSchema(new StreamSource(EML_SCHEMA));
      EML_VALIDATOR = schema.newValidator();
    }
    return EML_VALIDATOR;
  }
}
