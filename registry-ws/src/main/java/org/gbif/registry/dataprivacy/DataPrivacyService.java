package org.gbif.registry.dataprivacy;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Network;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.DataPrivacyNotificationMessage;
import org.gbif.registry.persistence.mapper.DataPrivacyNotificationMapper;

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

import static org.gbif.common.messaging.api.messages.DataPrivacyNotificationMessage.EntityType;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Service to work with Data Privacy that acts as the main point for any operation related to Data Privacy.
 */
@Singleton
public class DataPrivacyService {

  private static final Logger LOG = LoggerFactory.getLogger(DataPrivacyService.class);

  @Inject(optional = true)
  private MessagePublisher messagePublisher;

  private final DataPrivacyNotificationMapper dataPrivacyNotificationMapper;
  private final DataPrivacyConfiguration config;

  @Inject
  public DataPrivacyService(
    DataPrivacyConfiguration config, DataPrivacyNotificationMapper dataPrivacyNotificationMapper
  ) {
    this.config = config;
    this.dataPrivacyNotificationMapper = dataPrivacyNotificationMapper;
  }

  public boolean existsNotification(String email) {
    return existsNotification(email, config.getVersion());
  }

  public boolean existsNotification(String email, String version) {
    Objects.requireNonNull(email);
    return dataPrivacyNotificationMapper.existsNotification(email,
                                                            Strings.isNullOrEmpty(version)
                                                              ? config.getVersion()
                                                              : version);
  }

  public void createNotification(String email, String version, Map<EntityType, List<UUID>> context) {
    Objects.requireNonNull(email);
    dataPrivacyNotificationMapper.create(email,
                                         Strings.isNullOrEmpty(version) ? config.getVersion() : version,
                                         context);
  }

  public <T extends NetworkEntity> void checkDataPrivacyNotification(UUID uuid, Class<T> objectClass, Contact contact) {
    checkDataPrivacyNotification(uuid, getEntityType(objectClass), contact);
  }

  public void checkDataPrivacyNotification(UUID uuid, EntityType entityType, Contact contact) {
    if (!config.isDataPrivacyEnabled()) {
      LOG.info("Data privacy check disabled");
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

      if (!dataPrivacyNotificationMapper.existsNotification(email, config.getVersion())) {
        if (messagePublisher != null) {
          try {
            // create context
            Map<EntityType, List<UUID>> context = new HashMap<>();
            context.put(entityType, Collections.singletonList(uuid));
            // create message
            DataPrivacyNotificationMessage message =
              new DataPrivacyNotificationMessage(email, context, config.getVersion());
            // send message
            LOG.info("Sending data privacy message to queue for {} and version {}", email, config.getVersion());
            messagePublisher.send(message);
          } catch (IOException e) {
            LOG.error("Unable to send data privacy message to the queue for email {}", email, e);
          }
        } else {
          LOG.warn("Registry is configured to run without messaging capabilities. "
                   + "Unable to send data privacy message to the queue for email {}", email);
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
