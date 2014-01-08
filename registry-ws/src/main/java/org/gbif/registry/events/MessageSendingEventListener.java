/*
 * Copyright 2013 Global Biodiversity Information Facility (GBIF)
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

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.common.messaging.api.Message;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.RegistryChangeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

public class MessageSendingEventListener {

  private static final Logger LOG = LoggerFactory.getLogger(MessageSendingEventListener.class);
  private final MessagePublisher messagePublisher;

  @Inject
  public MessageSendingEventListener(MessagePublisher messagePublisher, EventBus eventBus) {
    checkNotNull(messagePublisher, "messagePublisher can't be null");
    this.messagePublisher = messagePublisher;
    eventBus.register(this);
  }

  @Subscribe
  public <T extends NetworkEntity> void sendCreatedEvent(CreateEvent<T> event) {
    Message message = new RegistryChangeMessage(RegistryChangeMessage.ChangeType.CREATED,
                                                event.getObjectClass(),
                                                null,
                                                event.getNewObject());
    try {
      messagePublisher.send(message);
    } catch (IOException e) {
      LOG.warn("Failed sending RegistryChangeMessage for CreateEvent [{}]", event.getObjectClass().getSimpleName(), e);
    }
  }

  @Subscribe
  public <T extends NetworkEntity> void sendUpdatedEvent(UpdateEvent<T> event) {
    Message message = new RegistryChangeMessage(RegistryChangeMessage.ChangeType.UPDATED,
                                                event.getObjectClass(),
                                                event.getOldObject(),
                                                event.getNewObject());
    try {
      messagePublisher.send(message);
    } catch (IOException e) {
      LOG.warn("Failed sending RegistryChangeMessage for UpdateEvent [{}]", event.getObjectClass().getSimpleName(), e);
    }
  }

  @Subscribe
  public <T extends NetworkEntity> void sendDeletedEvent(DeleteEvent<T> event) {
    Message message = new RegistryChangeMessage(RegistryChangeMessage.ChangeType.DELETED,
                                                event.getObjectClass(),
                                                event.getOldObject(),
                                                null);
    try {
      messagePublisher.send(message);
    } catch (IOException e) {
      LOG.warn("Failed sending RegistryChangeMessage for DeleteEvent [{}]", event.getObjectClass().getSimpleName(), e);
    }
  }

}
