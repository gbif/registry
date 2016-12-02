package org.gbif.registry.search;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NetworkEntityService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.util.iterables.Iterables;
import org.gbif.utils.concurrent.NamedThreadFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sun.jersey.api.NotFoundException;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.registry.search.guice.RegistrySearchModule.INDEXING_THREADS_PROP;


/**
 * A service that modifies the solr index, adding, updating or removing dataset documents.
 */
@Singleton
public class DatasetIndexService implements AutoCloseable{

  private static final Logger LOG = LoggerFactory.getLogger(DatasetIndexService.class);
  private final SolrClient solrClient;
  private final DatasetService datasetService;
  private final InstallationService installationService;
  private final OrganizationService organizationService;
  private final GetCache<Installation> installationCache;
  private final GetCache<Organization> organizationCache;
  private final DatasetDocConverter docConverter = new DatasetDocConverter();
  private final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
  // Executes each submitted task (agent synchronization) using one of possibly several pooled threads
  private ThreadPoolExecutor threadPool;

  @Inject
  public DatasetIndexService(@Named("dataset") SolrClient solrClient, @Named(INDEXING_THREADS_PROP) int maxPoolSize,
                             DatasetService datasetService, InstallationService installationService, OrganizationService organizationService) {
    Preconditions.checkArgument(maxPoolSize>0 && maxPoolSize<100, "max pool size needs to be in the range of 1-100");
    this.solrClient = solrClient;
    this.datasetService = datasetService;
    this.installationService = installationService;
    this.organizationService = organizationService;
    // use a cache for repeated lookups
    this.installationCache = new GetCache<Installation>(installationService);
    this.organizationCache = new GetCache<Organization>(organizationService);

    threadPool = new ThreadPoolExecutor(1, maxPoolSize, 10, TimeUnit.SECONDS, queue, new NamedThreadFactory("dataset-index-service"));
  }

  public void add(Dataset dataset) {
    if (dataset != null) {
      threadPool.submit(new IndexDatasetJob(dataset));
    }
  }

  public void add(Collection<Dataset> datasets) {
    if (datasets != null && !datasets.isEmpty()) {
      threadPool.submit(new IndexDatasetJob(datasets));
    }
  }

  public void delete(UUID datasetKey) {
    if (datasetKey != null) {
      threadPool.submit(new DeleteDatasetJob(datasetKey));
    }
  }

  public void trigger(Organization org) {
    if (org != null) {
      threadPool.submit(new IndexOrganizationJob(org));
    }
  }

  public void trigger(Installation installation) {
    if (installation != null) {
      threadPool.submit(new IndexInstallationJob(installation));
    }
  }

  private SolrInputDocument toDoc(Dataset d) {
      // see http://dev.gbif.org/issues/browse/REG-405 which explains why we defend against NotFoundExceptions below

    Organization publisher = null;
      try {
        publisher = d.getPublishingOrganizationKey() != null ? organizationCache.get(d.getPublishingOrganizationKey()) : null;
      } catch (NotFoundException e) {
        // server side, interceptors may trigger on a @nulltoNotFoundException which we code defensively for, but smells
        LOG.warn("Service reports organization[{}] cannot be found for dataset[{}]", d.getPublishingOrganizationKey(),
            d.getKey());
      }

      Installation installation = null;
      try {
        installation = d.getInstallationKey() != null ? installationCache.get(d.getInstallationKey()) : null;
      } catch (NotFoundException e) {
        // server side, interceptors may trigger on a @nulltoNotFoundException which we code defensively for, but smells
        LOG.warn("Service reports installation[{}] cannot be found for dataset[{}]", d.getInstallationKey(), d.getKey());
      }

      Organization host = null;
      try {
        host = installation != null && installation.getOrganizationKey() != null ? organizationCache.get(installation
            .getOrganizationKey()) : null;
      } catch (NotFoundException e) {
        // server side, interceptors may trigger on a @nulltoNotFoundException which we code defensively for, but smells
        LOG.warn("Service reports organization[{}] cannot be found for installation[{}]",
            installation.getOrganizationKey(), installation.getKey());
      }

    return docConverter.build(d,
        datasetService.getMetadataDocument(d.getKey()),
        publisher,
        host
    );
  }

