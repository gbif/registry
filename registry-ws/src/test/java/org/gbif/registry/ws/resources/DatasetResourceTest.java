package org.gbif.registry.ws.resources;

import java.net.URI;
import java.util.UUID;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DatasetResourceTest {

  @Test
  public void testDatasetPortalUrl() throws Exception {
    UUID test = UUID.randomUUID();
    assertEquals(URI.create("http://www.gbif.org/dataset/"+test), DatasetResource.datasetPortalUrl(test));
  }
}