package org.gbif.registry.gdpr;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Network;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.GdprNotificationMessage;
import org.gbif.registry.persistence.mapper.GdprNotificationMapper;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.common.messaging.api.messages.GdprNotificationMessage.EntityType;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Service to work with Gdpr that acts as the main point for any operation related to Gdpr.
 */
@Singleton
public class GdprService {

  private static final Logger LOG = LoggerFactory.getLogger(GdprService.class);

  @Inject(optional = true)
  private MessagePublisher messagePublisher;

  private final GdprNotificationMapper gdprNotificationMapper;
  private final GdprConfiguration config;

  @Inject
  public GdprService(GdprConfiguration config, GdprNotificationMapper gdprNotificationMapper) {
    this.config = config;
    this.gdprNotificationMapper = gdprNotificationMapper;
  }

  public boolean existsNotification(String email) {
    return existsNotification(email, config.getVersion());
  }

  public boolean existsNotification(String email, String version) {
    Objects.requireNonNull(email);
    return gdprNotificationMapper.existsNotification(email,
                                                     Strings.isNullOrEmpty(version) ? config.getVersion() : version);
  }

  public void createNotification(String email, String version, Map<EntityType, List<UUID>> context) {
    Objects.requireNonNull(email);
    gdprNotificationMapper.create(email, Strings.isNullOrEmpty(version) ? config.getVersion() : version, context);
  }

  public <T extends NetworkEntity> void checkGdprNotification(UUID uuid, Class<T> objectClass, Contact contact) {
    checkGdprNotification(uuid, getEntityType(objectClass), contact);
  }

  public void checkGdprNotification(UUID uuid, EntityType entityType, Contact contact) {
    if (!config.isGdprEnabled()) {
      LOG.info("GDPR check disabled");
      return;
    }

    checkNotNull(uuid, "Uuid cannot be null");
    checkNotNull(entityType, "Entity type cannot be null");
    checkNotNull(contact, "Contact cannot be null");

    if (contact.getEmail() == null) {
      return;
    }

    contact.getEmail().forEach(email -> {
      if (Strings.isNullOrEmpty(email)) {
        return;
      }

      if (!gdprNotificationMapper.existsNotification(email, config.getVersion())) {
        if (messagePublisher != null) {
          try {
            // create context
            Map<EntityType, List<UUID>> context = new HashMap<>();
            context.put(entityType, Collections.singletonList(uuid));
            // create message
            GdprNotificationMessage message = new GdprNotificationMessage(email, context, config.getVersion());
            // send message
            LOG.info("Sending GDPR message to queue for {} and version {}", email, config.getVersion());
            messagePublisher.send(message);
          } catch (IOException e) {
            LOG.error("Unable to send GDPR notification message to the queue for email {}", email, e);
          }
        } else {
          LOG.warn("Registry is configured to run without messaging capabilities.  Unable to send GDPR notification "
                   + "message to the queue for email {}", email);
        }
      }
    });
  }

  private <T extends NetworkEntity> EntityType getEntityType(Class<T> objectClass) {
    // check for all the entity types
    if (objectClass != null) {
      if (objectClass.isAssignableFrom(Dataset.class)) {
        return EntityType.Dataset;
      }

      if (objectClass.isAssignableFrom(Installation.class)) {
        return EntityType.Installation;
      }

      if (objectClass.isAssignableFrom(Network.class)) {
        return EntityType.Network;
      }

      if (objectClass.isAssignableFrom(Node.class)) {
        return EntityType.Node;
      }

      if (objectClass.isAssignableFrom(Organization.class)) {
        return EntityType.Organization;
      }
    }

    throw new IllegalArgumentException("Entity type not supported: " + objectClass);
  }

}
