package org.gbif.registry.persistence.mapper.collections;

import org.gbif.registry.persistence.mapper.collections.dto.AuditLogDto;

import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogMapper {

  void create(AuditLogDto auditLogDto);

}
