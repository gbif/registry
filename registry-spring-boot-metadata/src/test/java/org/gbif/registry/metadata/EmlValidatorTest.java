package org.gbif.registry.metadata;

import org.junit.Test;

import javax.xml.transform.stream.StreamSource;

public class EmlValidatorTest {

  private StreamSource getEMLMetadataAsStreamSource(String filename){
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

  @Test(expected = InvalidEmlException.class)
  public void testValidateEmlMetadataProfileSampleV11Fails() throws Exception {
    EmlValidator validator = EmlValidator.newValidator(EMLProfileVersion.GBIF_1_1);
    validator.validate(getEMLMetadataAsStreamSource("eml-metadata-profile/sample5-v1.1.xml"));
  }

  @Test(expected = InvalidEmlException.class)
  public void testValidateEmlMetadataProfileSampleV11Fails2() throws Exception {
    EmlValidator validator = EmlValidator.newValidator(EMLProfileVersion.GBIF_1_1);
    validator.validate(getEMLMetadataAsStreamSource("eml-metadata-profile/sample6-v1.1.xml"));
  }

  @Test(expected = InvalidEmlException.class)
  public void testValidateFail() throws Exception {
    EmlValidator validator = EmlValidator.newValidator(EMLProfileVersion.GBIF_1_1);
    validator.validate("<eml><dataset/></eml>");
  }

  @Test(expected = InvalidEmlException.class)
  public void testValidateIpt() throws Exception {
    EmlValidator validator = EmlValidator.newValidator(EMLProfileVersion.GBIF_1_1);
    validator.validate(getEMLMetadataAsStreamSource("eml/ipt_eml.xml"));
  }

  // see https://github.com/gbif/registry/issues/26
  @Test(expected = InvalidEmlException.class)
  public void testDownloadEml() throws Exception {
    EmlValidator validator = EmlValidator.newValidator(EMLProfileVersion.GBIF_1_1);
    validator.validate(getEMLMetadataAsStreamSource("eml/download_metadata.xml"));
  }

}
