package org.gbif.registry.events.collections;

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

// TODO: listeners que sean @Async?? luego poner @EnableAsync en Application

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
  public <T extends CollectionEntity> void logCreatedEvents(CreateCollectionEntityEvent<T> event) {
    AuditLogDto dto = collectionBaseEventToDto(event);
    dto.setCollectionEntityKey(event.getNewObject().getKey());
    dto.setPostState(toJson(event.getNewObject()));
    auditLogMapper.create(dto);
  }

  @EventListener
  public <T extends CollectionEntity> void logUpdatedEvents(UpdateCollectionEntityEvent<T> event) {
    AuditLogDto dto = collectionBaseEventToDto(event);
    dto.setCollectionEntityKey(event.getNewObject().getKey());
    dto.setPreState(toJson(event.getOldObject()));
    dto.setPostState(toJson(event.getNewObject()));
    auditLogMapper.create(dto);
  }

  @EventListener
  public <T extends CollectionEntity> void logDeletedEvents(DeleteCollectionEntityEvent<T> event) {
    AuditLogDto dto = collectionBaseEventToDto(event);
    dto.setCollectionEntityKey(event.getOldObject().getKey());
    dto.setPreState(toJson(event.getOldObject()));
    dto.setPostState(toJson(event.getDeletedObject()));
    auditLogMapper.create(dto);
  }

  @EventListener
  public <T extends CollectionEntity> void logReplacedEvents(ReplaceEntityEvent<T> event) {
    AuditLogDto dto = collectionBaseEventToDto(event);
    dto.setCollectionEntityKey(event.getTargetEntityKey());
    dto.setReplacementKey(event.getReplacementKey());
    auditLogMapper.create(dto);
  }

  @EventListener
  public <T extends CollectionEntity, R> void logSubEntityEvents(
      SubEntityCollectionEvent<T, R> event) {
    AuditLogDto dto = subEntityToDto(event);
    auditLogMapper.create(dto);
  }

  private <T extends CollectionEntity, R> AuditLogDto subEntityToDto(
      SubEntityCollectionEvent<T, R> event) {
    AuditLogDto dto = collectionBaseEventToDto(event);
    dto.setSubEntityType(event.getSubEntity().getClass().getSimpleName());
    dto.setCollectionEntityKey(event.getCollectionEntityKey());
    dto.setSubEntityKey(event.getSubEntityKey());

    if (event.getEventType() == EventType.CREATE) {
      dto.setPostState(toJson(event.getSubEntity()));
    } else if (event.getEventType() == EventType.DELETE) {
      dto.setPreState(toJson(event.getSubEntity()));
    }

    return dto;
  }

  private <T extends CollectionEntity> AuditLogDto collectionBaseEventToDto(
      CollectionsBaseEvent<T> event) {
    AuditLogDto dto = new AuditLogDto();
    dto.setTraceId(tracer.currentSpan().context().traceId());
    dto.setCollectionEntityType(event.getCollectionEntityType());
    dto.setOperation(event.getEventType().name());
    dto.setCreatedBy(getUsername());
    return dto;
  }

  // TODO: getusername y toJson deberian ir a una clase de utils ya q lo uso en changeSuggestion tb

  protected String getUsername() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication.getName();
  }

  protected String toJson(Object entity) {
    try {
      return objectMapper.writeValueAsString(entity);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Cannot serialize entity", e);
    }
  }
}
