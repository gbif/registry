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
package org.gbif.registry.events;

import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.domain.ws.DerivedDataset;
import org.gbif.registry.events.collections.CreateCollectionEntityEvent;
import org.gbif.registry.events.collections.DeleteCollectionEntityEvent;
import org.gbif.registry.events.collections.SubEntityCollectionEvent;
import org.gbif.registry.events.collections.UpdateCollectionEntityEvent;
import org.gbif.registry.persistence.mapper.collections.dto.ChangeSuggestionDto;
import org.gbif.varnish.VarnishPurger;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.google.common.base.Joiner;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * A event bus listener that will flush varnish if registry entities like datasets and organizations
 * have been created, modified or deleted.
 *
 * <p>Varnish provides two main ways of invalidating its cache: PURGE and BAN. PURGE truly frees the
 * cache, but only works on individual resource URLs while BANs work with regular expressions and
 * can banRegex entire subresources from being served. BANs do not remove the object from the
 * varnish memory though.
 *
 * @see <h ref="https://www.varnish-software.com/static/book/Cache_invalidation.html">Varnish
 *     Book</h>
 *     <h3>Purging cascade logic</h3>
 *     A quick overview of the logic how entity changes trigger purges on other resources. In case
 *     of updates all keys used from the changed objects need be taken from both the old and new
 *     version.
 *     <h4>Node</h4>
 *     <p>Nodes are only modified in the Directory and no events are ever triggered.
 *     <ul>
 *       <li>Node detail PURGE
 *       <li>List all nodes BAN
 *     </ul>
 *     <h4>Organization</h4>
 *     <ul>
 *       <li>Organization detail PURGE
 *       <li>List all organizations BAN
 *       <li>Publisher search & suggest BAN
 *       <li>/node/{org.endorsingNodeKey}/organization
 *     </ul>
 *     <h4>Network</h4>
 *     <ul>
 *       <li>Network detail PURGE
 *       <li>List all networks BAN
 *     </ul>
 *     <h4>Installation</h4>
 *     <ul>
 *       <li>Installation detail PURGE
 *       <li>List all installations BAN
 *       <li>/node/{i.organization.endorsingNodeKey}/installation BAN
 *       <li>/organization/{UUID}/installation BAN
 *     </ul>
 *     <h4>Dataset</h4>
 *     <ul>
 *       <li>dataset/{key} PURGE
 *       <li>dataset/{d.parentKey} PURGE
 *       <li>dataset/{d.parentKey}/constituents BAN
 *       <li>dataset BAN
 *       <li>dataset/search|suggest BAN
 *       <li>/installation/{d.installationKey}/dataset BAN
 *       <li>/organization/{d.installation.organizationKey}/hostedDataset BAN
 *       <li>/organization/{d.publishingOrganizationKey}/publishedDataset BAN
 *       <li>/node/{d.publishingOrganization.endorsingNodeKey}/dataset BAN
 *       <li>/network/{any UUID}/constituents BAN
 *     </ul>
 *     <h4>DerivedDataset</h4>
 *     <ul>
 *       <li>derivedDataset/{doiPrefix}/{doiSuffix}/* BAN
 *       <li>derivedDataset/dataset/{datasetKey} BAN
 *       <li>derivedDataset/dataset/{doiPrefix}/{doiSuffix} BAN
 *       <li>derivedDataset/user/{user} BAN
 *     </ul>
 *     <h4>Institution</h4>
 *     <ul>
 *       <li>grscicoll/institution/{key} PURGE
 *       <li>grscicoll/institution/{d.parentKey} PURGE
 *       <li>grscicoll/institution BAN
 *       <li>grscicoll/institution/suggest BAN
 *       <li>grscicoll/search BAN
 *     </ul>
 *     <h4>Collection</h4>
 *     <ul>
 *       <li>grscicoll/collection/{key} PURGE
 *       <li>grscicoll/collection/{d.parentKey} PURGE
 *       <li>grscicoll/collection BAN
 *       <li>grscicoll/collection/suggest BAN
 *       <li>grscicoll/search BAN
 *     </ul>
 *     <h4>Person</h4>
 *     <ul>
 *       <li>grscicoll/person/{key} PURGE
 *       <li>grscicoll/person/{d.parentKey} PURGE
 *       <li>grscicoll/person BAN
 *       <li>grscicoll/person/suggest BAN
 *       <li>grscicoll/institution/{key}/contact
 *       <li>grscicoll/collection/{key}/contact
 *     </ul>
 *     <h4>ChangeSuggestion</h4>
 *     <ul>
 *       <li>grscicoll/{collection|institution}/changeSuggestion/{key} PURGE
 *       <li>grscicoll/{collection|institution}/changeSuggestion BAN
 *     </ul>
 *     <h3>Threading</h3>
 *     Purging is best-effort and must never hold up the request that triggered it. Handlers do not
 *     run on the posting thread: their work is queued on a bounded executor, and when the event is
 *     posted inside a transaction it is only queued once that transaction commits. This keeps
 *     remote calls (Varnish, and the lookups done to compute cascades) out of open transactions
 *     and stops one slow call from blocking every request that posts an event.
 */
