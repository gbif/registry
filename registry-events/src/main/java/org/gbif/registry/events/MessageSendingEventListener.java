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
package org.gbif.registry.events;

import org.gbif.common.messaging.api.Message;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.RegistryChangeMessage;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.Subscribe;

/**
 * A Guava event bus listener that propagates messages to the postal service. This can optionally be
 * configured with an embargo period, which will apply to all messages and result in a delay before
 * sending to the postal service. This deferral happens in a separate thread pool to ensure it is
 * non blocking.
 */
@SuppressWarnings("UnstableApiUsage")
@Service
public class MessageSendingEventListener {

  private static final Logger LOG = LoggerFactory.getLogger(MessageSendingEventListener.class);
  private final MessagePublisher messagePublisher;
  private final int embargoSeconds;

  // a scheduler allowing for deferred, asynchronous operations that will not block the caller
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);

  public MessageSendingEventListener(
      @Lazy MessagePublisher messagePublisher,
      EventManager eventManager,
      @Value("${registry.postalservice.embargoSeconds:#{NULL}}") Integer durationInSeconds) {
    Preconditions.checkNotNull(messagePublisher, "messagePublisher can't be null");
    embargoSeconds = durationInSeconds == null ? 0 : durationInSeconds;
    LOG.info("Message sending configured with an embargo durations of {} seconds", embargoSeconds);
    this.messagePublisher = messagePublisher;
    eventManager.register(this);
  }

  @Subscribe
  public <T> void sendCreatedEvent(final CreateEvent<T> event) {
    final Message message =
        new RegistryChangeMessage(
            RegistryChangeMessage.ChangeType.CREATED,
            event.getObjectClass(),
            null,
            event.getNewObject());
    LOG.debug(
        "Scheduling notification of CreateEvent [{}] with an embargo durations of {} seconds",
        event.getObjectClass().getSimpleName(),
        embargoSeconds);
    scheduler.schedule(
        () -> {
          try {
            LOG.debug(
                "Broadcasting to postal service CreateEvent [{}]",
                event.getObjectClass().getSimpleName());
            messagePublisher.send(message);
          } catch (IOException e) {
            LOG.warn(
                "Failed sending RegistryChangeMessage for CreateEvent [{}]",
                event.getObjectClass().getSimpleName(),
                e);
          }
        },
        embargoSeconds,
        TimeUnit.SECONDS);
  }

  @Subscribe
  public <T> void sendUpdatedEvent(final UpdateEvent<T> event) {
    final Message message =
        new RegistryChangeMessage(
            RegistryChangeMessage.ChangeType.UPDATED,
            event.getObjectClass(),
            event.getOldObject(),
            event.getNewObject());
    LOG.debug(
        "Scheduling notification of UpdateEvent [{}] with an embargo durations of {} seconds",
        event.getObjectClass().getSimpleName(),
        embargoSeconds);

    scheduler.schedule(
        () -> {
          try {
            LOG.debug(
                "Broadcasting to postal service UpdateEvent [{}]",
                event.getObjectClass().getSimpleName());
            messagePublisher.send(message);
          } catch (IOException e) {
            LOG.warn(
                "Failed sending RegistryChangeMessage for UpdateEvent [{}]",
                event.getObjectClass().getSimpleName(),
                e);
          }
        },
        embargoSeconds,
        TimeUnit.SECONDS);
  }

  @Subscribe
  public <T> void sendDeletedEvent(final DeleteEvent<T> event) {
    final Message message =
        new RegistryChangeMessage(
            RegistryChangeMessage.ChangeType.DELETED,
            event.getObjectClass(),
            event.getOldObject(),
            null);
    LOG.debug(
        "Scheduling notification of DeleteEvent [{}] with an embargo durations of {} seconds",
        event.getObjectClass().getSimpleName(),
        embargoSeconds);

    scheduler.schedule(
        () -> {
          try {
            LOG.debug(
                "Broadcasting to postal service DeleteEvent [{}]",
                event.getObjectClass().getSimpleName());
            messagePublisher.send(message);
          } catch (IOException e) {
            LOG.warn(
                "Failed sending RegistryChangeMessage for DeleteEvent [{}]",
                event.getObjectClass().getSimpleName(),
                e);
          }
        },
        embargoSeconds,
        TimeUnit.SECONDS);
  }

  @Subscribe
  public final void updatedComponent(ChangedComponentEvent event) {
    final Message message =
        new RegistryChangeMessage(
            RegistryChangeMessage.ChangeType.UPDATE_COMPONENT,
            event.getComponentClass(),
            event.getTargetEntityKey(),
            null);
    LOG.debug(
        "Scheduling notification of UpdateEvent [{}] with an embargo durations of {} seconds",
        event.getTargetClass().getSimpleName(),
        embargoSeconds);

    scheduler.schedule(
        () -> {
          try {
            LOG.debug(
                "Broadcasting to postal service UpdateEvent [{}]",
                event.getTargetClass().getSimpleName());
            messagePublisher.send(message);
          } catch (IOException e) {
            LOG.warn(
                "Failed sending RegistryChangeMessage for UpdateEvent [{}]",
                event.getTargetClass().getSimpleName(),
                e);
          }
        },
        embargoSeconds,
        TimeUnit.SECONDS);
  }
}
