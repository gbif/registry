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
