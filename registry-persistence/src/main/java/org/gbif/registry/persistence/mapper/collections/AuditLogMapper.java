/*
 * Copyright 2020-2021 Global Biodiversity Information Facility (GBIF)
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
package org.gbif.registry.persistence.mapper.collections;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.registry.domain.collections.AuditLog;
import org.gbif.registry.persistence.mapper.collections.params.AuditLogListParams;

import java.util.List;

import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogMapper {

  void create(AuditLog auditLog);

  List<AuditLog> list(
      @Param("params") AuditLogListParams params, @Nullable @Param("page") Pageable page);

  long count(@Param("params") AuditLogListParams params);
}
