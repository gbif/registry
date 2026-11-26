/*
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
package org.gbif.registry.service.privatesector;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.Organization;
import org.gbif.registry.events.ChangedComponentEvent;
import org.gbif.registry.events.CreateEvent;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.events.UpdateEvent;

import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.google.common.eventbus.Subscribe;

import lombok.extern.slf4j.Slf4j;

/**
 * Triggers {@link PrivateSectorCategoryService} when a dataset is created, changes publisher or
 * category, or when an organization's machine tags change.
 *
 * <p>The event bus is synchronous, so the sync is deferred until the caller's transaction has
 * committed: it then sees the committed state (e.g. a deleted tag) and a failure can't roll back
 * or fail the user's request. Failures are only logged; the full sync run on every DatasetCategory
 * release, or the admin endpoint, repairs them.
 */
@Slf4j
@Component
public class PrivateSectorCategoryListener {

  private final PrivateSectorCategoryService service;

  @Autowired
  public PrivateSectorCategoryListener(
      PrivateSectorCategoryService service, EventManager eventManager) {
    this.service = service;
    eventManager.register(this);
  }

  @Subscribe
  public <T> void created(CreateEvent<T> event) {
    if (Dataset.class.isAssignableFrom(event.getObjectClass())) {
      UUID key = ((Dataset) event.getNewObject()).getKey();
      if (key != null) {
        afterCommit("dataset " + key, () -> service.syncDataset(key));
      }
    }
  }

  @Subscribe
  public <T> void updated(UpdateEvent<T> event) {
    if (Dataset.class.isAssignableFrom(event.getObjectClass())) {
      Dataset oldDataset = (Dataset) event.getOldObject();
      Dataset newDataset = (Dataset) event.getNewObject();
      if (newDataset == null || newDataset.getKey() == null) {
        return;
      }
      boolean publisherChanged =
          oldDataset == null
              || !Objects.equals(
                  oldDataset.getPublishingOrganizationKey(),
                  newDataset.getPublishingOrganizationKey());
      boolean categoryChanged =
          oldDataset == null
              || !Objects.equals(oldDataset.getCategory(), newDataset.getCategory());
      if (publisherChanged || categoryChanged) {
        UUID key = newDataset.getKey();
        afterCommit("dataset " + key, () -> service.syncDataset(key));
      }
    }
  }

  @Subscribe
  public void componentChanged(ChangedComponentEvent event) {
    // the event doesn't say which tag changed, so any machine tag change re-syncs the publisher
    if (Organization.class.equals(event.getTargetClass())
        && MachineTag.class.equals(event.getComponentClass())
        && event.getTargetEntityKey() != null) {
      UUID key = event.getTargetEntityKey();
      afterCommit("organization " + key, () -> service.syncOrganization(key));
    }
  }

  private void afterCommit(String target, Runnable sync) {
    Runnable safeSync =
        () -> {
          try {
            sync.run();
          } catch (Exception e) {
            log.error("Private sector category sync failed for {}", target, e);
          }
        };

    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              safeSync.run();
            }
          });
    } else {
      safeSync.run();
    }
  }
}
