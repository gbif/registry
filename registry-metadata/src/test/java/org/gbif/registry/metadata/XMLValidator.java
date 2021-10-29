/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.metadata;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * XML validation helper class.
 *
 * @author cgendreau
 */
public class XMLValidator {

  private static final SchemaFactory FACTORY =
      SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

  /** Asserts that the xmlContent is valid according to the XSD at xsdLocation. */
  public static void assertXMLAgainstXSD(String xmlContent, String xsdLocation) throws IOException {
    try {
      // warning, this is slow
      Schema schema = FACTORY.newSchema(new StreamSource(xsdLocation));
      Validator validator = schema.newValidator();
      validator.validate(new StreamSource(new StringReader(xmlContent)));
    } catch (SAXException saxEx) {
      fail("XML content does not validate against XSD Schema: " + saxEx.getMessage());
    }
  }
}
