package org.gbif.registry.metadata;

import java.net.URL;

import org.junit.Test;

public class EmlValidatorTest {

  @Test
  public void testValidate() throws Exception {
    EmlValidator.validate(getClass().getClassLoader().getResourceAsStream("eml/sample-strict.xml"));
  }

  @Test
  public void testValidateIptEml() throws Exception {
    EmlValidator.validate(new URL("https://gbif-providertoolkit.googlecode.com/svn/trunk/gbif-ipt/src/test/resources/resources/res1/eml-1.1.xml").openStream());
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