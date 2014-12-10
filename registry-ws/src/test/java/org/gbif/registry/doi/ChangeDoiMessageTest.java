package org.gbif.registry.doi;

import org.gbif.api.model.common.DOI;
import org.gbif.doi.service.DoiStatus;
import org.gbif.utils.file.FileUtils;

import java.net.URI;

import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ChangeDoiMessageTest {

  @Test
  public void testXmlWithinJson() throws Exception {
    ObjectMapper mapper = new ObjectMapper();

    String xml = IOUtils.toString(FileUtils.classpathStream("metadata/datacite-example-full-v3.1.xml"), Charsets.UTF_8);
    ChangeDoiMessage msg = new ChangeDoiMessage(DoiStatus.Status.REGISTERED, new DOI("10.999/gbif"), xml, URI.create("http://www.gbif.org"));

    String json = mapper.writeValueAsString(msg);
    ChangeDoiMessage msg2 = mapper.readValue(json, ChangeDoiMessage.class);
    assertEquals(msg, msg2);
  }
}