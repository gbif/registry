/*
 * Copyright 2013 Global Biodiversity Information Facility (GBIF)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.ws.resources;

import org.gbif.api.exception.ServiceUnavailableException;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.model.crawler.DatasetProcessStatus;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Metadata;
import org.gbif.api.model.registry.Network;
import org.gbif.api.model.registry.search.DatasetSearchParameter;
import org.gbif.api.model.registry.search.DatasetSearchRequest;
import org.gbif.api.model.registry.search.DatasetSearchResult;
import org.gbif.api.model.registry.search.DatasetSuggestRequest;
import org.gbif.api.model.registry.search.DatasetSuggestResult;
import org.gbif.api.service.registry.DatasetProcessStatusService;
import org.gbif.api.service.registry.DatasetSearchService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.MetadataType;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.StartCrawlMessage;
import org.gbif.common.messaging.api.messages.StartCrawlMessage.Priority;
import org.gbif.registry.metadata.EMLWriter;
import org.gbif.registry.metadata.parse.DatasetParser;
import org.gbif.registry.persistence.WithMyBatis;
import org.gbif.registry.persistence.mapper.CommentMapper;
import org.gbif.registry.persistence.mapper.ContactMapper;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.DatasetProcessStatusMapper;
import org.gbif.registry.persistence.mapper.EndpointMapper;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.MetadataMapper;
import org.gbif.registry.persistence.mapper.NetworkMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.ws.guice.Trim;
import org.gbif.registry.ws.security.EditorAuthorizationService;
import org.gbif.ws.server.interceptor.NullToNotFound;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.mybatis.guice.transactional.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.ws.security.UserRoles.EDITOR_ROLE;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A MyBATIS implementation of the service.
 */
