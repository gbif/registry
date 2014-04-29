package org.gbif.registry.search;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.events.ChangedComponentEvent;
import org.gbif.registry.events.CreateEvent;
import org.gbif.registry.events.DeleteEvent;
import org.gbif.registry.events.UpdateEvent;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sun.jersey.api.NotFoundException;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The index updater will asynchronously keep the provided SOLR index up to date with dataset changes and those cascaded
 * from (e.g.) organization changes.
 * Depending on the provided configuration, it will synchronize the index on initialization.
 */
@Singleton
public class DatasetIndexUpdateListener {

  private static final Logger LOG = LoggerFactory.getLogger(DatasetIndexUpdateListener.class);
  private final SolrServer solrServer;
  private final DatasetDocConverter docConverter;

  // Used to build a new index before consuming if required
  private final DatasetIndexBuilder indexBuilder;
  private final boolean performIndexSync;

  // The backlog of mutations to apply to the index
  private final BlockingQueue<Object> queue = new LinkedBlockingQueue<Object>();

  // We keep track of the queue size independently, as anything using this count outside this
  // class wants to know when the work is serviced, not just pulled from the queue. There is a
  // race condition if we simply return the queue size as the event might still be in process.
  private final AtomicInteger queuedUpdates = new AtomicInteger(0);

  // Required to determine cascading changes on the Organization titles
  private final OrganizationService organizationService;
  private final InstallationService installationService;
  private final DatasetService datasetService;

  @Inject
  public DatasetIndexUpdateListener(DatasetIndexBuilder indexBuilder,
    @Named("performIndexSync") boolean performIndexSync,
    @Named("Dataset") SolrServer solrServer,
    OrganizationService organizationService,
    InstallationService installationService,
    DatasetService datasetService,
    EventBus eventBus) {
    this.indexBuilder = indexBuilder;
    this.performIndexSync = performIndexSync;
    this.solrServer = solrServer;
    this.docConverter = new DatasetDocConverter(organizationService, installationService, datasetService);
    this.organizationService = organizationService;
    this.installationService = installationService;
    this.datasetService = datasetService;
    eventBus.register(this);
    Thread updateThread = new Thread(new Consumer());
    updateThread.start();
  }

  @Subscribe
  public final <T extends NetworkEntity> void created(CreateEvent<T> event) {
    if (event.getObjectClass().equals(Dataset.class)) {
      queuedUpdates.incrementAndGet();
      queue.add(event);
    }
  }

  @Subscribe
  public final <T extends NetworkEntity> void updated(UpdateEvent<T> event) {
    if (event.getObjectClass().equals(Dataset.class) || event.getObjectClass().equals(Organization.class)
      || event.getObjectClass().equals(Installation.class)) {
      queuedUpdates.incrementAndGet();
      queue.add(event);
    }
  }

  @Subscribe
  public final <T extends NetworkEntity> void deleted(DeleteEvent<T> event) {
    if (event.getObjectClass().equals(Dataset.class)) {
      queuedUpdates.incrementAndGet();
      queue.add(event);
    }
  }

  @Subscribe
  public final void updatedComponent(ChangedComponentEvent event) {
    // only fire in case of tagged datasets which become keywords in solr
    if (event.getTargetClass().equals(Dataset.class) && event.getComponentClass().equals(Tag.class)) {
      queuedUpdates.incrementAndGet();
      queue.add(event);
    }
  }

  /**
   * Allows an external process to observe if there are pending actions in the queue. It is only anticipated that
   * integration tests will use this method.
   *
   * @return The number of events on the backlog to process.
   */
  @VisibleForTesting
  public int queuedUpdates() {
    return Math.max(queue.size(), queuedUpdates.get()); // for safety
  }

  /**
   * The queue consumer updates the SOLR index.
   * Before subscribing to real time changes, if instructed will synchronize the SOLR cube with the database.
   */
  private class Consumer implements Runnable {

