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
package org.gbif.registry.oaipmh;

import org.gbif.api.exception.ServiceUnavailableException;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.metrics.cube.OccurrenceCube;
import org.gbif.api.model.metrics.cube.ReadBuilder;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.registry.metadata.DublinCoreWriter;
import org.gbif.registry.metadata.EMLWriter;
import org.gbif.registry.occurrenceclient.OccurrenceMetricsClient;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.service.RegistryDatasetService;
import org.gbif.ws.util.ExtraMediaTypes;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.NotNull;

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
import org.springframework.stereotype.Component;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;

import static org.gbif.registry.oaipmh.OaipmhSetRepository.SetType.COUNTRY;
import static org.gbif.registry.oaipmh.OaipmhSetRepository.SetType.DATASET_TYPE;
import static org.gbif.registry.oaipmh.OaipmhSetRepository.SetType.INSTALLATION;

/** Implementation of a XOAI ItemRepository for {@link Dataset}. */
@Component
public class OaipmhItemRepository implements ItemRepository {

  private static final Logger LOG = LoggerFactory.getLogger(OaipmhItemRepository.class);

  private final LoadingCache<UUID, Organization> ORGANIZATION_CACHE =
      CacheBuilder.newBuilder()
          .maximumSize(1000)
          .expireAfterAccess(1, TimeUnit.MINUTES)
          .build(buildOrganizationCacheLoader());

  private final RegistryDatasetService datasetService;
  private final OrganizationMapper organizationMapper;
  private final DatasetMapper datasetMapper;
  private final OccurrenceMetricsClient occurrenceMetricsClient;

  private final EMLWriter emlWriter;
  private final DublinCoreWriter dublinCoreWriter;

  public OaipmhItemRepository(
      RegistryDatasetService datasetService,
      DatasetMapper datasetMapper,
      OrganizationMapper organizationMapper,
      OccurrenceMetricsClient occurrenceMetricsClient) {
    this.datasetService = datasetService;
    this.datasetMapper = datasetMapper;
    this.organizationMapper = organizationMapper;
    this.occurrenceMetricsClient = occurrenceMetricsClient;

    // should eventually be injected
    emlWriter = EMLWriter.newInstance(false, true);
    dublinCoreWriter = DublinCoreWriter.newInstance();
  }

  /**
   * Build a CacheLoader<UUID, Organization> around the organizationMapper instance.
   *
   * @return
   */
  private CacheLoader<UUID, Organization> buildOrganizationCacheLoader() {
    return new CacheLoader<UUID, Organization>() {
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
      dataset = datasetService.get(UUID.fromString(s));
    } catch (IllegalArgumentException ignoreEx) {
    }

    if (dataset != null) {
      try {
        return toOaipmhItem(dataset);
      } catch (Exception e) {
        throw new ServiceUnavailableException("Failed to serialize dataset " + s + " to DC/EML", e);
      }
    }
    throw new IdDoesNotExistException();
  }

  @Override
  public ListItemIdentifiersResult getItemIdentifiers(
      List<ScopedFilter> list, int offset, int length) throws OAIException {
    return getItemIdentifiers(list, offset, length, null, null, null);
  }

  @Override
  public ListItemIdentifiersResult getItemIdentifiers(
      List<ScopedFilter> list, int offset, int length, Date from) throws OAIException {
    return getItemIdentifiers(list, offset, length, null, from, null);
  }

  @Override
  public ListItemIdentifiersResult getItemIdentifiersUntil(
      List<ScopedFilter> list, int offset, int length, Date until) throws OAIException {
    return getItemIdentifiers(list, offset, length, null, null, until);
  }

  @Override
  public ListItemIdentifiersResult getItemIdentifiers(
      List<ScopedFilter> list, int offset, int length, Date from, Date until) throws OAIException {
    return getItemIdentifiers(list, offset, length, null, from, until);
  }

  @Override
  public ListItemIdentifiersResult getItemIdentifiers(
      List<ScopedFilter> list, int offset, int length, String set) throws OAIException {
    return getItemIdentifiers(list, offset, length, set, null, null);
  }

  @Override
  public ListItemIdentifiersResult getItemIdentifiers(
      List<ScopedFilter> list, int offset, int length, String set, Date from) throws OAIException {
    return getItemIdentifiers(list, offset, length, set, from, null);
  }

  @Override
  public ListItemIdentifiersResult getItemIdentifiersUntil(
      List<ScopedFilter> list, int offset, int length, String set, Date until) throws OAIException {
    return getItemIdentifiers(list, offset, length, set, null, until);
  }