@Path("dataset")
@Singleton
public class DatasetResource extends BaseNetworkEntityResource<Dataset>
  implements DatasetService, DatasetSearchService, DatasetProcessStatusService {

  private static final Logger LOG = LoggerFactory.getLogger(DatasetResource.class);
  private final DatasetSearchService searchService;
  private final MetadataMapper metadataMapper;
  private final DatasetMapper datasetMapper;
  private final ContactMapper contactMapper;
  private final NetworkMapper networkMapper;
  private final DatasetProcessStatusMapper datasetProcessStatusMapper;

  /**
   * The messagePublisher can be optional, and optional is not supported in constructor injection.
   */
  @Inject(optional = true)
  private final MessagePublisher messagePublisher = null;

  @Inject
  public DatasetResource(DatasetMapper datasetMapper, ContactMapper contactMapper, EndpointMapper endpointMapper,
    MachineTagMapper machineTagMapper, TagMapper tagMapper, IdentifierMapper identifierMapper,
    CommentMapper commentMapper, EventBus eventBus, DatasetSearchService searchService, MetadataMapper metadataMapper,
    DatasetProcessStatusMapper datasetProcessStatusMapper, NetworkMapper networkMapper,
    EditorAuthorizationService userAuthService) {
    super(datasetMapper, commentMapper, contactMapper, endpointMapper, identifierMapper, machineTagMapper, tagMapper,
      Dataset.class, eventBus, userAuthService);
    this.searchService = searchService;
    this.metadataMapper = metadataMapper;
    this.datasetMapper = datasetMapper;
    this.contactMapper = contactMapper;
    this.datasetProcessStatusMapper = datasetProcessStatusMapper;
    this.networkMapper = networkMapper;
  }

  @GET
  @Path("search")
  @Override
  public SearchResponse<DatasetSearchResult, DatasetSearchParameter> search(
    @Context DatasetSearchRequest searchRequest) {
    LOG.debug("Search operation received {}", searchRequest);
    return searchService.search(searchRequest);
  }

  @Path("suggest")
  @GET
  @Override
  public List<DatasetSuggestResult> suggest(@Context DatasetSuggestRequest suggestRequest) {
    // TODO: Commented out because DatasetSuggestRequest doesn't have a toString method yet
    // LOG.debug("Suggest operation received {}", suggestRequest);
    return searchService.suggest(suggestRequest);
  }

  @GET
  @Path("{key}")
  @Nullable
  @NullToNotFound
  @Override
  public Dataset get(@PathParam("key") UUID key) {
    return merge(getPreferredMetadataDataset(key), super.get(key));
  }


  /**
   * All network entities support simple (!) search with "&q=".
   * This is to support the console user interface, and is in addition to any complex, faceted search that might
   * additionally be supported, such as dataset search.
   */
  @GET
  public PagingResponse<Dataset> list(@Nullable @Context Country country,
    @Nullable @QueryParam("type") DatasetType datasetType,
    @Nullable @QueryParam("identifierType") IdentifierType identifierType,
    @Nullable @QueryParam("identifier") String identifier,
    @Nullable @QueryParam("q") String query,
    @Nullable @Context Pageable page) {
    // This is getting messy: http://dev.gbif.org/issues/browse/REG-426
    if (country == null && datasetType != null) {
      return listByType(datasetType, page);
    } else if (country != null) {
      return listByCountry(country, datasetType, page);
    } else if (identifierType != null && identifier != null) {
      return listByIdentifier(identifierType, identifier, page);
    } else if (identifier != null) {
      return listByIdentifier(identifier, page);
    } else if (!Strings.isNullOrEmpty(query)) {
      return search(query, page);
    } else {
      return list(page);
    }
  }

  @Override
  public PagingResponse<Dataset> listByCountry(Country country, DatasetType type, Pageable page) {
    long total = datasetMapper.countWithFilter(country, type);
    return pagingResponse(page, total, datasetMapper.listWithFilter(country, type, page));
  }

  @Override
  public PagingResponse<Dataset> listByType(DatasetType type, Pageable page) {
    long total = datasetMapper.countWithFilter(null, type);
    return pagingResponse(page, total, datasetMapper.listWithFilter(null, type, page));
  }


  @Override
  public PagingResponse<Dataset> search(String query, @Nullable Pageable page) {
    return augmentWithMetadata(super.search(query, page));
  }

  @Override
  public PagingResponse<Dataset> list(@Nullable Pageable page) {
    return augmentWithMetadata(super.list(page));
  }

  /**
   * Returns the parsed, preferred metadata document as a dataset.
   */
  private Dataset getPreferredMetadataDataset(UUID key) {
    List<Metadata> docs = listMetadata(key, null);
    if (!docs.isEmpty()) {
      InputStream stream = null;
      try {
        // the list is sorted by priority already, just pick the first!
        stream = getMetadataDocument(docs.get(0).getKey());
        return DatasetParser.build(stream);
      } catch (IOException e) {
        LOG.error("Stored metadata document {} cannot be read", docs.get(0).getKey(), e);
      } finally {
        Closeables.closeQuietly(stream);
      }
    }

    return null;
  }

  /**
   * Augments a list of datasets with information from their preferred metadata document.
   * 
   * @return a the same paging response with a new list of augmented dataset instances
   */
  private PagingResponse<Dataset> augmentWithMetadata(PagingResponse<Dataset> resp) {
    List<Dataset> augmented = Lists.newArrayList();
    for (Dataset d : resp.getResults()) {
      augmented.add(merge(getPreferredMetadataDataset(d.getKey()), d));
    }
    resp.setResults(augmented);
    return resp;
  }

  /**
   * Augments the target dataset with all persistable properties from the supplementary dataset.
   * Typically the target would be a dataset built from rich XML metadata, and the supplementary would be the persisted
   * view of the same dataset. NULL values in the supplementary dataset overwrite existing values in the target.
   * Developers please note:
   * <ul>
   * <li>If the target is null, then the supplementary dataset object itself is returned - not a copy</li>
   * <li>These objects are all mutable, and care should be taken that the returned object may be one or the other of the
   * supplied, thus you need to {@code Dataset result = merge(Dataset emlView, Dataset dbView);}</li>
   * </ul>
   * 
   * @param target that will be modified with persitable values from the supplementary
   * @param supplementary holding the preferred properties for the target
   * @return the modified tagret dataset, or the supplementary dataset if the target is null
   */
  @Nullable
  private Dataset merge(@Nullable Dataset target, @Nullable Dataset supplementary) {
    // nothing to merge, return the target (which may be null)
    if (supplementary == null) {
      return target;
    }

    // nothing to overlay into
    if (target == null) {
      return supplementary;
    }

    // otherwise, copy all persisted values
    target.setKey(supplementary.getKey());
    target.setParentDatasetKey(supplementary.getParentDatasetKey());
    target.setDuplicateOfDatasetKey(supplementary.getDuplicateOfDatasetKey());
    target.setInstallationKey(supplementary.getInstallationKey());
    target.setPublishingOrganizationKey(supplementary.getPublishingOrganizationKey());
    target.setExternal(supplementary.isExternal());
    target.setNumConstituents(supplementary.getNumConstituents());
    target.setType(supplementary.getType());
    target.setSubtype(supplementary.getSubtype());
    target.setTitle(supplementary.getTitle());
    target.setAlias(supplementary.getAlias());
    target.setAbbreviation(supplementary.getAbbreviation());
    target.setDescription(supplementary.getDescription());
    target.setLanguage(supplementary.getLanguage());
    target.setHomepage(supplementary.getHomepage());
    target.setLogoUrl(supplementary.getLogoUrl());
    target.setCitation(supplementary.getCitation());
    target.setRights(supplementary.getRights());
    target.setLockedForAutoUpdate(supplementary.isLockedForAutoUpdate());
    target.setCreated(supplementary.getCreated());
    target.setCreatedBy(supplementary.getCreatedBy());
    target.setModified(supplementary.getModified());
    target.setModifiedBy(supplementary.getModifiedBy());
    target.setDeleted(supplementary.getDeleted());
    // nested properties
    target.setComments(supplementary.getComments());
    target.setContacts(supplementary.getContacts());
    target.setEndpoints(supplementary.getEndpoints());
    target.setIdentifiers(supplementary.getIdentifiers());
    target.setMachineTags(supplementary.getMachineTags());
    target.setTags(supplementary.getTags());

    return target;
  }

  @Path("{key}/document")
  @GET
  @Produces(MediaType.APPLICATION_XML)
  @Override
  public InputStream getMetadataDocument(@PathParam("key") UUID datasetKey) {
    // the fully augmented dataset
    Dataset dataset = get(datasetKey);
    if (dataset != null) {
      // generate new EML
      try {
        StringWriter eml = new StringWriter();
        EMLWriter.write(dataset, eml);
        return new ByteArrayInputStream(eml.toString().getBytes("UTF-8"));

      } catch (Exception e) {
        throw new ServiceUnavailableException("Failed to serialize dataset " + datasetKey, e);
      }
    }
    return null;
  }

  @Path("{key}/document")
  @POST
  @Consumes(MediaType.APPLICATION_XML)
  @RolesAllowed({ADMIN_ROLE, EDITOR_ROLE})
  public Metadata insertMetadata(@PathParam("key") UUID datasetKey, @Context HttpServletRequest request,
    @Context SecurityContext security) {
    try {
      return insertMetadata(datasetKey, request.getInputStream(), security.getUserPrincipal().getName());
    } catch (IOException e) {
      return null;
    }
  }

  private Metadata insertMetadata(UUID datasetKey, InputStream document, String user) {
    // check if the dataset actually exists
    Dataset dataset = super.get(datasetKey);
    if (dataset == null) {
      throw new IllegalArgumentException("Dataset " + datasetKey + " not existing");
    }

    // first keep document as byte array so we can analyze it as much as we want and store it later
    byte[] data;
    try {
      data = ByteStreams.toByteArray(document);
    } catch (IOException e) {
      throw new IllegalArgumentException("Unreadable document", e);
    }

    // now detect type and create a new metadata record
    MetadataType type;
    InputStream in = null;
    try {
      in = new ByteArrayInputStream(data);
      type = DatasetParser.detectParserType(in);
    } finally {
      Closeables.closeQuietly(in);
    }

    if (type == null) {
      throw new IllegalArgumentException("Document format not supported");
    }

    Metadata metadata = new Metadata();
    metadata.setDatasetKey(datasetKey);
    metadata.setType(type);
    metadata.setCreatedBy(user);
    metadata.setModifiedBy(user);

    // persist metadata & data
    // first remove all existing metadata of the same type (so we end up storing only one document per type)
    for (Metadata exist : listMetadata(datasetKey, type)) {
      deleteMetadata(exist.getKey());
    }
    int metaKey = metadataMapper.create(metadata, data);
    metadata.setKey(metaKey);

    // check if we should update our registered base information
    if (dataset.isLockedForAutoUpdate()) {
      LOG
        .info(
          "Dataset {} locked for automatic updates. Uploaded metadata document not does not modify registered dataset information",
          datasetKey);

    } else {
      // we retrieve the preferred document and only update if this new metadata is the preferred one
      // e.g. we could put a DC document while an EML document exists that takes preference
      Dataset updDataset = getPreferredMetadataDataset(datasetKey);
      // keep some of the original properties
      updDataset.setKey(dataset.getKey());
      updDataset.setParentDatasetKey(dataset.getParentDatasetKey());
      updDataset.setDuplicateOfDatasetKey(dataset.getDuplicateOfDatasetKey());
      updDataset.setInstallationKey(dataset.getInstallationKey());
      updDataset.setPublishingOrganizationKey(dataset.getPublishingOrganizationKey());
      updDataset.setExternal(dataset.isExternal());
      updDataset.setNumConstituents(dataset.getNumConstituents());
      updDataset.setType(dataset.getType());
      updDataset.setSubtype(dataset.getSubtype());
      updDataset.setLockedForAutoUpdate(dataset.isLockedForAutoUpdate());
      updDataset.setCreatedBy(dataset.getCreatedBy());
      updDataset.setCreated(dataset.getCreated());
      updDataset.setModifiedBy(user);
      updDataset.setModified(new Date());
      // persist contacts, overwriting any existing ones
      for (Contact c : dataset.getContacts()) {
        datasetMapper.deleteContact(datasetKey, c.getKey());
      }
      for (Contact c : updDataset.getContacts()) {
        c.setCreatedBy(user);
        c.setCreated(new Date());
        c.setModifiedBy(user);
        c.setModified(new Date());
        WithMyBatis.addContact(contactMapper, datasetMapper, datasetKey, c);
      }
      // now update the core dataset only, remove associated data which could break validation
      updDataset.getContacts().clear();
      updDataset.getIdentifiers().clear();
      updDataset.getTags().clear();
      updDataset.getMachineTags().clear();
      update(updDataset);

      LOG.info("Dataset {} updated with base information from metadata document {}", datasetKey, metaKey);
    }

    return metadata;
  }

  /**
   * We need to implement this interface method here, but there is no way to retrieve the actual user
   * as we cannot access any http request. The real server method does this correctly but has more parameters.
   */
  @Override
  public Metadata insertMetadata(@PathParam("key") UUID datasetKey, InputStream document) {
    // this method should never be called but from tests
    return insertMetadata(datasetKey, document, "UNKNOWN USER");
  }

  @Path("{key}/constituents")
  @GET
  @Override
  public PagingResponse<Dataset> listConstituents(@PathParam("key") UUID datasetKey, @Context Pageable page) {
    return pagingResponse(page, (long) datasetMapper.countConstituents(datasetKey),
      datasetMapper.listConstituents(datasetKey, page));
  }

  @Path("{key}/networks")
  @GET
  @Override
  public List<Network> listNetworks(@PathParam("key") UUID datasetKey) {
    return networkMapper.listByDataset(datasetKey);
  }

  @GET
  @Path("constituents")
  @Override
  public PagingResponse<Dataset> listConstituents(@Context Pageable page) {
    return pagingResponse(page, datasetMapper.countSubdatasets(), datasetMapper.subdatasets(page));
  }

  @Path("{key}/metadata")
  @GET
  @Override
  public List<Metadata> listMetadata(@PathParam("key") UUID datasetKey, @QueryParam("type") MetadataType type) {
    return metadataMapper.list(datasetKey, type);
  }

  @Path("metadata/{key}")
  @GET
  @Override
  @NullToNotFound
  public Metadata getMetadata(@PathParam("key") int metadataKey) {
    return metadataMapper.get(metadataKey);
  }

  @Path("metadata/{key}/document")
  @GET
  @Produces(MediaType.APPLICATION_XML)
  @Override
  public InputStream getMetadataDocument(@PathParam("key") int metadataKey) {
    return new ByteArrayInputStream(metadataMapper.getDocument(metadataKey).getData());
  }

  @Path("metadata/{key}")
  @DELETE
  @Override
  public void deleteMetadata(@PathParam("key") int metadataKey) {
    metadataMapper.delete(metadataKey);
  }

  @GET
  @Path("deleted")
  @Override
  public PagingResponse<Dataset> listDeleted(@Context Pageable page) {
    return pagingResponse(page, datasetMapper.countDeleted(), datasetMapper.deleted(page));
  }

  @GET
  @Path("duplicate")
  @Override
  public PagingResponse<Dataset> listDuplicates(@Context Pageable page) {
    return pagingResponse(page, datasetMapper.countDuplicates(), datasetMapper.duplicates(page));
  }

  @GET
  @Path("withNoEndpoint")
  @Override
  public PagingResponse<Dataset> listDatasetsWithNoEndpoint(@Context Pageable page) {
    return pagingResponse(page, datasetMapper.countWithNoEndpoint(), datasetMapper.withNoEndpoint(page));
  }

  /**
   * This is a REST only (e.g. not part of the Java API) method that allows the registry console to trigger the
   * crawling of the dataset. This simply emits a message to rabbitmq requesting the crawl, and applies
   * necessary security.
   */
  @POST
  @Path("{key}/crawl")
  @RolesAllowed({ADMIN_ROLE, EDITOR_ROLE})
  public void crawl(@PathParam("key") UUID datasetKey) {
    if (messagePublisher != null) {
      LOG.info("Requesting crawl of dataset[{}]", datasetKey);
      try {
        // we'll bump this to the top of the queue since it is a user initiated
        messagePublisher.send(new StartCrawlMessage(datasetKey, Priority.CRITICAL));
      } catch (IOException e) {
        LOG.error("Unable to send message requesting crawl", e);
      }

    } else {
      LOG.warn("Registry is configured to run without messaging capabilities.  Unable to crawl dataset[{}]",
        datasetKey);
    }
  }

  @POST
  @Path("{datasetKey}/process")
  @Trim
  @Transactional
  @RolesAllowed(ADMIN_ROLE)
  public void createDatasetProcessStatus(@PathParam("datasetKey") UUID datasetKey,
    @Valid @NotNull @Trim DatasetProcessStatus datasetProcessStatus) {
    checkArgument(datasetKey.equals(datasetProcessStatus.getDatasetKey()),
      "DatasetProcessStatus must have the same key as the dataset");
    createDatasetProcessStatus(datasetProcessStatus);
  }

  @Trim
  @Transactional
  @RolesAllowed(ADMIN_ROLE)
  @Override
  public void createDatasetProcessStatus(@Valid @NotNull @Trim DatasetProcessStatus datasetProcessStatus) {
    checkNotNull(datasetProcessStatus.getDatasetKey(),
      "DatasetProcessStatus must have the dataset key");
    checkNotNull(datasetProcessStatus.getCrawlJob(),
      "DatasetProcessStatus must have the crawl job with an attempt number");
    DatasetProcessStatus existing =
      datasetProcessStatusMapper.get(datasetProcessStatus.getDatasetKey(), datasetProcessStatus.getCrawlJob()
        .getAttempt());
    checkArgument(existing == null, "Cannot create dataset process status [%s] for attempt[%s] as one already exists",
      datasetProcessStatus.getDatasetKey(), datasetProcessStatus.getCrawlJob().getAttempt());
    datasetProcessStatusMapper.create(datasetProcessStatus);
  }

  @PUT
  @Path("{datasetKey}/process/{attempt}")
  @Trim
  @Transactional
  @RolesAllowed(ADMIN_ROLE)
  public void createDatasetProcessStatus(@PathParam("datasetKey") UUID datasetKey, @PathParam("attempt") int attempt,
    @Valid @NotNull @Trim DatasetProcessStatus datasetProcessStatus) {
    checkArgument(datasetKey.equals(datasetProcessStatus.getDatasetKey()),
      "DatasetProcessStatus must have the same key as the url");
    checkArgument(attempt == datasetProcessStatus.getCrawlJob().getAttempt(),
      "DatasetProcessStatus must have the same attempt as the url");
    updateDatasetProcessStatus(datasetProcessStatus);
  }

  @Trim
  @Transactional
  @RolesAllowed(ADMIN_ROLE)
  @Override
  public void updateDatasetProcessStatus(@Valid @NotNull @Trim DatasetProcessStatus datasetProcessStatus) {
    datasetProcessStatusMapper.update(datasetProcessStatus);
  }

  @GET
  @Path("{datasetKey}/process/{attempt}")
  @Nullable
  @NullToNotFound
  @Override
  public DatasetProcessStatus getDatasetProcessStatus(@PathParam("datasetKey") UUID datasetKey,
    @PathParam("attempt") int attempt) {
    return datasetProcessStatusMapper.get(datasetKey, attempt);
  }

  @GET
  @Path("process")
  @Override
  public PagingResponse<DatasetProcessStatus> listDatasetProcessStatus(@Context Pageable page) {
    return new PagingResponse<DatasetProcessStatus>(page, (long) datasetProcessStatusMapper.count(),
      datasetProcessStatusMapper.list(page));
  }

  @GET
  @Path("{datasetKey}/process")
  @Override
  public PagingResponse<DatasetProcessStatus> listDatasetProcessStatus(@PathParam("datasetKey") UUID datasetKey,
    @Context Pageable page) {
    return new PagingResponse<DatasetProcessStatus>(page, (long) datasetProcessStatusMapper.countByDataset(datasetKey),
      datasetProcessStatusMapper.listByDataset(datasetKey, page));
  }

  @Override
  protected UUID owningEntityKey(@NotNull Dataset entity) {
    return entity.getPublishingOrganizationKey();
  }
}
