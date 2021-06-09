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
package org.gbif.registry.events.collections;

import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.registry.domain.collections.AuditLog;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.persistence.mapper.collections.AuditLogMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.Subscribe;

import brave.Tracer;

@Component
public class AuditLogger {

  private final Tracer tracer;
  private final AuditLogMapper auditLogMapper;
  private final ObjectMapper objectMapper;

  @Autowired
  public AuditLogger(
      Tracer tracer,
      AuditLogMapper auditLogMapper,
      ObjectMapper objectMapper,
      EventManager eventManager) {
    this.tracer = tracer;
    this.auditLogMapper = auditLogMapper;
    this.objectMapper = objectMapper;
    eventManager.register(this);
  }

  @Subscribe
  public <T extends CollectionEntity> void logCreatedEvents(CreateCollectionEntityEvent<T> event) {
    AuditLog auditLog = collectionBaseEventToAuditLog(event);
    auditLog.setCollectionEntityKey(event.getNewObject().getKey());
    auditLog.setPostState(toJson(event.getNewObject()));
    auditLogMapper.create(auditLog);
  }

  @Subscribe
  public <T extends CollectionEntity> void logUpdatedEvents(UpdateCollectionEntityEvent<T> event) {
    AuditLog auditLog = collectionBaseEventToAuditLog(event);
    auditLog.setCollectionEntityKey(event.getNewObject().getKey());
    auditLog.setPreState(toJson(event.getOldObject()));
    auditLog.setPostState(toJson(event.getNewObject()));
    auditLogMapper.create(auditLog);
  }

  @Subscribe
  public <T extends CollectionEntity> void logDeletedEvents(DeleteCollectionEntityEvent<T> event) {
    AuditLog auditLog = collectionBaseEventToAuditLog(event);
    auditLog.setCollectionEntityKey(event.getOldObject().getKey());
    auditLog.setPreState(toJson(event.getOldObject()));
    auditLog.setPostState(toJson(event.getDeletedObject()));
    auditLogMapper.create(auditLog);
  }

  @Subscribe
  public <T extends CollectionEntity> void logReplacedEvents(ReplaceEntityEvent<T> event) {
    AuditLog auditLog = collectionBaseEventToAuditLog(event);
    auditLog.setCollectionEntityKey(event.getTargetEntityKey());
    auditLog.setReplacementKey(event.getReplacementKey());
    auditLogMapper.create(auditLog);
  }

  @Subscribe
  public <T extends CollectionEntity, R> void logSubEntityEvents(
      SubEntityCollectionEvent<T, R> event) {
    AuditLog auditLog = subEntityEventToAuditLog(event);
    auditLogMapper.create(auditLog);
  }

  private <T extends CollectionEntity, R> AuditLog subEntityEventToAuditLog(
      SubEntityCollectionEvent<T, R> event) {
    AuditLog auditLog = collectionBaseEventToAuditLog(event);
    auditLog.setSubEntityType(event.getSubEntityClass().getSimpleName());
    auditLog.setCollectionEntityKey(event.getCollectionEntityKey());
    auditLog.setSubEntityKey(event.getSubEntityKey());

    if (event.getEventType() == EventType.CREATE) {
      auditLog.setPostState(toJson(event.getSubEntity()));
    } else if (event.getEventType() == EventType.DELETE) {
      auditLog.setPreState(toJson(event.getSubEntity()));
    } else if (event.getEventType() == EventType.UPDATE
        || event.getEventType() == EventType.APPLY_SUGGESTION
        || event.getEventType() == EventType.DISCARD_SUGGESTION) {
      auditLog.setPreState(toJson(event.getOldSubEntity()));
      auditLog.setPostState(toJson(event.getSubEntity()));
    }

    return auditLog;
  }

  private <T extends CollectionEntity> AuditLog collectionBaseEventToAuditLog(
      CollectionsBaseEvent<T> event) {
    AuditLog auditLog = new AuditLog();
    auditLog.setTraceId(getTraceId());
    auditLog.setCollectionEntityType(event.getCollectionEntityType());
    auditLog.setOperation(event.getEventType().name());
    auditLog.setCreatedBy(getUsername());
    return auditLog;
  }

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

  private long getTraceId() {
    if (tracer.currentSpan() != null) {
      return tracer.currentSpan().context().traceId();
    }
    return tracer.newTrace().context().traceId();
  }
}
