/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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

import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class EmlValidatorTest {

  private StreamSource getEMLMetadataAsStreamSource(String filename) {
    return new StreamSource(getClass().getClassLoader().getResourceAsStream(filename));
  }

  @Test
  public void testValidateEmlMetadataProfileSamplesV10() throws Exception {
    EmlValidator validator = EmlValidator.newValidator(EMLProfileVersion.GBIF_1_0);
    validator.validate(getEMLMetadataAsStreamSource("eml-metadata-profile/sample1-v1.0.xml"));
  }

  @Test
  public void testValidateEmlMetadataProfileSamplesV101() throws Exception {
    EmlValidator validator = EmlValidator.newValidator(EMLProfileVersion.GBIF_1_0_1);
    validator.validate(getEMLMetadataAsStreamSource("eml-metadata-profile/sample2-v1.0.1.xml"));
    validator.validate(getEMLMetadataAsStreamSource("eml-metadata-profile/sample3-v1.0.1.xml"));
  }

  @Test
  public void testValidateEmlMetadataProfileSamplesV11() throws Exception {
    EmlValidator validator = EmlValidator.newValidator(EMLProfileVersion.GBIF_1_1);
    validator.validate(getEMLMetadataAsStreamSource("eml-metadata-profile/sample4-v1.1.xml"));
    validator.validate(getEMLMetadataAsStreamSource("eml-metadata-profile/sample7-v1.1.xml"));
    validator.validate(getEMLMetadataAsStreamSource("eml-metadata-profile/sample8-v1.1.xml"));
  }

  @Test
  public void testValidateEmlMetadataProfileSampleV11Fails() throws Exception {
    EmlValidator validator = EmlValidator.newValidator(EMLProfileVersion.GBIF_1_1);
    assertThrows(InvalidEmlException.class,
        () -> validator.validate(getEMLMetadataAsStreamSource("eml-metadata-profile/sample5-v1.1.xml")));
  }

  @Test
  public void testValidateEmlMetadataProfileSampleV11Fails2() throws Exception {
    EmlValidator validator = EmlValidator.newValidator(EMLProfileVersion.GBIF_1_1);
    assertThrows(InvalidEmlException.class,
        () -> validator.validate(getEMLMetadataAsStreamSource("eml-metadata-profile/sample6-v1.1.xml")));
  }

  @Test
  public void testValidateFail() throws Exception {
    EmlValidator validator = EmlValidator.newValidator(EMLProfileVersion.GBIF_1_1);
    assertThrows(InvalidEmlException.class, () -> validator.validate("<eml><dataset/></eml>"));
  }

  @Test
  public void testValidateIpt() throws Exception {
    EmlValidator validator = EmlValidator.newValidator(EMLProfileVersion.GBIF_1_1);
    assertThrows(InvalidEmlException.class,
        () -> validator.validate(getEMLMetadataAsStreamSource("eml/ipt_eml.xml")));
  }

  // see https://github.com/gbif/registry/issues/26
  @Test
  public void testDownloadEml() throws Exception {
    EmlValidator validator = EmlValidator.newValidator(EMLProfileVersion.GBIF_1_1);
    assertThrows(InvalidEmlException.class,
        () -> validator.validate(getEMLMetadataAsStreamSource("eml/download_metadata.xml")));
  }
}
