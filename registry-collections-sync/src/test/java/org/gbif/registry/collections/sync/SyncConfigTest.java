package org.gbif.registry.collections.sync;

import java.io.IOException;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Tests the {@link SyncConfig}. */
public class SyncConfigTest {

  private static final String CONFIG_TEST_PATH = "sync-config.yaml";

  @Test
  public void loadConfigTest() throws IOException {
    String path = getClass().getClassLoader().getResource(CONFIG_TEST_PATH).getPath();
    SyncConfig config = SyncConfig.getConfig(new String[] {"--config", path}).orElse(null);

    assertNotNull(config);
    assertNotNull(config.getRegistryWsUrl());
    assertNotNull(config.getIhWsUrl());
    assertTrue(config.isSaveResultsToFile());
    assertTrue(config.isDryRun());
    assertTrue(config.isIgnoreConflicts());
  }
}
