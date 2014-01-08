package org.gbif.registry.metadata;

import org.gbif.api.model.registry.Dataset;
import org.gbif.registry.metadata.parse.DatasetParser;
import org.gbif.utils.file.FileUtils;

import java.io.StringWriter;
import java.util.UUID;

import org.junit.Test;

public class EMLWriterTest {

  @Test
  public void testWrite() throws Exception {
    Dataset d = DatasetParser.build(FileUtils.classpathStream("eml/sample.xml"));
    d.setKey(UUID.randomUUID());
    StringWriter writer = new StringWriter();
    EMLWriter.write(d, writer);
  }

  @Test
  public void testWriteNullContact() throws Exception {
    Dataset d = DatasetParser.build(FileUtils.classpathStream("eml/sample.xml"));
    d.setKey(UUID.randomUUID());
    d.getContacts().clear();
    StringWriter writer = new StringWriter();
    EMLWriter.write(d, writer);
  }

  @Test
  public void testWriteDC() throws Exception {
    Dataset d = DatasetParser.build(FileUtils.classpathStream("dc/worms_dc.xml"));
    d.setKey(UUID.randomUUID());
    StringWriter writer = new StringWriter();
    EMLWriter.write(d, writer);
  }
}