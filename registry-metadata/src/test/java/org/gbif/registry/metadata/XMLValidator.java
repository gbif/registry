package org.gbif.registry.metadata;

import java.io.IOException;
import java.io.StringReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;

import static org.junit.Assert.fail;

/**
 * XML validation helper class.
 *
 * @author cgendreau
 */
public class XMLValidator {

  private static final SchemaFactory FACTORY = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");

  /**
   * Asserts that the xmlContent is valid according to the XSD at xsdLocation.
   *
   * @param xmlContent
   * @param xsdLocation
   * @throws IOException
   */
  public static void assertXMLAgainstXSD(String xmlContent, String xsdLocation) throws IOException {
    try {
      //warning, this is slow
      Schema schema = FACTORY.newSchema(new StreamSource(xsdLocation));
      Validator validator = schema.newValidator();
      validator.validate(new StreamSource(new StringReader(xmlContent)));
    } catch (SAXException saxEx) {
      fail("XML content does not validate against XSD Schema: " + saxEx.getMessage());
    }
  }
}