    @Override
    public void run() {
      // Rebuild the index before consuming changes unless instructed to skip
      if (performIndexSync) {
        try {
          indexBuilder.build();
        } catch (Exception e) {
          throw Throwables.propagate(e);
        }
      }

      LOG.info("Starting dataset index queue consumer.  Current queue size[{}]", queue.size());
      try {
        while (true) {
          Object event = queue.take();

          if (event.getClass().equals(CreateEvent.class)) {
            @SuppressWarnings("unchecked")
            CreateEvent<Dataset> dsEvent = (CreateEvent<Dataset>) event;
            createOrReplaceDataset(dsEvent.getNewObject());

          } else if (event.getClass().equals(UpdateEvent.class)) {
            // Handle dataset, organization and installation updates
            if (((UpdateEvent<?>) event).getObjectClass().equals(Dataset.class)) {
              @SuppressWarnings("unchecked")
              UpdateEvent<Dataset> dsEvent = (UpdateEvent<Dataset>) event;
              createOrReplaceDataset(dsEvent.getNewObject());

            } else if (((UpdateEvent<?>) event).getObjectClass().equals(Organization.class)) {

              @SuppressWarnings("unchecked")
              UpdateEvent<Organization> oEvent = (UpdateEvent<Organization>) event;
              handleOrganizationUpdate(oEvent);

            } else if (((UpdateEvent<?>) event).getObjectClass().equals(Installation.class)) {

              @SuppressWarnings("unchecked")
              UpdateEvent<Installation> iEvent = (UpdateEvent<Installation>) event;
              handleInstallationUpdate(iEvent);

            }

          } else if (event.getClass().equals(ChangedComponentEvent.class)) {
            // we only put tagged datasets onto the queue for this event type!
            UUID key = ((ChangedComponentEvent)event).getTargetEntityKey();
            try {
              Dataset d = datasetService.get( key );
              createOrReplaceDataset(d);
            } catch (NotFoundException e) {
              LOG.error("Cannot update missing dataset {}", key);
            }

          } else if (event.getClass().equals(DeleteEvent.class)) {
            @SuppressWarnings("unchecked")
            DeleteEvent<Dataset> dsEvent = (DeleteEvent<Dataset>) event;
            deleteDataset(dsEvent.getOldObject());
          }

          // and now we can safely declare update the queued event count, since it is serviced
          queuedUpdates.decrementAndGet();

        }
      } catch (InterruptedException ex) {
        LOG.warn("Received interupt request, index synchronization stopped");
      }
    }

    /**
     * If the installation has changed the host, then all hosted datasets get a new organization title.
     */
    private void handleInstallationUpdate(UpdateEvent<Installation> event) {
      if (!event.getNewObject().getOrganizationKey().equals(event.getOldObject().getOrganizationKey())) {
        PagingResponse<Dataset> results = null;
        PagingRequest page = new PagingRequest();
        do {
          results = installationService.getHostedDatasets(event.getOldObject().getKey(), page);
          if (!results.getResults().isEmpty()) {
            LOG.debug("Found page of {} datasets hosted by installation[{}]", results.getResults().size(), event
              .getOldObject().getKey());
            updateDatasets(results.getResults());
          }
          page.nextPage();
        } while (!results.isEndOfRecords());
      }
    }

    /**
     * Pages over the hosted and owned datasets for the organization and updates the relative datasets.
     */
    private void handleOrganizationUpdate(UpdateEvent<Organization> oEvent) {
      // Page over all HOSTED datasets that could be affected
      PagingResponse<Dataset> results = null;
      PagingRequest page = new PagingRequest();
      do {
        results = organizationService.hostedDatasets(oEvent.getOldObject().getKey(), page);
        if (!results.getResults().isEmpty()) {
          LOG.debug("Found page of {} datasets hosted by organization[{}]", results.getResults().size(), oEvent
            .getOldObject().getKey());
          updateDatasets(results.getResults());
        }
        page.nextPage();
      } while (!results.isEndOfRecords());

      // Page over all OWNED datasets that could be affected
      page = new PagingRequest();
      do {
        results = organizationService.ownedDatasets(oEvent.getOldObject().getKey(), page);
        if (!results.getResults().isEmpty()) {
          LOG.debug("Found page of {} datasets owned by organization[{}]", results.getResults().size(), oEvent
            .getOldObject().getKey());
          updateDatasets(results.getResults());
        }
        page.nextPage();
      } while (!results.isEndOfRecords());
    }

    private void createOrReplaceDataset(Dataset dataset) {
      if (dataset != null) {
        SolrInputDocument doc = docConverter.build(dataset);
        try {
          solrServer.add(doc);
          solrServer.commit();
        } catch (Exception e) {
          LOG.error("CRITICAL: Unable to update SOLR - index is now out of sync", e);
        }
      }
    }

    private void updateDatasets(List<Dataset> datasets) {
      LOG.debug("Batch updating {} datasets in SOLR", datasets.size());
      List<SolrInputDocument> docs = Lists.newArrayList();
      for (Dataset d : datasets) {
        docs.add(docConverter.build(d));
      }
      try {
        solrServer.add(docs);
        solrServer.commit();
      } catch (Exception e) {
        LOG.error("CRITICAL: Unable to update SOLR - index is now out of sync", e);
      }
    }

    private void deleteDataset(Dataset dataset) {
      try {
        solrServer.deleteById(String.valueOf(dataset.getKey()));
        solrServer.commit();
      } catch (Exception e) {
        LOG.error("CRITICAL: Unable to delete from SOLR - index is now out of sync", e);
      }
    }
  }
}