  /**
   * Get items identifier as {@link ListItemIdentifiersResult} matching the provided filters.
   *
   * @param list
   * @param offset
   * @param length
   * @param set a valid set or null
   * @param from from date (inclusive) or null
   * @param until until date (exclusive) or null
   * @return
   * @throws OAIException
   */
  @Override
  public ListItemIdentifiersResult getItemIdentifiers(
      List<ScopedFilter> list, int offset, int length, String set, Date from, Date until)
      throws OAIException {
    // ask for length+1 to determine if there are more results
    List<Dataset> datasetList = getDatasetListFromFilters(offset, length + 1, set, from, until);
    List<ItemIdentifier> results = Lists.newArrayListWithCapacity(datasetList.size());

    boolean hasMoreResults = (datasetList.size() == length + 1);
    // remove last element, it was only retrieve to determine hasMoreResults
    if (hasMoreResults) {
      datasetList.remove(datasetList.size() - 1);
    }

    for (Dataset dataset : datasetList) {
      results.add(toOaipmhItemIdentifier(dataset));
    }

    return new ListItemIdentifiersResult(hasMoreResults, results);
  }

  /**
   * See {@link #getItems(List, int, int, String, Date, Date) getItems}
   *
   * @param list
   * @param offset
   * @param length
   * @return
   * @throws OAIException
   */
  @Override
  public ListItemsResults getItems(List<ScopedFilter> list, int offset, int length)
      throws OAIException {
    return getItems(list, offset, length, null, null, null);
  }

  /**
   * See {@link #getItems(List, int, int, String, Date, Date) getItems}
   *
   * @param list
   * @param offset
   * @param length
   * @param from
   * @return
   * @throws OAIException
   */
  @Override
  public ListItemsResults getItems(List<ScopedFilter> list, int offset, int length, Date from)
      throws OAIException {
    return getItems(list, offset, length, null, from, null);
  }

  /**
   * See {@link #getItems(List, int, int, String, Date, Date) getItems}
   *
   * @param list
   * @param offset
   * @param length
   * @param until
   * @return
   * @throws OAIException
   */
  @Override
  public ListItemsResults getItemsUntil(List<ScopedFilter> list, int offset, int length, Date until)
      throws OAIException {
    return getItems(list, offset, length, null, null, until);
  }

  /**
   * See {@link #getItems(List, int, int, String, Date, Date) getItems}
   *
   * @param list
   * @param offset
   * @param length
   * @param from
   * @param until
   * @return
   * @throws OAIException
   */
  @Override
  public ListItemsResults getItems(
      List<ScopedFilter> list, int offset, int length, Date from, Date until) throws OAIException {
    return getItems(list, offset, length, null, from, until);
  }

  /**
   * See {@link #getItems(List, int, int, String, Date, Date) getItems}
   *
   * @param list
   * @param offset
   * @param length
   * @param set
   * @return
   * @throws OAIException
   */
  @Override
  public ListItemsResults getItems(List<ScopedFilter> list, int offset, int length, String set)
      throws OAIException {
    return getItems(list, offset, length, set, null, null);
  }

  /**
   * See {@link #getItems(List, int, int, String, Date, Date) getItems}
   *
   * @param list
   * @param offset
   * @param length
   * @param set
   * @param from
   * @return
   * @throws OAIException
   */
  @Override
  public ListItemsResults getItems(
      List<ScopedFilter> list, int offset, int length, String set, Date from) throws OAIException {
    return getItems(list, offset, length, set, from, null);
  }

  /**
   * See {@link #getItems(List, int, int, String, Date, Date) getItems}
   *
   * @param list
   * @param offset
   * @param length
   * @param set
   * @param until
   * @return
   * @throws OAIException
   */
  @Override
  public ListItemsResults getItemsUntil(
      List<ScopedFilter> list, int offset, int length, String set, Date until) throws OAIException {
    return getItems(list, offset, length, set, null, until);
  }

  /**
   * Get items as {@link ListItemsResults} matching the provided filters.
   *
   * @param list
   * @param offset
   * @param length
   * @param set a valid set or null
   * @param from from date (inclusive) or null
   * @param until until date (exclusive) or null
   * @return
   * @throws OAIException
   */
  @Override
  public ListItemsResults getItems(
      List<ScopedFilter> list, int offset, int length, String set, Date from, Date until)
      throws OAIException {

    // ask for length+1 to determine if there are more results
    List<Dataset> datasetList = getDatasetListFromFilters(offset, length + 1, set, from, until);
    List<Item> results = Lists.newArrayListWithCapacity(datasetList.size());

    boolean hasMoreResults = (datasetList.size() == length + 1);
    // remove last element, it was only retrieve to determine hasMoreResults
    if (hasMoreResults) {
      datasetList.remove(datasetList.size() - 1);
    }

    PagingResponse<Dataset> pagingResponse = new PagingResponse<>();
    pagingResponse.setResults(datasetList);
    pagingResponse = datasetService.augmentWithMetadata(pagingResponse);

    try {
      for (Dataset dataset : pagingResponse.getResults()) {
        results.add(toOaipmhItem(dataset));
      }
    } catch (IOException e) {
      // caused by https://github.com/DSpace/xoai/issues/31
      LOG.error("Failed to serialize datasets to DC/EML", e);
    }
    return new ListItemsResults(hasMoreResults, results);
  }

