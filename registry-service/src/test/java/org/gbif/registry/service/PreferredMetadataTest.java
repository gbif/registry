/*
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
package org.gbif.registry.service;

import org.gbif.api.model.registry.Metadata;
import org.gbif.api.vocabulary.MetadataType;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/** Covers {@link RegistryDatasetServiceImpl#preferredMetadata}. */
class PreferredMetadataTest {

  @Test
  void noDocuments() {
    assertNull(RegistryDatasetServiceImpl.preferredMetadata(Collections.emptyList()));
  }

  @Test
  void emlStoredLastWins() {
    Metadata eml = metadata(MetadataType.EML, 2);
    Metadata colDp = metadata(MetadataType.COL_DP, 1);

    assertSame(eml, RegistryDatasetServiceImpl.preferredMetadata(List.of(eml, colDp)));
  }

  @Test
  void archiveMetadataStoredAfterEmlWins() {
    Metadata eml = metadata(MetadataType.EML, 1);
    Metadata colDp = metadata(MetadataType.COL_DP, 2);
    Metadata dwcDp = metadata(MetadataType.DWC_DP, 3);

    assertSame(dwcDp, RegistryDatasetServiceImpl.preferredMetadata(List.of(eml, colDp, dwcDp)));
  }

  @Test
  void dcOnlyWhenNothingElse() {
    Metadata eml = metadata(MetadataType.EML, 1);
    Metadata dc = metadata(MetadataType.DC, 2);

    assertSame(eml, RegistryDatasetServiceImpl.preferredMetadata(List.of(eml, dc)));
    assertSame(dc, RegistryDatasetServiceImpl.preferredMetadata(List.of(dc)));
  }

  @Test
  void sameDateKeepsTypePriority() {
    Metadata eml = metadata(MetadataType.EML, 1);
    Metadata colDp = metadata(MetadataType.COL_DP, 1);

    assertSame(eml, RegistryDatasetServiceImpl.preferredMetadata(List.of(eml, colDp)));
  }

  private static Metadata metadata(MetadataType type, long created) {
    Metadata metadata = new Metadata();
    metadata.setType(type);
    metadata.setCreated(new Date(created));
    return metadata;
  }
}
