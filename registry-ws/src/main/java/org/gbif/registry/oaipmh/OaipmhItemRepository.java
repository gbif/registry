package org.gbif.registry.oaipmh;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.ibatis.annotations.Param;
import org.dspace.xoai.dataprovider.exceptions.IdDoesNotExistException;
import org.dspace.xoai.dataprovider.exceptions.OAIException;
import org.dspace.xoai.dataprovider.filter.ScopedFilter;
import org.dspace.xoai.dataprovider.handlers.results.ListItemIdentifiersResult;
import org.dspace.xoai.dataprovider.handlers.results.ListItemsResults;
import org.dspace.xoai.dataprovider.model.Item;
import org.dspace.xoai.dataprovider.model.ItemIdentifier;
import org.dspace.xoai.dataprovider.model.Set;
import org.dspace.xoai.dataprovider.repository.ItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gbif.api.exception.ServiceUnavailableException;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.registry.metadata.DublinCoreWriter;
import org.gbif.registry.metadata.EMLWriter;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.InstallationMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.ws.resources.DatasetResource;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.inject.Singleton;

/**
 *
 */
@Singleton
public class OaipmhItemRepository implements ItemRepository {

  private static final Logger LOG = LoggerFactory.getLogger(OaipmhItemRepository.class);

  private final LoadingCache<UUID, Organization> ORGANIZATION_CACHE =
          CacheBuilder.newBuilder()
                  .maximumSize(1000)
                  .expireAfterAccess(1, TimeUnit.MINUTES)
                  .build(buildOrganizationCacheLoader());

  // TODO review if we should still use DatasetResource and DatasetMapper
  private final DatasetResource datasetResource;
  private final OrganizationMapper organizationMapper;
  private final DatasetMapper datasetMapper;

  @Inject
  public OaipmhItemRepository(DatasetResource datasetResource, DatasetMapper datasetMapper, OrganizationMapper organizationMapper ) {
    this.datasetResource = datasetResource;
    this.datasetMapper = datasetMapper;
    this.organizationMapper = organizationMapper;
  }

  /**
   * Build a CacheLoader<UUID, Organization> around the organizationMapper instance.
   *
   * @return
   */
  private CacheLoader<UUID, Organization> buildOrganizationCacheLoader(){
    return new CacheLoader<UUID, Organization>(){
      @Override
      public Organization load(UUID key) throws Exception {
        return organizationMapper.get(key);
      }
    };
  }

  @Override
  public Item getItem(String s) throws IdDoesNotExistException, OAIException {

    Dataset dataset = null;

    // the fully augmented dataset
    try {
      dataset = datasetResource.get(UUID.fromString(s));
    }
    catch (IllegalArgumentException ignoreEx){}

    if (dataset != null) {
      try {
        return toOaipmhItem(dataset, getSets(dataset));
      } catch (Exception e) {
        throw new ServiceUnavailableException("Failed to serialize dataset " + s + " to DC/EML", e);
      }
    }
    throw new IdDoesNotExistException();
  }

