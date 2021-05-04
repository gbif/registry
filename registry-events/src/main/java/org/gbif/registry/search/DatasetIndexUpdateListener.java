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
package org.gbif.registry.search;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Network;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.registry.events.ChangedComponentEvent;
import org.gbif.registry.events.CreateEvent;
import org.gbif.registry.events.DeleteEvent;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.events.UpdateEvent;
import org.gbif.registry.search.dataset.indexing.DatasetRealtimeIndexer;
import org.gbif.ws.NotFoundException;

import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.eventbus.Subscribe;

@SuppressWarnings("UnstableApiUsage")
@Service
public class DatasetIndexUpdateListener {

  private static final Logger LOG = LoggerFactory.getLogger(DatasetIndexUpdateListener.class);

  // Used to build a new index before consuming if required
  private final DatasetRealtimeIndexer indexService;
  private final DatasetService datasetService;

  public DatasetIndexUpdateListener(
      DatasetRealtimeIndexer indexService,
      DatasetService datasetService,
      EventManager eventManager) {
    this.indexService = indexService;
    this.datasetService = datasetService;
    eventManager.register(this);
  }

  @Subscribe
  public final <T> void created(CreateEvent<T> event) {
    if (event.getObjectClass().equals(Dataset.class)) {
      indexService.index((Dataset) event.getNewObject());
    }
  }

  @Subscribe
  public final <T> void updated(UpdateEvent<T> event) {
    if (event.getObjectClass().equals(Dataset.class)) {
      indexService.index((Dataset) event.getNewObject());

    } else if (event.getObjectClass().equals(Organization.class)) {
      // we only care about title & country changes
      Organization org1 = (Organization) event.getOldObject();
      Organization org2 = (Organization) event.getNewObject();
      if (!Objects.equals(org1.getTitle(), org2.getTitle())
          || !Objects.equals(org1.getCountry(), org2.getCountry())) {
        indexService.index(org2);
      }

    } else if (event.getObjectClass().equals(Installation.class)) {
      // we only care about the hosting organization
      Installation i1 = (Installation) event.getOldObject();
      Installation i2 = (Installation) event.getNewObject();
      if (!Objects.equals(i1.getOrganizationKey(), (i2.getOrganizationKey()))
          || !Objects.equals(i1.getTitle(), i2.getTitle())) {
        indexService.index(i2);
      }
    } else if (event.getObjectClass().equals(Network.class)) {
      // we only care about title changes
      Network network1 = (Network) event.getOldObject();
      Network network2 = (Network) event.getNewObject();
      if (!Objects.equals(network1.getTitle(), network2.getTitle())) {
        indexService.index(network2);
      }
    }
  }

  @Subscribe
  public final <T> void deleted(DeleteEvent<T> event) {
    if (event.getObjectClass().equals(Dataset.class)) {
      indexService.delete((Dataset) event.getOldObject());
    } else if (event.getObjectClass().equals(Organization.class)) {
      indexService.index((Organization) event.getOldObject());
    } else if (event.getObjectClass().equals(Network.class)) {
      indexService.index((Network) event.getOldObject());
    }
  }

  @Subscribe
  public final void updatedComponent(ChangedComponentEvent event) {
    // only fire in case of tagged datasets
    if ((event.getTargetClass().equals(Dataset.class)
            && event.getComponentClass().equals(Tag.class))
        || (event.getTargetClass().equals(Network.class)
            && event.getComponentClass().equals(Dataset.class))) {
      // we only put tagged datasets onto the queue for this event type!
      UUID key = event.getTargetEntityKey();
      try {
        Dataset d = datasetService.get(key);
        indexService.index(d);
      } catch (NotFoundException e) {
        LOG.error("Cannot update tagged, but missing dataset {}", key);
      }
    }
  }
}
