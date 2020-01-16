package org.gbif.registry.collections.sync;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Tests the {@link SyncConfig}. */
public class SyncConfigTest {

  private static final String CONFIG_TEST_PATH = "sync-config.yaml";

  @Test
  public void loadConfigTest() {
    String path = getClass().getClassLoader().getResource(CONFIG_TEST_PATH).getPath();
    SyncConfig config = SyncConfig.fromFileName(path).orElse(null);

    assertNotNull(config);
    assertNotNull(config.getRegistryWsUrl());
    assertNotNull(config.getIhWsUrl());
    assertTrue(config.isSaveResultsToFile());
    assertTrue(config.isDryRun());
    assertTrue(config.isIgnoreConflicts());
    assertNotNull(config.getNotification());
  }
}
