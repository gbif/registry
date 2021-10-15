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
package org.gbif.registry.ws.resources.collections;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.registry.domain.collections.AuditLog;
import org.gbif.registry.persistence.mapper.collections.AuditLogMapper;
import org.gbif.registry.persistence.mapper.collections.params.AuditLogListParams;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.gbif.registry.security.UserRoles.GRSCICOLL_ADMIN_ROLE;

/**
 * Class that acts both as the WS endpoint for {@link Collection} entities and also provides an
 * implementation of {@link CollectionService}.
 */
@RestController
@RequestMapping(value = "grscicoll/auditLog", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuditLogResource {

  private final AuditLogMapper auditLogMapper;

  public AuditLogResource(AuditLogMapper auditLogMapper) {
    this.auditLogMapper = auditLogMapper;
  }

  @Secured(GRSCICOLL_ADMIN_ROLE)
  @GetMapping()
  public PagingResponse<AuditLog> getAuditLogs(
      @RequestParam(name = "traceId", required = false) Long traceId,
      @RequestParam(name = "collectionEntityType", required = false)
          CollectionEntityType collectionEntityType,
      @RequestParam(name = "subEntityType", required = false) String subEntityType,
      @RequestParam(name = "operation", required = false) String operation,
      @RequestParam(name = "collectionEntityKey", required = false) UUID collectionEntityKey,
      @RequestParam(name = "createdBy", required = false) String createdBy,
      @RequestParam(name = "dateFrom", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          Date dateFrom,
      @RequestParam(name = "dateTo", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          Date dateTo,
      Pageable page) {
    page = page != null ? page : new PagingRequest();

    AuditLogListParams params =
        AuditLogListParams.builder()
            .traceId(traceId)
            .collectionEntityType(collectionEntityType)
            .subEntityType(subEntityType)
            .operation(operation)
            .collectionEntityKey(collectionEntityKey)
            .createdBy(createdBy)
            .dateFrom(dateFrom)
            .dateTo(dateTo)
            .build();

    List<AuditLog> dtos = auditLogMapper.list(params, page);
    long count = auditLogMapper.count(params);
    return new PagingResponse<>(page, count, dtos);
  }
}