  @Override
  public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> list, int offset, int length) throws OAIException {
    return getItemIdentifiers(list, offset, length, null, null, null);
  }

  @Override
  public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> list, int offset, int length, Date from) throws OAIException {
    return getItemIdentifiers(list, offset, length, null, from, null);
  }

  @Override
  public ListItemIdentifiersResult getItemIdentifiersUntil(List<ScopedFilter> list, int offset, int length, Date until) throws OAIException {
    return getItemIdentifiers(list, offset, length, null, null, until);
  }

  @Override
  public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> list, int offset, int length, Date from, Date until) throws OAIException {
    return getItemIdentifiers(list, offset, length, null, from, until);
  }

  @Override
  public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> list, int offset, int length, String set) throws OAIException {
    return getItemIdentifiers(list, offset, length, set, null, null);
  }

  @Override
  public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> list, int offset, int length, String set, Date from) throws OAIException {
    return getItemIdentifiers(list, offset, length, set, from, null);
  }

  @Override
  public ListItemIdentifiersResult getItemIdentifiersUntil(List<ScopedFilter> list, int offset, int length, String set, Date until) throws OAIException {
    return getItemIdentifiers(list, offset, length, set, null, until);
  }

  @Override
  public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> list, int offset, int length, String set, Date from, Date until) throws OAIException {
    List<Dataset> datasetList = getDatasetListFromFilters(offset, length, set, from, until);
    List<ItemIdentifier> results = Lists.newArrayListWithCapacity(datasetList.size());

    for (Dataset dataset : datasetList) {
      results.add(new OaipmhItem(dataset, getSets(dataset)));
    }

    //FIXME asMoreResults is not set properly
    return new ListItemIdentifiersResult(true, results);
  }

  @Override
  public ListItemsResults getItems(List<ScopedFilter> list, int offset, int length) throws OAIException {
    return getItems(list, offset, length, null, null, null);
  }

  @Override
  public ListItemsResults getItems(List<ScopedFilter> list, int offset, int length, Date from) throws OAIException {
    return getItems(list, offset, length, null, from, null);
  }

  @Override
  public ListItemsResults getItemsUntil(List<ScopedFilter> list, int offset, int length, Date until) throws OAIException {
    return getItems(list, offset, length, null, null, until);
  }

  @Override
  public ListItemsResults getItems(List<ScopedFilter> list, int offset, int length, Date from, Date until) throws OAIException {
    return getItems(list, offset, length, null, from,  until);
  }

  @Override
  public ListItemsResults getItems(List<ScopedFilter> list, int offset, int length, String set) throws OAIException {
    return getItems(list, offset, length, set, null,  null);
  }

  @Override
  public ListItemsResults getItems(List<ScopedFilter> list, int offset, int length, String set, Date from) throws OAIException {
    return getItems(list, offset, length, set, from,  null);
  }

  @Override
  public ListItemsResults getItemsUntil(List<ScopedFilter> list, int offset, int length, String set, Date until) throws OAIException {
    return getItems(list, offset, length, set, null,  until);
  }

  @Override
  public ListItemsResults getItems(List<ScopedFilter> list, int offset, int length, String set, Date from, Date until) throws OAIException {

    List<Dataset> datasetList = getDatasetListFromFilters(offset, length, set, from, until);
    List<Item> results = Lists.newArrayListWithCapacity(datasetList.size());

    try {
      for (Dataset dataset : datasetList) {
        results.add(toOaipmhItem(dataset, getSets(dataset)));
      }
    } catch (IOException e) {
      //TODO handle that, caused by https://github.com/DSpace/xoai/issues/31
      e.printStackTrace();
    }

    //FIXME asMoreResults is not set properly
    return new ListItemsResults(true, results);

  }

  private OaipmhItem toOaipmhItem(Dataset dataset, List<Set> sets) throws IOException {
    /**
     * The XOAI library doesn't provide us with the metadata type (EML / OAI DC), so both must be produced.
     * An XSLT transform pulls out the one that's required.
     * This is ugly, so see https://github.com/DSpace/xoai/issues/31
     */
    StringWriter xml = new StringWriter();

    xml.write("<root>");

    xml.write("<oaidc>\n");
    DublinCoreWriter.write(dataset, xml);
    xml.write("</oaidc>\n");

    xml.write("<eml>\n");
    EMLWriter.write(dataset, xml);
    xml.write("</eml>\n");

    xml.write("</root>\n");
    return new OaipmhItem(dataset, xml.toString(), sets);
  }

  /**
   * Get the list of {@link org.dspace.xoai.dataprovider.model.Set} for a {@link Dataset}.
   *
   * @param dataset
   * @return
   */
  private List<Set> getSets(Dataset dataset) {
    Country publishingCountry = null;
    try {
      publishingCountry = ORGANIZATION_CACHE.get(dataset.getPublishingOrganizationKey()).getCountry();
    } catch (ExecutionException e) {
      LOG.error("Error while loading Organization from cache for dataset {}", dataset.getKey(), e);
    }
    List<Set> sets = Lists.newArrayList();
    sets.add(new Set(OaipmhSetRepository.SetType.INSTALLATION.getSubsetPrefix() + dataset.getInstallationKey().toString()));
    sets.add(new Set(OaipmhSetRepository.SetType.DATASET_TYPE.getSubsetPrefix() + dataset.getType().toString()));
    if(publishingCountry != null) {
      sets.add(new Set(OaipmhSetRepository.SetType.COUNTRY.getSubsetPrefix() + publishingCountry.getIso2LetterCode()));
    }
    return sets;
  }

  /**
   * Get a list of {@link Dataset} based on filter(s).
   *
   * @param offset
   * @param length
   * @param set set name in the form of set:subset {@see http://www.openarchives.org/OAI/openarchivesprotocol.html#Set}
   * @param from
   * @param until
   * @return
   */
  private List<Dataset> getDatasetListFromFilters(int offset, int length, String set, Date from, Date until){

    Optional<OaipmhSetRepository.SetIdentification> setIdentification = OaipmhSetRepository.parseSetName(set);

    List<Dataset> datasetList;
    if(setIdentification.isPresent()){
      Country country = null;
      UUID installationKey = null;
      DatasetType datasetType = null;

      //TODO handle wrong set values ---
      String subSet = setIdentification.get().getSubSet();
      switch (setIdentification.get().getSetType()) {
        case COUNTRY:
          country = Country.fromIsoCode(subSet);
          break;
        case INSTALLATION:
          installationKey = UUID.fromString(subSet);
          break;
        case DATASET_TYPE:
          datasetType = DatasetType.fromString(subSet);
          break;
      }

      datasetList = datasetMapper.listWithFilter(country, datasetType, installationKey,
              from, until, new PagingRequest(offset, length));
    }
    else {
      datasetList = datasetMapper.listWithFilter(null, null, null, from, until, new PagingRequest(offset, length));
    }

    return datasetList;
  }

}
