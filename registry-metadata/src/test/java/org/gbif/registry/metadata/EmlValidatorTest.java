package org.gbif.registry.metadata;

import org.junit.Test;

public class EmlValidatorTest {

  @Test
  public void testValidate() throws Exception {
    EmlValidator.validate(getClass().getClassLoader().getResourceAsStream("eml/sample-strict.xml"));
  }

  /**
   * Validate the xml files copied from the eml-metadata-project at
   * https://code.google.com/p/gbif-common-resources/source/browse/gbif-metadata-profile/trunk/src/test/resources/eml/
   */
  @Test
  public void testValidateEmlMetadataProfileSample() throws Exception {
    EmlValidator.validate(getClass().getClassLoader().getResourceAsStream("eml-metadata-profile/sample.xml"));
    EmlValidator.validate(getClass().getClassLoader().getResourceAsStream("eml-metadata-profile/sample2.xml"));
    // currently invalid
    // EmlValidator.validate(getClass().getClassLoader().getResourceAsStream("eml-metadata-profile/sample3.xml"));
  }

  @Test
  public void testValidateIptEml() throws Exception {
    EmlValidator.validate(getClass().getClassLoader().getResourceAsStream("eml/ipt-eml-1.1.xml"));
  }

  @Test(expected = InvalidEmlException.class)
  public void testValidateFail() throws Exception {
    EmlValidator.validate("<eml><dataset/></eml>");
  }

  @Test(expected = InvalidEmlException.class)
  public void testValidateIpt() throws Exception {
    EmlValidator.validate(getClass().getClassLoader().getResourceAsStream("eml/ipt_eml.xml"));
  }

}