  private List<SolrInputDocument> toDocs(Iterable<Dataset> datasets) {
    List<SolrInputDocument> docs = Lists.newArrayList();
    for (Dataset d : datasets) {
      docs.add(toDoc(d));
    }
    return docs;
  }

  /**
   * Allows an external process to observe if there are pending actions in the queue or currently running in the executor.
   * It is only anticipated that integration tests will use this method.
   */
  public boolean isActive() {
    return !queue.isEmpty() || threadPool.getActiveCount() > 0;
  }

  @Override
  public void close() throws Exception {
    LOG.info("Shutting down index service threadpool");
    threadPool.shutdown();
  }

  private void commit() throws IOException, SolrServerException {
    // Use soft commits
    solrClient.commit(false, true, true);
  }

  private void addDocs(Collection<SolrInputDocument> docs) throws IOException, SolrServerException {
    if (docs != null && !docs.isEmpty()) {
      solrClient.add(docs);
    }
  }

  private class IndexDatasetJob implements Runnable {
    private final Collection<Dataset> datasets;

    private IndexDatasetJob(Dataset dataset) {
      this(Lists.<Dataset>newArrayList(dataset));
    }

    private IndexDatasetJob(Collection<Dataset> datasets) {
      this.datasets = datasets;
    }

    @Override
    public void run() {
      try {
        solrClient.add(toDocs(datasets));
        commit();
      } catch (Exception e) {
        LOG.error("Unable to update {} datasets - index is now out of sync", datasets.size(), e);
      }
    }
  }

  private class DeleteDatasetJob implements Runnable {
    private final UUID key;

    private DeleteDatasetJob(UUID key) {
      this.key = key;
    }
    @Override
    public void run() {
      try {
        solrClient.deleteById(key.toString());
        commit();
      } catch (Exception e) {
        LOG.error("Unable to delete dataset {} from SOLR - index is now out of sync", key, e);
      }
    }
  }

  private class IndexOrganizationJob implements Runnable {
    private final Organization organization;

    private IndexOrganizationJob(Organization organization) {
      this.organization = organization;
    }

    @Override
    public void run() {
      // first purge cache
      organizationCache.purge(organization.getKey());

      // Update published datasets for the organization
      try {
        LOG.debug("Updating published datasets for organization {}", organization.getKey());
        Iterable<Dataset> datasets = Iterables.publishedDatasets(organization.getKey(), null, organizationService);
        addDocs(toDocs(datasets));
      } catch (Exception e) {
        LOG.error("Unable to update published datasets for organization {} - index is now out of sync", organization.getKey(), e);
      }

      // Update hosted datasets for the organization
      try {
        LOG.debug("Updating hosted datasets for organization {}: {}", organization.getKey(), organization.getTitle());
        Iterable<Dataset> datasets = Iterables.hostedDatasets(organization.getKey(), null, organizationService);
        addDocs(toDocs(datasets));
        commit();
      } catch (Exception e) {
        LOG.error("Unable to update hosted datasets for organization {} - index is now out of sync", organization.getKey(), e);
      }
    }
  }

  private class IndexInstallationJob implements Runnable {
    private final Installation installation;

    private IndexInstallationJob(Installation installation) {
      this.installation = installation;
    }

    @Override
    public void run() {
      // first purge cache
      installationCache.purge(installation.getKey());

      // Update hosted datasets for the organization
      try {
        LOG.debug("Updating hosted datasets for installation {}", installation.getKey());
        Iterable<Dataset> datasets = Iterables.hostedDatasets(installation.getKey(), null, installationService);
        addDocs(toDocs(datasets));
        commit();
      } catch (Exception e) {
        LOG.error("Unable to update hosted datasets for installation {} - index is now out of sync", installation.getKey(), e);
      }
    }
  }

  /**
   * A lightweight cache to help improve performance of the builder.
   *
   * @param <T> The type of entity being wrapped
   */
  private class GetCache<T> {
    private final NetworkEntityService<T> service;
    private final LoadingCache<UUID, T> cache = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build(
            new CacheLoader<UUID, T>() {

              @Override
              public T load(UUID key) throws Exception {
                return service.get(key);
              }
            });
    public GetCache(NetworkEntityService<T> service) {
      this.service = service;
    }

    public T get(UUID key) {
      try {
        return cache.get(key);
      } catch (ExecutionException e) {
        return null;
      }
    }

    public void purge(UUID key) {
      cache.invalidate(key);
    }
  }

}
