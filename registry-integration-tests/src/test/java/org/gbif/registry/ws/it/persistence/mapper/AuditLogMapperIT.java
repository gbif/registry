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

import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.domain.collections.AuditLog;
import org.gbif.registry.persistence.mapper.collections.AuditLogMapper;
import org.gbif.registry.persistence.mapper.collections.params.AuditLogListParams;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuditLogMapperIT extends BaseItTest {

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule =
      new TestCaseDatabaseInitializer(CONTAINER, "grscicoll_audit_log");

  private final AuditLogMapper auditLogMapper;

  @Autowired
  public AuditLogMapperIT(
      AuditLogMapper auditLogMapper,
      SimplePrincipalProvider principalProvider,
      EsManageServer esServer) {
    super(principalProvider, esServer);
    this.auditLogMapper = auditLogMapper;
  }

  @Test
  public void createAndListTest() {
    assertTrue(
        auditLogMapper.list(AuditLogListParams.builder().build(), new PagingRequest()).isEmpty());

    AuditLog dto = new AuditLog();
    dto.setCollectionEntityKey(UUID.randomUUID());
    dto.setCollectionEntityType(CollectionEntityType.COLLECTION);
    dto.setSubEntityType("Identifier");
    dto.setSubEntityKey("23543");
    dto.setOperation("update");
    dto.setCreatedBy("test");
    auditLogMapper.create(dto);

    dto = new AuditLog();
    dto.setCollectionEntityKey(UUID.randomUUID());
    dto.setCollectionEntityType(CollectionEntityType.INSTITUTION);
    dto.setSubEntityType("Machine Tag");
    dto.setSubEntityKey("1111");
    dto.setOperation("create");
    dto.setCreatedBy("test2");

    auditLogMapper.create(dto);

    assertEquals(
        2, auditLogMapper.list(AuditLogListParams.builder().build(), new PagingRequest()).size());
    assertEquals(
        1,
        auditLogMapper
            .list(
                AuditLogListParams.builder()
                    .collectionEntityType(CollectionEntityType.INSTITUTION)
                    .build(),
                new PagingRequest())
            .size());
    assertEquals(
        1,
        auditLogMapper
            .list(AuditLogListParams.builder().operation("create").build(), new PagingRequest())
            .size());
    assertEquals(
        1,
        auditLogMapper
            .list(AuditLogListParams.builder().createdBy("test").build(), new PagingRequest())
            .size());
    assertEquals(
        0,
        auditLogMapper
            .list(
                AuditLogListParams.builder()
                    .collectionEntityKey(dto.getCollectionEntityKey())
                    .createdBy("test")
                    .build(),
                new PagingRequest())
            .size());
    assertEquals(
        2,
        auditLogMapper
            .list(AuditLogListParams.builder().dateTo(new Date()).build(), new PagingRequest())
            .size());
    assertEquals(
        0,
        auditLogMapper
            .list(AuditLogListParams.builder().dateFrom(new Date()).build(), new PagingRequest())
            .size());
  }
}
