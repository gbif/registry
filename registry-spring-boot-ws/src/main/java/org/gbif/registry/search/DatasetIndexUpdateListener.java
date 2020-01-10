package org.gbif.registry.search;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.registry.events.ChangedComponentEvent;
import org.gbif.registry.events.CreateEvent;
import org.gbif.registry.events.DeleteEvent;
import org.gbif.registry.events.UpdateEvent;
import org.gbif.ws.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * The index updater will asynchronously keep the provided SOLR index up to date with dataset changes and those cascaded
 * from (e.g.) organization changes.
 * Depending on the provided configuration, it will synchronize the index on initialization.
 */
@SuppressWarnings("UnstableApiUsage")
@Service
public class DatasetIndexUpdateListener {

  private static final Logger LOG = LoggerFactory.getLogger(DatasetIndexUpdateListener.class);

  // Used to build a new index before consuming if required
  private final DatasetIndexService indexService;
  private final DatasetService datasetService;

  public DatasetIndexUpdateListener(DatasetIndexService indexService,
                                    DatasetService datasetService,
                                    EventBus eventBus) {
    this.indexService = indexService;
    this.datasetService = datasetService;
    eventBus.register(this);
  }

  @Subscribe
  public final <T extends NetworkEntity> void created(CreateEvent<T> event) {
    if (event.getObjectClass().equals(Dataset.class)) {
      indexService.add((Dataset)event.getNewObject());
    }
  }

  @Subscribe
  public final <T extends NetworkEntity> void updated(UpdateEvent<T> event) {
    if (event.getObjectClass().equals(Dataset.class)) {
      indexService.add((Dataset)event.getNewObject());

    } else if (event.getObjectClass().equals(Organization.class)) {
      // we only care about title & country changes
      Organization org1 = (Organization)event.getOldObject();
      Organization org2 = (Organization)event.getNewObject();
      if (!org1.getTitle().equals(org2.getTitle()) || org1.getCountry() != org2.getCountry()) {
        indexService.trigger(org2);
      }

    } else if (event.getObjectClass().equals(Installation.class)) {
      // we only care about the hosting organization
      Installation i1 = (Installation)event.getOldObject();
      Installation i2 = (Installation)event.getNewObject();
      if (!i1.getOrganizationKey().equals(i2.getOrganizationKey())) {
        indexService.trigger(i2);
      }
    }
  }

  @Subscribe
  public final <T extends NetworkEntity> void deleted(DeleteEvent<T> event) {
    if (event.getObjectClass().equals(Dataset.class)) {
      indexService.delete(event.getOldObject().getKey());
    }
  }

  @Subscribe
  public final void updatedComponent(ChangedComponentEvent event) {
    // only fire in case of tagged datasets which become keywords in solr
    if (event.getTargetClass().equals(Dataset.class) && event.getComponentClass().equals(Tag.class)) {
      // we only put tagged datasets onto the queue for this event type!
      UUID key = event.getTargetEntityKey();
      try {
        Dataset d = datasetService.get( key );
        indexService.add(d);
      } catch (NotFoundException e) {
        LOG.error("Cannot update tagged, but missing dataset {}", key);
      }
    }
  }
}
