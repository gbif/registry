package org.gbif.registry.persistence.mapper.collections.dto;

import org.gbif.api.model.collections.EntityType;

import java.util.Date;
import java.util.UUID;

public class AuditLogDto {

  private long key;
  private long traceId;
  private EntityType entityType;
  private UUID entityKey;
  private Date created;
  private String createdBy;
  private String preState;
  private String postState;
  private String note;

  public long getKey() {
    return key;
  }

  public void setKey(long key) {
    this.key = key;
  }

  public long getTraceId() {
    return traceId;
  }

  public void setTraceId(long traceId) {
    this.traceId = traceId;
  }

  public EntityType getEntityType() {
    return entityType;
  }

  public void setEntityType(EntityType entityType) {
    this.entityType = entityType;
  }

  public UUID getEntityKey() {
    return entityKey;
  }

  public void setEntityKey(UUID entityKey) {
    this.entityKey = entityKey;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public String getPreState() {
    return preState;
  }

  public void setPreState(String preState) {
    this.preState = preState;
  }

  public String getPostState() {
    return postState;
  }

  public void setPostState(String postState) {
    this.postState = postState;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }
}
