package org.gbif.registry.ws.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.gbif.common.messaging.api.messages.GdprNotificationMessage.EntityType;

/**
 * Models a Gdpr Notification.
 */
public class GdprNotification {

  private String email;
  private String version;
  private Map<EntityType, List<UUID>> context;

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public Map<EntityType, List<UUID>> getContext() {
    return context;
  }

  public void setContext(Map<EntityType, List<UUID>> context) {
    this.context = context;
  }

}
