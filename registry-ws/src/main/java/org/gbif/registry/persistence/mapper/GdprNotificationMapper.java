package org.gbif.registry.persistence.mapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.ibatis.annotations.Param;

import static org.gbif.common.messaging.api.messages.GdprNotificationMessage.EntityType;

public interface GdprNotificationMapper {

  boolean existsNotification(@Param("email") String email, @Param("version") String version);

  void create(
    @Param("email") String email,
    @Param("version") String version,
    @Param("context") Map<EntityType, List<UUID>> context
  );

}
