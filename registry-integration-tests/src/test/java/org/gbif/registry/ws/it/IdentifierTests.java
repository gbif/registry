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
package org.gbif.registry.ws.it;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.service.registry.IdentifierService;
import org.gbif.api.service.registry.NetworkEntityService;
import org.gbif.registry.test.TestDataFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.gbif.registry.ws.it.LenientAssert.assertLenientEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IdentifierTests {

  public static <T extends NetworkEntity> void testAddDelete(
      IdentifierService service,
      NetworkEntityService<T> networkEntityService,
      T entity,
      TestDataFactory testDataFactory) {

    // check there are none on a newly created entity
    List<Identifier> identifiers = service.listIdentifiers(entity.getKey());
    assertNotNull(
        identifiers, "Identifier list should be empty, not null when no identifiers exist");
    assertTrue(identifiers.isEmpty(), "Identifiers should be empty when none added");

    // test additions
    Identifier identifier1 = testDataFactory.newIdentifier();
    int identifier1Key = service.addIdentifier(entity.getKey(), identifier1);
    Identifier identifier2 = testDataFactory.newIdentifier();
    int identifier2Key = service.addIdentifier(entity.getKey(), identifier2);

    identifiers = service.listIdentifiers(entity.getKey());
    assertNotNull(identifiers);
    assertEquals(2, identifiers.size(), "2 identifiers have been added");

    // ensure the search works for this test. One entity with 2 duplicate identifiers is still one
    // entity
    Identifier identifier = testDataFactory.newIdentifier();
    PagingResponse<T> entities =
        networkEntityService.listByIdentifier(
            identifier1.getType(), identifier1.getIdentifier(), new PagingRequest());
    assertEquals((Long) 1L, entities.getCount(), "Only one entity should have the identifier");
    entities =
        networkEntityService.listByIdentifier(identifier1.getIdentifier(), new PagingRequest());
    assertEquals((Long) 1L, entities.getCount(), "Only one entity should have the identifier");

    // test deletion, ensuring correct one is deleted
    service.deleteIdentifier(entity.getKey(), identifier1Key);
    identifiers = service.listIdentifiers(entity.getKey());
    assertNotNull(identifiers);
    assertEquals(1, identifiers.size(), "1 identifier should remain after the deletion");
    Identifier created = identifiers.get(0);
    assertLenientEquals("Created identifier does not read as expected", identifier2, created);
  }
}
