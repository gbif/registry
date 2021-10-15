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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.annotation.concurrent.NotThreadSafe;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;

/** GBIF Metadata Profile schema validator utility. */
@NotThreadSafe
public class EmlValidator {

  private static final String SCHEMA_LANG = "http://www.w3.org/2001/XMLSchema";

  private final Validator validator;

  /**
   * Return an instance of {@link EmlValidator} for a specific {@link EMLProfileVersion}. {@link
   * EmlValidator} instances are NOT thread safe.
   *
   * @param version
   * @return
   * @throws SAXException
   */
  public static EmlValidator newValidator(EMLProfileVersion version) throws SAXException {
    SchemaFactory factory = SchemaFactory.newInstance(SCHEMA_LANG);
    Schema schema = factory.newSchema(new StreamSource(version.getSchemaLocation()));
    return new EmlValidator(schema.newValidator());
  }

  /** Private constructor, use {@link #newValidator(EMLProfileVersion)} */
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
