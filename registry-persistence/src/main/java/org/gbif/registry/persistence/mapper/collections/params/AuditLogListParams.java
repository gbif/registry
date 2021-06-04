package org.gbif.registry.persistence.mapper.collections.params;

import org.gbif.api.model.collections.CollectionEntityType;

import java.util.Date;
import java.util.UUID;

import javax.annotation.Nullable;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class AuditLogListParams {
  @Nullable Long traceId;
  @Nullable CollectionEntityType collectionEntityType;
  @Nullable String subEntityType;
  @Nullable String subEntityKey;
  @Nullable String operation;
  @Nullable UUID collectionEntityKey;
  @Nullable String createdBy;
  @Nullable Date dateFrom;
  @Nullable Date dateTo;
}