  /**
   * Build a {@OaipmhItem} instance from a {@link Dataset} and the {@link Set} it belongs to.
   *
   * @param dataset
   * @return
   * @throws IOException
   */
  private OaipmhItem toOaipmhItem(Dataset dataset) throws IOException {

    Organization organization = null;
    try {
      organization = ORGANIZATION_CACHE.get(dataset.getPublishingOrganizationKey());
    } catch (ExecutionException e) {
      LOG.error("Error while loading Organization from cache fro dataset {}", dataset, e);
    }
    List<Set> sets = getSets(organization, dataset);
    Map<String, Object> additionalProperties = new HashMap<>();
    additionalProperties.put(
        DublinCoreWriter.ADDITIONAL_PROPERTY_DC_FORMAT, ExtraMediaTypes.APPLICATION_DWCA);

    // get the occurrence counts for this dataset, only used in DublinCore
    // This is designed to fail fast (short http timeout) and on failures which are expected to be
    // exceptional
    // events, it is simply omitted.  See the Guice RegistryWsSevletListener for the configuration
    // of the timeout.
    ReadBuilder readBuilder = new ReadBuilder();
    readBuilder.at(OccurrenceCube.DATASET_KEY, dataset.getKey());
    try {
      Long occurrenceCount = occurrenceMetricsClient.getCountForDataset(dataset.getKey());
      if (occurrenceCount > 0) {
        additionalProperties.put(DublinCoreWriter.ADDITIONAL_PROPERTY_OCC_COUNT, occurrenceCount);
      }
    } catch (Exception ex) {
      LOG.warn(
          "Unable to get occurrence count from cubeService for dataset {}. Omitting count.",
          dataset.getKey(),
          ex);
    }

    /**
     * The XOAI library doesn't provide us with the metadata type (EML / OAI DC), so both must be
     * produced. An XSLT transform pulls out the one that's required. This is ugly, so see
     * https://github.com/DSpace/xoai/issues/31
     */
    StringWriter xml = new StringWriter();

    xml.write("<root>");

    xml.write("<oaidc>\n");
    dublinCoreWriter.writeTo(organization, dataset, additionalProperties, xml);
    xml.write("</oaidc>\n");

    xml.write("<eml>\n");
    emlWriter.writeTo(dataset, xml);
    xml.write("</eml>\n");

    xml.write("</root>\n");
    return new OaipmhItem(dataset, xml.toString(), sets);
  }

  private OaipmhItem toOaipmhItemIdentifier(Dataset dataset) {
    Organization organization = null;
    try {
      organization = ORGANIZATION_CACHE.get(dataset.getPublishingOrganizationKey());
    } catch (ExecutionException e) {
      LOG.error("Error while loading Organization from cache fro dataset {}", dataset, e);
    }
    return new OaipmhItem(dataset, getSets(organization, dataset));
  }

  /**
   * Get the list of {@link org.dspace.xoai.dataprovider.model.Set} for a {@link Dataset}.
   *
   * @param organization {@link Organization}, can be null
   * @param dataset non-null {@link Dataset}
   * @return list of all {@link org.dspace.xoai.dataprovider.model.Set} that the {@link Dataset}
   *     belongs to. Never null.
   */
  private List<Set> getSets(Organization organization, @NotNull Dataset dataset) {
    Country publishingCountry = null;

    if (organization != null) {
      publishingCountry = organization.getCountry();
    }

    List<Set> sets = Lists.newArrayList();
    sets.add(new Set(INSTALLATION.getSubsetPrefix() + dataset.getInstallationKey().toString()));
    sets.add(new Set(DATASET_TYPE.getSubsetPrefix() + dataset.getType().toString()));
    if (publishingCountry != null) {
      sets.add(new Set(COUNTRY.getSubsetPrefix() + publishingCountry.getIso2LetterCode()));
    }
    return sets;
  }

  /**
   * Get a list of {@link Dataset} based on filter(s).
   *
   * @param offset
   * @param length
   * @param set set name in the form of set:subset {@see
   *     http://www.openarchives.org/OAI/openarchivesprotocol.html#Set} XOAI library validates the
   *     set before calling the ItemRepository so we do not validate it again here.
   * @param from
   * @param until
   * @return list of matching {@link Dataset}. Never null.
   */
  private List<Dataset> getDatasetListFromFilters(
      int offset, int length, String set, Date from, Date until) {

    Optional<OaipmhSetRepository.SetIdentification> setIdentification =
        OaipmhSetRepository.parseSetName(set);

    List<Dataset> datasetList;
    if (setIdentification.isPresent()) {
      Country country = null;
      UUID installationKey = null;
      DatasetType datasetType = null;

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

      datasetList =
          datasetMapper.listWithFilter(
              country,
              datasetType,
              installationKey,
              from,
              until,
              new PagingRequest(offset, length));
    } else {
      datasetList =
          datasetMapper.listWithFilter(
              null, null, null, from, until, new PagingRequest(offset, length));
    }

    return datasetList;
  }
}
