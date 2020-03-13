/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.cli.datasetupdater;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.google.common.io.Resources;

import static org.junit.Assert.assertEquals;

public class DatasetUpdaterTest {

  /** Tests keys file read successfully into list of UUID. */
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
