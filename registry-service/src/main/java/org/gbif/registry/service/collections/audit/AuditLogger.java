package org.gbif.registry.service.collections.audit;

import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.registry.persistence.mapper.collections.AuditLogMapper;
import org.gbif.registry.persistence.mapper.collections.dto.AuditLogDto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import brave.Tracer;

// TODO: listerners que sean @Async?? luego poner @EnableAsync en Application
// TODO: usar mismos events que en registry-events

@Component
public class AuditLogger {

  private final Tracer tracer;
  private final AuditLogMapper auditLogMapper;
  private final ObjectMapper objectMapper;

  @Autowired
  public AuditLogger(Tracer tracer, AuditLogMapper auditLogMapper, ObjectMapper objectMapper) {
    this.tracer = tracer;
    this.auditLogMapper = auditLogMapper;
    this.objectMapper = objectMapper;
  }

  @EventListener
  public <T extends CollectionEntity> void logCreatedEvents(CreatedEntityEvent<T> event) {
    AuditLogDto dto = createBaseDto(event);
    dto.setEntityKey(event.getCreatedEntity().getKey());
    dto.setPostState(toJson(event.getCreatedEntity()));
    auditLogMapper.create(dto);
  }

  @EventListener
  public <T extends CollectionEntity> void logUpdatedEvents(UpdatedEntityEvent<T> event) {
    AuditLogDto dto = createBaseDto(event);
    dto.setEntityKey(event.getNewEntity().getKey());
    dto.setPreState(toJson(event.getOldEntity()));
    dto.setPostState(toJson(event.getNewEntity()));
    auditLogMapper.create(dto);
  }

  private AuditLogDto createBaseDto(BaseEvent event) {
    AuditLogDto dto = new AuditLogDto();
    dto.setTraceId(tracer.currentSpan().context().traceId());
    dto.setEntityType(event.getEntityType());
    dto.setCreatedBy(getUsername());
    dto.setNote(event.getNote());
    return dto;
  }

  // TODO: getusername y toJson deberian ir a una clase de utils ya q lo uso en changeSuggestion tb

  protected String getUsername() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication.getName();
  }

  protected <T extends CollectionEntity> String toJson(T entity) {
    try {
      return objectMapper.writeValueAsString(entity);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Cannot serialize entity", e);
    }
  }
}
