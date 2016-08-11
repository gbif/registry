package org.gbif.registry.metadata;

import org.junit.Test;

public class EmlValidatorTest {

  @Test
  public void testValidateEmlMetadataProfileSamplesV10() throws Exception {
    EmlValidator.validate(getClass().getClassLoader().getResourceAsStream("eml-metadata-profile/sample1-v1.0.xml"), EMLProfileVersion.GBIF_1_0);
  }

  @Test
  public void testValidateEmlMetadataProfileSamplesV101() throws Exception {
    EmlValidator.validate(getClass().getClassLoader().getResourceAsStream("eml-metadata-profile/sample2-v1.0.1.xml"), EMLProfileVersion.GBIF_1_0_1);
    EmlValidator.validate(getClass().getClassLoader().getResourceAsStream("eml-metadata-profile/sample3-v1.0.1.xml"), EMLProfileVersion.GBIF_1_0_1);
  }

  @Test
  public void testValidateEmlMetadataProfileSamplesV11() throws Exception {
    EmlValidator.validate(getClass().getClassLoader().getResourceAsStream("eml-metadata-profile/sample4-v1.1.xml"), EMLProfileVersion.GBIF_1_1);
    EmlValidator.validate(getClass().getClassLoader().getResourceAsStream("eml-metadata-profile/sample7-v1.1.xml"), EMLProfileVersion.GBIF_1_1);
    EmlValidator.validate(getClass().getClassLoader().getResourceAsStream("eml-metadata-profile/sample8-v1.1.xml"), EMLProfileVersion.GBIF_1_1);
  }

  @Test(expected = InvalidEmlException.class)
  public void testValidateEmlMetadataProfileSampleV11Fails() throws Exception {
    EmlValidator.validate(getClass().getClassLoader().getResourceAsStream("eml-metadata-profile/sample5-v1.1.xml"), EMLProfileVersion.GBIF_1_1);
  }

  @Test(expected = InvalidEmlException.class)
  public void testValidateEmlMetadataProfileSampleV11Fails2() throws Exception {
    EmlValidator.validate(getClass().getClassLoader().getResourceAsStream("eml-metadata-profile/sample6-v1.1.xml"),
      EMLProfileVersion.GBIF_1_1);
  }

  @Test(expected = InvalidEmlException.class)
  public void testValidateFail() throws Exception {
    EmlValidator.validate("<eml><dataset/></eml>", EMLProfileVersion.GBIF_1_1);
  }

  @Test(expected = InvalidEmlException.class)
  public void testValidateIpt() throws Exception {
    EmlValidator.validate(getClass().getClassLoader().getResourceAsStream("eml/ipt_eml.xml"), EMLProfileVersion.GBIF_1_1);
  }
}