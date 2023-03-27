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
package org.gbif.registry.ws.it.persistence.mapper;

import org.gbif.api.model.collections.Batch;
import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.persistence.mapper.collections.BatchMapper;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BatchMapperIT extends BaseItTest {

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule =
      new TestCaseDatabaseInitializer("collections_batch");

  private final BatchMapper batchMapper;

  @Autowired
  public BatchMapperIT(
      BatchMapper batchMapper, SimplePrincipalProvider principalProvider, EsManageServer esServer) {
    super(principalProvider, esServer);
    this.batchMapper = batchMapper;
  }

  @Test
  public void createUpdateAndGetTest() {
    Batch batch = new Batch();
    batch.setEntityType(CollectionEntityType.INSTITUTION);
    batch.setCreatedBy("test");
    batch.setOperation(Batch.Operation.CREATE);
    batchMapper.create(batch);

    assertNotNull(batch.getKey());

    Batch created = batchMapper.get(batch.getKey());
    assertNotNull(created.getCreated());
    assertNull(created.getSuccessful());
    assertEquals(CollectionEntityType.INSTITUTION, created.getEntityType());
    assertEquals(Batch.Operation.CREATE, created.getOperation());
    assertEquals("test", created.getCreatedBy());

    batch.setFileErrors(Arrays.asList("e1", "e2"));
    batch.setSuccessful(true);
    batch.setResultFilePath("/test/file.zip");
    batchMapper.update(batch);

    Batch updated = batchMapper.get(batch.getKey());
    assertNotNull(updated.getCreated());
    assertEquals(CollectionEntityType.INSTITUTION, updated.getEntityType());
    assertEquals(Batch.Operation.CREATE, updated.getOperation());
    assertEquals("test", updated.getCreatedBy());
    assertEquals("/test/file.zip", updated.getResultFilePath());
    assertTrue(batch.getSuccessful());
    assertEquals(2, updated.getFileErrors().size());
    assertTrue(updated.getFileErrors().containsAll(Arrays.asList("e1", "e2")));
  }
}
