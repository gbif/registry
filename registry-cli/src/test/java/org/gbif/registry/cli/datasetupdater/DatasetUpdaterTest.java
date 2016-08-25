package org.gbif.registry.cli.datasetupdater;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.UUID;

import com.google.common.io.Resources;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DatasetUpdaterTest {

  /**
   * Tests keys file read successfully into list of UUID.
   */
  @Test
  public void testReadKeys() throws IOException {
    URL fileUrl = Resources.getResource("datasetupdater/datasetKeys.txt");
    DatasetUpdaterCommand command = new DatasetUpdaterCommand();
    List<UUID> keys = command.readKeys(fileUrl.getPath());
    assertEquals(1, keys.size());
  }

  /**
   * Tests keys file read successfully into list of UUID, skipping invalid line that isn't a UUID.
   */
  @Test
  public void testReadBadKeys() throws IOException {
    URL fileUrl = Resources.getResource("datasetupdater/datasetKeys-bad.txt");
    DatasetUpdaterCommand command = new DatasetUpdaterCommand();
    List<UUID> keys = command.readKeys(fileUrl.getPath());
    assertEquals(2, keys.size());
  }
}
