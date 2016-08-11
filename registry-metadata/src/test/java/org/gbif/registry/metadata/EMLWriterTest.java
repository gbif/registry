package org.gbif.registry.metadata;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.registry.metadata.parse.DatasetParser;
import org.gbif.utils.file.FileUtils;

import java.io.StringWriter;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EMLWriterTest {

  private static final String XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";

  EMLWriter emlWriter = EMLWriter.newInstance();
  EMLWriter emlWriterDOI = EMLWriter.newInstance(true);

  @Test
  public void testWrite() throws Exception {
    Dataset d = DatasetParser.build(FileUtils.classpathStream("eml-metadata-profile/sample2-v1.0.1.xml"));
    d.setKey(UUID.randomUUID());
    StringWriter writer = new StringWriter();
    emlWriter.writeTo(d, writer);
    assertTrue(StringUtils.startsWith(writer.toString().trim(), XML_DECLARATION));
  }

  @Test
  public void testWriteOmitXmlDeclaration() throws Exception {
    EMLWriter emlWriter = EMLWriter.newInstance(false, true);
    Dataset d = DatasetParser.build(FileUtils.classpathStream("eml-metadata-profile/sample2-v1.0.1.xml"));
    d.setKey(UUID.randomUUID());
    StringWriter writer = new StringWriter();
    emlWriter.writeTo(d, writer);
    assertFalse(StringUtils.startsWith(writer.toString().trim(), XML_DECLARATION));
  }

  @Test
  public void testWriteNullContact() throws Exception {
    Dataset d = DatasetParser.build(FileUtils.classpathStream("eml-metadata-profile/sample2-v1.0.1.xml"));
    d.setKey(UUID.randomUUID());
    d.getContacts().clear();
    StringWriter writer = new StringWriter();
    emlWriter.writeTo(d, writer);
  }

  @Test
  public void testNullAddress() throws Exception {
    Dataset d = DatasetParser.build(FileUtils.classpathStream("eml-metadata-profile/sample2-v1.0.1.xml"));
    d.setKey(UUID.randomUUID());
    Contact c = d.getContacts().get(0);
    c.getAddress().add(null);
    c.getAddress().add(null);
    StringWriter writer = new StringWriter();
    emlWriter.writeTo(d, writer);
  }

  @Test
  public void testWriteDC() throws Exception {
    Dataset d = DatasetParser.build(FileUtils.classpathStream("dc/worms_dc.xml"));
    d.setKey(UUID.randomUUID());
    StringWriter writer = new StringWriter();
    emlWriter.writeTo(d, writer);
  }

  @Test
  public void testWriteDoiAsPrimaryId() throws Exception {
    Dataset d = DatasetParser.build(FileUtils.classpathStream("dc/worms_dc.xml"));
    d.setKey(UUID.randomUUID());
    d.setDoi(new DOI("10.1234/5679"));
    StringWriter writer = new StringWriter();
    emlWriterDOI.writeTo(d, writer);
    assertTrue(writer.toString().contains("packageId=\"10.1234/5679\""));
  }
}
