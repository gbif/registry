package org.gbif.registry.collections.sync;

import java.nio.file.Paths;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Tests the {@link SyncConfig}. */
public class SyncConfigTest {

  private static final String CONFIG_TEST_PATH = "src/test/resources/sync-config.yaml";

  @Test
  public void loadConfigTest() {
    String path = Paths.get(CONFIG_TEST_PATH).toFile().getAbsolutePath();
    SyncConfig config = SyncConfig.fromFileName(path).orElse(null);

    assertNotNull(config);
    assertNotNull(config.getRegistryWsUrl());
    assertNotNull(config.getIhWsUrl());
    assertTrue(config.isSaveResultsToFile());
    assertTrue(config.isDryRun());
    assertTrue(config.isSendNotifications());
    assertNotNull(config.getNotification());
    assertFalse(config.getNotification().getGhIssuesAssignees().isEmpty());
  }
}
