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
package org.gbif.registry.ws.it;

import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.service.registry.MachineTagService;
import org.gbif.registry.test.TestDataFactory;

import java.util.List;

import static org.gbif.registry.ws.it.LenientAssert.assertLenientEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MachineTagTests {

  public static <T extends NetworkEntity> void testAddDelete(
      MachineTagService service, T entity, TestDataFactory testDataFactory) {

    // check there are none on a newly created entity
    List<MachineTag> machineTags = service.listMachineTags(entity.getKey());
    assertNotNull(
        machineTags, "Machine tag list should be empty, not null when no machine tags exist");
    assertTrue(machineTags.isEmpty(), "Machine Tag should be empty when none added");

    // test additions
    service.addMachineTag(entity.getKey(), testDataFactory.newMachineTag());
    service.addMachineTag(entity.getKey(), testDataFactory.newMachineTag());
    service.addMachineTag(entity.getKey(), testDataFactory.newMachineTag());
    machineTags = service.listMachineTags(entity.getKey());
    assertNotNull(machineTags);
    assertEquals(3, machineTags.size(), "3 machine tags have been added");

    // test deletion, ensuring correct one is deleted
    service.deleteMachineTag(entity.getKey(), machineTags.get(0).getKey());
    machineTags = service.listMachineTags(entity.getKey());
    assertNotNull(machineTags);
    assertEquals(2, machineTags.size(), "2 machine tags should remain after the deletion");
    MachineTag expected = testDataFactory.newMachineTag();
    MachineTag created = machineTags.get(0);
    assertLenientEquals("Created machine tag does not read as expected", expected, created);

    // test bulk deletion
    service.deleteMachineTags(entity.getKey(), "hit.gbif.org", "indexedRecords");
    machineTags = service.listMachineTags(entity.getKey());
    assertNotNull(machineTags);
    assertEquals(0, machineTags.size(), "0 machine tags should remain after the deletion");

    service.addMachineTag(entity.getKey(), testDataFactory.newMachineTag());
    service.addMachineTag(entity.getKey(), testDataFactory.newMachineTag());
    machineTags = service.listMachineTags(entity.getKey());
    assertNotNull(machineTags);
    assertEquals(2, machineTags.size(), "2 machine tags have been added");
    service.deleteMachineTags(entity.getKey(), "hit.gbif.org");
    machineTags = service.listMachineTags(entity.getKey());
    assertNotNull(machineTags);
    assertEquals(0, machineTags.size(), "0 machine tags should remain after the deletion");
  }
}