public class VarnishPurgeListener {

  private static final Logger LOG = LoggerFactory.getLogger(VarnishPurgeListener.class);
  private final OrganizationService organizationService;
  private final InstallationService installationService;
  private final DatasetService datasetService;
  private final InstitutionService institutionService;
  private final CollectionService collectionService;
  private final VarnishPurger purger;
  private final ExecutorService executor;
  private static final Joiner PATH_JOINER = Joiner.on("/").skipNulls();
  private static final int MAX_QUEUED_PURGES = 10_000;

  public VarnishPurgeListener(
      CloseableHttpClient client,
      EventManager eventManager,
      URI apiBaseUrl,
      OrganizationService organizationService,
      InstallationService installationService,
      DatasetService datasetService,
      InstitutionService institutionService,
      CollectionService collectionService,
      int purgingThreads) {
    this.organizationService = organizationService;
    this.installationService = installationService;
    this.datasetService = datasetService;
    this.institutionService = institutionService;
    this.collectionService = collectionService;
    eventManager.register(this);

    purger = new VarnishPurger(client, apiBaseUrl);
    int threads = Math.max(1, purgingThreads);
    executor =
        new ThreadPoolExecutor(
            threads,
            threads,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(MAX_QUEUED_PURGES),
            new ThreadFactoryBuilder().setNameFormat("varnish-purge-%d").setDaemon(true).build());
  }

  /** Stops the purge executor; called by Spring when the context closes. */
  public void shutdown() {
    executor.shutdown();
  }

  /**
   * Runs the task on the purge executor, deferring it until after commit when called inside a
   * transaction. Tasks for a rolled-back transaction are dropped, as nothing changed.
   */
  private void submit(Runnable task) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              dispatch(task);
            }
          });
    } else {
      dispatch(task);
    }
  }

  private void dispatch(Runnable task) {
    try {
      executor.execute(
          () -> {
            try {
              task.run();
            } catch (Exception e) {
              LOG.warn("Varnish purge failed", e);
            }
          });
    } catch (RejectedExecutionException e) {
      LOG.warn("Varnish purge queue is full or shut down, dropping purge");
    }
  }

  @Subscribe
  @AllowConcurrentEvents
  public final <T> void created(CreateEvent<T> event) {
    submit(
        () -> {
          if (NetworkEntity.class.isAssignableFrom(event.getObjectClass())) {
            purgeEntityAndBanLists(
                event.getObjectClass(), ((NetworkEntity) event.getNewObject()).getKey());
          }

          if (event.getObjectClass().equals(Organization.class)) {
            cascadeOrganizationChange((Organization) event.getNewObject());
          } else if (event.getObjectClass().equals(Dataset.class)) {
            cascadeDatasetChange((Dataset) event.getNewObject());
          } else if (event.getObjectClass().equals(Installation.class)) {
            cascadeInstallationChange((Installation) event.getNewObject());
          } else if (event.getObjectClass().equals(DerivedDataset.class)) {
            cascadeDerivedDatasetChange((DerivedDataset) event.getNewObject());
          }
        });
  }

  @Subscribe
  @AllowConcurrentEvents
  public final <T> void updated(UpdateEvent<T> event) {
    submit(
        () -> {
          if (NetworkEntity.class.isAssignableFrom(event.getObjectClass())) {
            purgeEntityAndBanLists(
                event.getObjectClass(), ((NetworkEntity) event.getOldObject()).getKey());
          }

          if (event.getObjectClass().equals(Organization.class)) {
            cascadeOrganizationChange(
                (Organization) event.getOldObject(), (Organization) event.getNewObject());
          } else if (event.getObjectClass().equals(Dataset.class)) {
            cascadeDatasetChange((Dataset) event.getOldObject(), (Dataset) event.getNewObject());
          } else if (event.getObjectClass().equals(Installation.class)) {
            cascadeInstallationChange(
                (Installation) event.getOldObject(), (Installation) event.getNewObject());
          } else if (event.getObjectClass().equals(DerivedDataset.class)) {
            cascadeDerivedDatasetChange((DerivedDataset) event.getNewObject());
          }
        });
  }

  @Subscribe
  @AllowConcurrentEvents
  public final <T> void deleted(DeleteEvent<T> event) {
    submit(
        () -> {
          if (NetworkEntity.class.isAssignableFrom(event.getObjectClass())) {
            purgeEntityAndBanLists(
                event.getObjectClass(), ((NetworkEntity) event.getOldObject()).getKey());
          }

          if (event.getObjectClass().equals(Organization.class)) {
            cascadeOrganizationChange((Organization) event.getOldObject());
          } else if (event.getObjectClass().equals(Dataset.class)) {
            cascadeDatasetChange((Dataset) event.getOldObject());
          } else if (event.getObjectClass().equals(Installation.class)) {
            cascadeInstallationChange((Installation) event.getOldObject());
          } else if (event.getObjectClass().equals(DerivedDataset.class)) {
            cascadeDerivedDatasetChange((DerivedDataset) event.getOldObject());
          }
        });
  }

  @Subscribe
  @AllowConcurrentEvents
  public final <T extends CollectionEntity> void createdCollection(
      CreateCollectionEntityEvent<T> event) {
    submit(
        () -> {
          purgeEntityAndBanLists(
              path("grscicoll", event.getCollectionEntityType().name().toLowerCase()),
              event.getNewObject().getKey());
          purger.ban("grscicoll/search");
        });
  }

  @Subscribe
  @AllowConcurrentEvents
  public final <T extends CollectionEntity> void updatedCollection(
      UpdateCollectionEntityEvent<T> event) {
    submit(
        () -> {
          purgeEntityAndBanLists(
              path("grscicoll", event.getCollectionEntityType().name().toLowerCase()),
              event.getOldObject().getKey());
          purger.ban("grscicoll/search");
        });
  }

  @Subscribe
  @AllowConcurrentEvents
  public final <T extends CollectionEntity> void deletedCollection(
      DeleteCollectionEntityEvent<T> event) {
    submit(
        () -> {
          purgeEntityAndBanLists(
              path("grscicoll", event.getCollectionEntityType().name().toLowerCase()),
              event.getOldObject().getKey());
          purger.ban("grscicoll/search");
        });
  }

  @Subscribe
  @AllowConcurrentEvents
  public final <T extends CollectionEntity, R> void collectionSubEntityChange(
      SubEntityCollectionEvent<T, R> event) {
    submit(
        () -> {
          if (event.getSubEntityClass().equals(ChangeSuggestionDto.class)) {
            purgeEntityAndBanLists(
                path(
                    "grscicoll",
                    event.getCollectionEntityType().name().toLowerCase(),
                    "changeSuggestion"),
                event.getSubEntityKey());
          } else {
            purgeEntityAndBanLists(
                path("grscicoll", event.getCollectionEntityType().name().toLowerCase()),
                event.getCollectionEntityKey());
          }
          purger.ban("grscicoll/search");
        });
  }

  @Subscribe
  @AllowConcurrentEvents
  public final void componentChange(ChangedComponentEvent event) {
    submit(
        () -> {
          purgeEntityAndBanLists(event.getTargetClass(), event.getTargetEntityKey());
          // keys have not changed, only some component of the entity itself
          if (event.getTargetClass().equals(Organization.class)) {
            cascadeOrganizationChange(organizationService.get(event.getTargetEntityKey()));
          } else if (event.getTargetClass().equals(Dataset.class)) {
            cascadeDatasetChange(datasetService.get(event.getTargetEntityKey()));
          } else if (event.getTargetClass().equals(Installation.class)) {
            cascadeInstallationChange(installationService.get(event.getTargetEntityKey()));
          }
        });
  }

  // group bans by entity class to avoid too many ban rules and thus bad varnish performance
  private void cascadeDatasetChange(Dataset... datasets) {
    // UUIDHashSet ignores null values
    Set<UUID> instKeys = new UUIDHashSet();
    Set<UUID> orgKeys = new UUIDHashSet();
    Set<UUID> nodeKeys = new UUIDHashSet();
    Set<UUID> parentKeys = new UUIDHashSet();
    for (Dataset d : datasets) {
      if (!orgKeys.contains(d.getPublishingOrganizationKey())) {
        Organization o = organizationService.get(d.getPublishingOrganizationKey());
        nodeKeys.add(o.getEndorsingNodeKey());
      }
      if (!instKeys.contains(d.getInstallationKey())) {
        instKeys.add(d.getInstallationKey());
        Installation i = installationService.get(d.getInstallationKey());
        orgKeys.add(i.getOrganizationKey());
      }
      orgKeys.add(d.getPublishingOrganizationKey());
      if (d.getParentDatasetKey() != null) {
        parentKeys.add(d.getParentDatasetKey());
        purger.purge(path("dataset", d.getParentDatasetKey()));
      }
    }
    purger.ban(String.format("dataset/%s/constituents", purger.anyKey(parentKeys)));
    // /installation/{d.installationKey}/dataset BAN
    purger.ban(String.format("installation/%s/dataset", purger.anyKey(instKeys)));
    // /organization/{d.publishingOrganizationKey}/publishedDataset BAN
    // /organization/{d.installation.organizationKey}/hostedDataset BAN
    purger.ban(String.format("organization/%s/(published|hosted)Dataset", purger.anyKey(orgKeys)));
    // /node/{d.publishingOrganization.endorsingNodeKey}/dataset BAN
    purger.ban(String.format("node/%s/dataset", purger.anyKey(nodeKeys)));
    // /network/{any UUID}/constituents BAN
    purger.ban("network/.+/constituents");
  }

  private void cascadeOrganizationChange(Organization... orgs) {
    // /node/{org.endorsingNodeKey}/
    Set<UUID> nodeKeys = new UUIDHashSet();
    for (Organization o : orgs) {
      nodeKeys.add(o.getEndorsingNodeKey());
    }
    purger.ban(String.format("node/%s/organization", purger.anyKey(nodeKeys)));
  }

  private void cascadeInstallationChange(Installation... installations) {
    Set<UUID> keys = new UUIDHashSet();
    // /organization/{i.organizationKey}/installation BAN
    for (Installation i : installations) {
      keys.add(i.getOrganizationKey());
    }
    purger.ban(String.format("organization/%s/installation", purger.anyKey(keys)));

    // /node/{i.organization.endorsingNodeKey}/installation BAN
    Set<UUID> nodekeys = new UUIDHashSet();
    for (UUID orgKey : keys) {
      Organization o = organizationService.get(orgKey);
      nodekeys.add(o.getEndorsingNodeKey());
    }
    purger.ban(String.format("%node/%s/organization", purger.anyKey(nodekeys)));
  }

  private void cascadeDerivedDatasetChange(DerivedDataset derivedDataset) {
    purger.ban(
        String.format(
            "derivedDataset/%s/%s/*",
            derivedDataset.getDoi().getPrefix(), derivedDataset.getDoi().getSuffix()));
    purger.ban(String.format("derivedDataset/user/%s", derivedDataset.getCreatedBy()));
    purger.ban("derivedDataset/dataset/*");
  }

  /**
   * Removes the specific entity from varnish and bans search & list pages. This method does not
   * check which entity class was supplied, but as it is some type of NetworkEntity we deal with the
   * right urls.
   */
  private void purgeEntityAndBanLists(String rootPath, String entityPath) {

    // purge entity detail
    purger.purge(entityPath);

    // banRegex lists and searches
    purger.ban(String.format("%s(/search|/suggest)?[^/]*$", rootPath));
  }

  private void purgeEntityAndBanLists(String rootPath, UUID key) {
    purgeEntityAndBanLists(rootPath, path(rootPath, key));
  }

  private void purgeEntityAndBanLists(String rootPath, int key) {
    purgeEntityAndBanLists(rootPath, path(rootPath, key));
  }

  private void purgeEntityAndBanLists(Class cl, UUID key) {
    purgeEntityAndBanLists(path(cl.getSimpleName().toLowerCase()), key);
  }

  private static String path(Object... parts) {
    return PATH_JOINER.join(parts);
  }

  /** HashSet with an overriden add method that silently ignores null values being added. */
  public class UUIDHashSet extends HashSet<UUID> {
    @Override
    public boolean add(UUID t) {
      return t != null && super.add(t);
    }
  }
}
