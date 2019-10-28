package org.gbif.registry.ws.resources;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;
import org.gbif.api.exception.ServiceUnavailableException;
import org.gbif.api.model.Constants;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.model.crawler.DatasetProcessStatus;
import org.gbif.api.model.registry.Citation;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.LenientEquals;
import org.gbif.api.model.registry.Metadata;
import org.gbif.api.model.registry.Network;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.PostPersist;
import org.gbif.api.model.registry.PrePersist;
import org.gbif.api.model.registry.Tag;
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
import org.gbif.api.vocabulary.License;
import org.gbif.api.vocabulary.MetadataType;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.Platform;
import org.gbif.common.messaging.api.messages.StartCrawlMessage;
import org.gbif.registry.doi.generator.DoiGenerator;
import org.gbif.registry.doi.handler.DataCiteDoiHandlerStrategy;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.metadata.CitationGenerator;
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
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.mapper.handler.ByteArrayWrapper;
import org.gbif.registry.ws.security.EditorAuthorizationService;
import org.gbif.ws.NotFoundException;
import org.gbif.ws.annotation.NullToNotFound;
import org.gbif.ws.annotation.Trim;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.ws.security.UserRoles.EDITOR_ROLE;

@RestController
@RequestMapping("dataset")
public class DatasetResource
  extends BaseNetworkEntityResource<Dataset>
  implements DatasetService, DatasetSearchService, DatasetProcessStatusService {

  private static final Logger LOG = LoggerFactory.getLogger(DatasetResource.class);

  public static final int ALL_DATASETS_LIMIT = 200;

  //HTML sanitizer policy for paragraph
  private static final PolicyFactory PARAGRAPH_HTML_SANITIZER = new HtmlPolicyBuilder()
    .allowCommonBlockElements() // "p", "div", "h1", ...
    .allowCommonInlineFormattingElements() // "b", "i" ...
    .allowElements("a")
    .allowUrlProtocols("https", "http")
    .allowAttributes("href").onElements("a")
    .toFactory();

  private final DatasetSearchService searchService;
  private final MetadataMapper metadataMapper;
  private final DatasetMapper datasetMapper;
  private final ContactMapper contactMapper;
  private final IdentifierMapper identifierMapper;
  private final TagMapper tagMapper;
  private final NetworkMapper networkMapper;
  private final OrganizationMapper organizationMapper;
  private final DatasetProcessStatusMapper datasetProcessStatusMapper;
  private final DoiGenerator doiGenerator;
  private final DataCiteDoiHandlerStrategy doiHandlerStrategy;
  private final WithMyBatis withMyBatis;

  private final LoadingCache<UUID, Organization> ORGANIZATION_CACHE = CacheBuilder.newBuilder()
    .expireAfterWrite(5, TimeUnit.MINUTES)
    .build(
      new CacheLoader<UUID, Organization>() {
        public Organization load(UUID key) {
          return organizationMapper.get(key);
        }
      });

  /**
   * The messagePublisher can be optional.
   */
  private final MessagePublisher messagePublisher;

  public DatasetResource(DatasetMapper datasetMapper,
                         ContactMapper contactMapper,
                         EndpointMapper endpointMapper,
                         MachineTagMapper machineTagMapper,
                         TagMapper tagMapper,
                         IdentifierMapper identifierMapper,
                         CommentMapper commentMapper,
                         EventManager eventManager,
                         @Qualifier("datasetSearchServiceStub") DatasetSearchService searchService,
                         MetadataMapper metadataMapper,
                         DatasetProcessStatusMapper datasetProcessStatusMapper,
                         NetworkMapper networkMapper,
                         EditorAuthorizationService userAuthService,
                         OrganizationMapper organizationMapper,
                         DoiGenerator doiGenerator,
                         DataCiteDoiHandlerStrategy doiHandlingStrategy,
                         WithMyBatis withMyBatis,
                         @Autowired(required = false) MessagePublisher messagePublisher) {
    super(datasetMapper, commentMapper, contactMapper, endpointMapper, identifierMapper, machineTagMapper, tagMapper,
      Dataset.class, eventManager, userAuthService, withMyBatis);
    this.searchService = searchService;
    this.metadataMapper = metadataMapper;
    this.datasetMapper = datasetMapper;
    this.contactMapper = contactMapper;
    this.identifierMapper = identifierMapper;
    this.tagMapper = tagMapper;
    this.datasetProcessStatusMapper = datasetProcessStatusMapper;
    this.networkMapper = networkMapper;
    this.organizationMapper = organizationMapper;
    this.doiGenerator = doiGenerator;
    this.doiHandlerStrategy = doiHandlingStrategy;
    this.messagePublisher = messagePublisher;
    this.withMyBatis = withMyBatis;
  }

  @GetMapping("search")
  @Override
  public SearchResponse<DatasetSearchResult, DatasetSearchParameter> search(DatasetSearchRequest searchRequest) {
    return searchService.search(searchRequest);
  }

  @GetMapping("suggest")
  @Override
  public List<DatasetSuggestResult> suggest(DatasetSuggestRequest suggestRequest) {
    return searchService.suggest(suggestRequest);
  }

  @GetMapping("{key}")
  @Nullable
  @NullToNotFound
  @Override
  public Dataset get(@PathVariable UUID key) {
    Dataset dataset = merge(getPreferredMetadataDataset(key), super.get(key));
    if (dataset == null) {
      return null;
    }

    return sanitizeDataset(dataset);
  }

  /**
   * All network entities support simple (!) search with "&q=".
   * This is to support the console user interface, and is in addition to any complex, faceted search that might
   * additionally be supported, such as dataset search.
   */
  @GetMapping
  public PagingResponse<Dataset> list(
    @Nullable Country country,
    @Nullable @RequestParam(value = "type", required = false) DatasetType datasetType,
    @Nullable @RequestParam(value = "identifierType", required = false) IdentifierType identifierType,
    @Nullable @RequestParam(value = "identifier", required = false) String identifier,
    @Nullable @RequestParam(value = "machineTagNamespace", required = false) String namespace,
    @Nullable @RequestParam(value = "machineTagName", required = false) String name,
    @Nullable @RequestParam(value = "machineTagValue", required = false) String value,
    @Nullable @RequestParam(value = "q", required = false) String query,
    Pageable page
  ) {
    // This is getting messy: http://dev.gbif.org/issues/browse/REG-426
    if (country == null && datasetType != null) {
      return listByType(datasetType, page);
    } else if (country != null) {
      return listByCountry(country, datasetType, page);
    } else if (identifierType != null && identifier != null) {
      return listByIdentifier(identifierType, identifier, page);
    } else if (identifier != null) {
      return listByIdentifier(identifier, page);
    } else if (namespace != null) {
      return listByMachineTag(namespace, name, value, page);
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
  public PagingResponse<Dataset> search(String query, Pageable page) {
    return augmentWithMetadata(super.search(query, page));
  }

  @Override
  public PagingResponse<Dataset> list(Pageable page) {
    return augmentWithMetadata(super.list(page));
  }

  /**
   * Returns the parsed, preferred metadata document as a dataset.
   */
  @Nullable
  private Dataset getPreferredMetadataDataset(UUID key) {
    List<Metadata> docs = listMetadata(key, null);
    if (!docs.isEmpty()) {
      InputStream stream = null;
      try {
        // the list is sorted by priority already, just pick the first!
        stream = getMetadataDocument(docs.get(0).getKey());
        return DatasetParser.build(stream);
      } catch (IOException | IllegalArgumentException e) {
        //Not sure if we should not propagate an Exception to return a 500 instead
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
  public PagingResponse<Dataset> augmentWithMetadata(PagingResponse<Dataset> resp) {
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
   * @param target        that will be modified with persitable values from the supplementary
   * @param supplementary holding the preferred properties for the target
   * @return the modified target dataset, or the supplementary dataset if the target is null
   */
  @Nullable
  private Dataset merge(@Nullable Dataset target, @Nullable Dataset supplementary) {

    // nothing to merge, return the target (which may be null)
    if (supplementary == null) {
      setGeneratedCitation(target);
      return target;
    }

    // nothing to overlay into
    if (target == null) {
      setGeneratedCitation(supplementary);
      return supplementary;
    }

    // otherwise, copy all persisted values
    target.setKey(supplementary.getKey());
    target.setDoi(supplementary.getDoi());
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
    target.setLicense(supplementary.getLicense());
    target.setMaintenanceUpdateFrequency(supplementary.getMaintenanceUpdateFrequency());
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

    setGeneratedCitation(target);

    return target;
  }

  @GetMapping(value = "{key}/document", produces = MediaType.APPLICATION_XML_VALUE)
  @Override
  public InputStream getMetadataDocument(@PathVariable("key") UUID datasetKey) {
    // the fully augmented dataset
    Dataset dataset = get(datasetKey);
    if (dataset != null) {
      // generate new EML
      try {
        StringWriter eml = new StringWriter();
        EMLWriter.write(dataset, eml);
        return new ByteArrayInputStream(eml.toString().getBytes(StandardCharsets.UTF_8));

      } catch (Exception e) {
        throw new ServiceUnavailableException("Failed to serialize dataset " + datasetKey, e);
      }
    }
    return null;
  }

  @PostMapping(value = "{key}/document", consumes = MediaType.APPLICATION_XML_VALUE)
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public Metadata insertMetadata(@PathVariable("key") UUID datasetKey, HttpServletRequest request) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    try {
      return insertMetadata(datasetKey, request.getInputStream(), authentication.getName());
    } catch (IOException e) {
      return null;
    }
  }

  private Metadata insertMetadata(UUID datasetKey, InputStream document, String user) {
    // check if the dataset actually exists
    Dataset dataset = super.get(datasetKey);
    if (dataset == null) {
      throw new NotFoundException("Dataset " + datasetKey + " not existing");
    } else if (dataset.getDeleted() != null) {
      throw new NotFoundException("Dataset " + datasetKey + " has been deleted");
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
    try (InputStream in = new ByteArrayInputStream(data)) {
      type = DatasetParser.detectParserType(in);
      //TODO: should we not also validate the EML/DC document ???
    } catch (IOException e) {
      throw new IllegalArgumentException("Unreadable document", e);
    }

    // first, determine if this document is already stored, returning it with no action
    // we do this, because updating metadata when nothing has changed, results in registry change events being
    // propagated which can trigger crawlers which will run an update etc.
    List<Metadata> existingDocs = listMetadata(datasetKey, type);
    for (Metadata existing : existingDocs) {
      try (InputStream in = getMetadataDocument(existing.getKey())) {
        String existingContent = CharStreams.toString(new InputStreamReader(in, Charsets.UTF_8));
        if (existingContent != null) {
          if (existingContent.equals(new String(data))) {
            LOG.debug("This metadata document already exists - returning existing");
            return existing;
          }
        }
      } catch (Exception e) {
        // swallow - we'll delete it anyway
      }
    }

    // persist metadata & data, which we know is not already stored
    // first remove all existing metadata of the same type (so we end up storing only one document per type)
    Metadata metadata = new Metadata();
    metadata.setDatasetKey(datasetKey);
    metadata.setType(type);
    metadata.setCreatedBy(user);
    metadata.setModifiedBy(user);
    for (Metadata existing : existingDocs) {
      deleteMetadata(existing.getKey());
    }
    int metaKey = metadataMapper.create(metadata, data);
    metadata.setKey(metaKey);

    // check if we should update our registered base information
    if (dataset.isLockedForAutoUpdate()) {
      LOG.info(
        "Dataset {} locked for automatic updates. Uploaded metadata document not does not modify registered dataset information",
        datasetKey);

    } else {
      // we retrieve the preferred document and only update if this new metadata is the preferred one
      // e.g. we could put a DC document while an EML document exists that takes preference
      updateFromPreferredMetadata(datasetKey, user);
      LOG.info("Dataset {} updated with base information from metadata document {}", datasetKey, metaKey);
    }

    return metadata;
  }

  /**
   * When we get a new Metadata document, this method is responsible to preserve the GBIF properties on the
   * dataset object to make sure they are not overwritten.
   *
   * @param updatedDataset  normally instantiated from a metadata document
   * @param existingDataset current {@link Dataset} object from the database.
   * @return same instance as updatedDataset but with GBIF properties preserved (taken from existingDataset)
   */
  private Dataset preserveGBIFDatasetProperties(Dataset updatedDataset, Dataset existingDataset) {
    // keep properties that we do not allow to update via metadata
    updatedDataset.setKey(existingDataset.getKey());
    updatedDataset.setParentDatasetKey(existingDataset.getParentDatasetKey());
    updatedDataset.setDuplicateOfDatasetKey(existingDataset.getDuplicateOfDatasetKey());
    updatedDataset.setInstallationKey(existingDataset.getInstallationKey());
    updatedDataset.setPublishingOrganizationKey(existingDataset.getPublishingOrganizationKey());
    updatedDataset.setExternal(existingDataset.isExternal());
    updatedDataset.setNumConstituents(existingDataset.getNumConstituents());
    updatedDataset.setType(existingDataset.getType());
    updatedDataset.setSubtype(existingDataset.getSubtype());
    updatedDataset.setLockedForAutoUpdate(existingDataset.isLockedForAutoUpdate());
    updatedDataset.setCreatedBy(existingDataset.getCreatedBy());
    updatedDataset.setCreated(existingDataset.getCreated());

    // keep original license, unless a supported license detected in preferred metadata
    if (!replaceLicense(updatedDataset.getLicense())) {
      LOG.warn("New dataset license {} cannot replace old license {}! Restoring old license.",
        updatedDataset.getLicense(), existingDataset.getLicense());
      updatedDataset.setLicense(existingDataset.getLicense());
    }

    return updatedDataset;
  }

  /**
   * Updates dataset by reinterpreting its preferred metadata document, if it exists.
   *
   * @param uuid the dataset to update
   * @param user the modifier
   */
  public void updateFromPreferredMetadata(UUID uuid, String user) {
    Dataset dataset = super.get(uuid);
    if (dataset == null) {
      throw new NotFoundException("Dataset " + uuid + " not existing");
    } else if (dataset.getDeleted() != null) {
      throw new NotFoundException("Dataset " + uuid + " has been deleted");
    }
    // retrieve preferred metadata document, if it exists
    Dataset updDataset = getPreferredMetadataDataset(uuid);
    if (updDataset != null) {
      updDataset = preserveGBIFDatasetProperties(updDataset, dataset);
      //keep the DOI only if none can be extracted from the metadata
      if (updDataset.getDoi() == null && dataset.getDoi() != null) {
        updDataset.setDoi(dataset.getDoi());
      }

      updDataset.setModifiedBy(user);
      updDataset.setModified(new Date());

      // persist contacts, overwriting any existing ones
      replaceContacts(uuid, updDataset.getContacts(), user);
      addIdentifiers(uuid, updDataset.getIdentifiers(), user);
      addTags(uuid, updDataset.getTags(), user);

      // now update the core dataset only, remove associated data to avoid confusion and potential validation problems
      updDataset.getContacts().clear();
      updDataset.getIdentifiers().clear();
      updDataset.getTags().clear();
      updDataset.getMachineTags().clear();

      update(updDataset);
    } else {
      LOG.debug("Dataset [key={}] has no preferred metadata document, skipping update!", uuid);
    }
  }

  private <T extends LenientEquals> boolean containedIn(T id, Collection<T> ids) {
    for (T id2 : ids) {
      if (id.lenientEquals(id2)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Add all not yet existing identifiers to the db!
   */
  private void addIdentifiers(UUID datasetKey, List<Identifier> newIdentifiers, String user) {
    List<Identifier> existing = datasetMapper.listIdentifiers(datasetKey);
    for (Identifier id : newIdentifiers) {
      if (IdentifierType.UNKNOWN != id.getType() && !containedIn(id, existing)) {
        // insert into db
        id.setCreatedBy(user);
        id.setCreated(new Date());
        withMyBatis.addIdentifier(identifierMapper, datasetMapper, datasetKey, id);
        // keep it in list for subsequent tests
        existing.add(id);
      }
    }
  }

  /**
   * Add all not yet existing identifiers to the db!
   */
  private void addTags(UUID datasetKey, List<Tag> newTags, String user) {
    List<Tag> existing = datasetMapper.listTags(datasetKey);
    for (Tag tag : newTags) {
      if (!containedIn(tag, existing)) {
        // insert into db
        tag.setCreatedBy(user);
        tag.setCreated(new Date());
        withMyBatis.addTag(tagMapper, datasetMapper, datasetKey, tag);
        // keep it in list for subsequent tests
        existing.add(tag);
      }
    }
  }

  private void replaceContacts(UUID datasetKey, List<Contact> contacts, String user) {
    // persist contacts, overwriting any existing ones
    datasetMapper.deleteContacts(datasetKey);
    for (Contact c : contacts) {
      c.setCreatedBy(user);
      c.setCreated(new Date());
      c.setModifiedBy(user);
      c.setModified(new Date());
      withMyBatis.addContact(contactMapper, datasetMapper, datasetKey, c);
    }
  }

  /**
   * Decide whether the current license should be overwritten based on following rule(s):
   * Only overwrite current license is overwriting license is a GBIF-supported license.
   *
   * @param license license
   */
  private boolean replaceLicense(@Nullable License license) {
    if (license == null) {
      return false;
    }
    return license.isConcrete();
  }

  /**
   * Set the generated GBIF citation on the provided Dataset object.
   * This function is used until we decide if we store the GBIF generated citation in the database.
   * <p>
   * see https://github.com/gbif/registry/issues/4
   *
   * @param dataset
   * @return
   */
  private void setGeneratedCitation(Dataset dataset) {
    if (dataset != null && dataset.getPublishingOrganizationKey() != null
      // for CoL and its constituents we want to show the verbatim citation and no GBIF generated one:
      // https://github.com/gbif/portal-feedback/issues/1819
      && !Constants.COL_DATASET_KEY.equals(dataset.getKey())
      && !Constants.COL_DATASET_KEY.equals(dataset.getParentDatasetKey())) {

      // if the citation already exists keep it and only change the text. That allows us to keep the identifier
      // if provided.
      Citation citation = dataset.getCitation() == null ? new Citation() : dataset.getCitation();
      citation.setText(CitationGenerator.generateCitation(dataset,
        ORGANIZATION_CACHE.getUnchecked(dataset.getPublishingOrganizationKey())));
      dataset.setCitation(citation);
    }
  }

  /**
   * Creates a new Dataset.
   * </br>
   * Before creating it, method:
   * 1. assigns it a {@link DOI} as per <a href="http://dev.gbif.org/issues/browse/POR-2554">GBIF DOI business rules</a>
   * 2. ensures it has a {@link License} as per <a href="http://dev.gbif.org/issues/browse/POR-3133">GBIF License business rules</a>
   */
  @Override
  public UUID create(@Validated({PrePersist.class, Default.class}) Dataset dataset) {
    if (dataset.getDoi() == null) {
      dataset.setDoi(doiGenerator.newDatasetDOI());
    }
    // Assign CC-BY 4.0 (default license) when license not specified yet
    // See https://github.com/gbif/registry/issues/71#issuecomment-438280021 for background on possibly changing this.
    if (dataset.getLicense() == null) {
      LOG.warn("Dataset created by {} {} with the V1 API does not specify a license, defaulting to CC_BY_4_0",
        dataset.getPublishingOrganizationKey(), dataset.getCreatedBy());
      dataset.setLicense(License.CC_BY_4_0);
    }

    final UUID key = super.create(dataset);
    // now that we have a UUID schedule to scheduleRegistration the DOI
    // to get the latest timestamps we need to read a new copy of the dataset
    doiHandlerStrategy.scheduleDatasetRegistration(dataset.getDoi(), doiHandlerStrategy.buildMetadata(get(key)), key);
    return key;
  }

  @Override
  public void update(@Validated({PostPersist.class, Default.class}) Dataset dataset) {
    Dataset old = super.get(dataset.getKey());
    if (old == null) {
      throw new IllegalArgumentException("Dataset " + dataset.getKey() + " not existing");
    }
    // replace current license? Only if dataset being updated has a supported license
    if (!replaceLicense(dataset.getLicense())) {
      LOG.warn("New dataset license {} cannot replace old license {}! Restoring old license.", dataset.getLicense(),
        old.getLicense());
      dataset.setLicense(old.getLicense());
    }
    update(dataset, old.getIdentifiers(), old.getDoi(), dataset.getModifiedBy());
  }

  /**
   * This method does a regular dataset update as defined in the super.update(), but also deals with setting, changing
   * or removing DOIs from the dataset.doi property and the list of attached identifiers.
   * <p>
   * DOI update logic:
   * <ul>
   *   <li>If oldDoi exists and the new DOI is the same nothing happens</li>
   *   <li>If oldDoi exists and the new DOI is different, the new one is used for the dataset and the old one is moved
   *   to the identifiers table. If the new DOI existed in the identifiers table it will be removed.</li>
   *   <li>If the dataset has no DOI and no oldDoi exists a new GBIF DOI is issued</li>
   *   <li>If the dataset has no DOI and the oldDoi is a GBIF DOI, the oldDoi is kept</li>
   *   <li>If the dataset has no DOI and the oldDoi is not a GBIF DOI, the oldDoi is moved to the identifiers table.
   *   In case the identifiers table already contains a GBIF DOI this is removed and used for the dataset. If there was
   *   no GBIF DOI yet a new one is issued</li>
   * </ul>
   * <p>
   * Also see http://dev.gbif.org/issues/browse/POR-2554 for a discussion.
   *
   * @param dataset     the dataset to be used to update the dataset table in postgres
   * @param existingIds the complete list of identifiers linked in postgres to the dataset before the update
   * @param oldDoi      the doi as found in postgres for the dataset before this update
   * @param user        the gbif user doing the update
   */
  private void update(Dataset dataset, List<Identifier> existingIds, @Nullable final DOI oldDoi, final String user) {
    // no need to parse EML for the DOI, just get the current mybatis dataset props
    if (dataset.getDoi() == null) {
      // a dataset must have a DOI. If it came in with none a GBIF DOI needs to exist
      if (oldDoi != null && doiHandlerStrategy.isUsingMyPrefix(oldDoi)) {
        dataset.setDoi(oldDoi);
      } else {
        // we have a non GBIF DOI before that we need to deprecate
        reactivatePreviousGbifDoiOrMintNew(existingIds, dataset);
        // add old DOI to list of alt identifiers
        if (oldDoi != null) {
          addDOIAsAlternateId(oldDoi, dataset.getKey(), user);
        }
      }
    } else if (oldDoi != null && !dataset.getDoi().equals(oldDoi)) {
      // the doi has changed. Add old DOI to list of alt identifiers
      addDOIAsAlternateId(oldDoi, dataset.getKey(), user);
      removeAltIdIfExists(dataset.getKey(), dataset.getDoi(), existingIds);
    }

    // update database for core dataset only
    super.update(dataset);

    // to get the latest timestamps we need to read a new copy of the dataset
    doiHandlerStrategy.datasetChanged(get(dataset.getKey()), oldDoi);
  }

  /**
   * Add old DOI to list of alt identifiers in dataset.
   */
  private void addDOIAsAlternateId(DOI altId, UUID datasetKey, String user) {
    // update alt ids of dataset
    Identifier id = new Identifier();
    id.setType(IdentifierType.DOI);
    id.setIdentifier(altId.toString());
    id.setCreatedBy(user);
    id.setCreated(new Date());
    LOG.info("DOI changed. Adding previous DOI {} to alternative identifier list for dataset {}", altId, datasetKey);
    withMyBatis.addIdentifier(identifierMapper, datasetMapper, datasetKey, id);
  }

  /**
   * Removes a DOI from the alternative identifiers list of a dataset if it exists.
   */
  private void removeAltIdIfExists(UUID key, DOI doiToRemove, List<Identifier> existingIds) {
    for (Identifier id : existingIds) {
      if (DOI.isParsable(id.getIdentifier())) {
        DOI doi = new DOI(id.getIdentifier());
        if (doiToRemove.equals(doi)) {
          // remove from id list
          datasetMapper.deleteIdentifier(key, id.getKey());
        }
      }
    }
  }

  /**
   * Scan list of alternate identifiers to find a previous, deleted GBIF DOI and update the dataset instance.
   * If none can be found use a newly generated one.
   */
  private void reactivatePreviousGbifDoiOrMintNew(List<Identifier> existingIds, Dataset d) {
    for (Identifier id : existingIds) {
      if (DOI.isParsable(id.getIdentifier())) {
        DOI doi = new DOI(id.getIdentifier());
        if (doiHandlerStrategy.isUsingMyPrefix(doi)) {
          // remove from id list and make primary DOI
          LOG.info("Reactivating old GBIF DOI {} for dataset {}", doi, d.getKey());
          datasetMapper.deleteIdentifier(d.getKey(), id.getKey());
          d.setDoi(doi);
          return;
        }
      }
    }
    // we never had a GBIF DOI for this dataset, give it a new one
    DOI doi = doiGenerator.newDatasetDOI();
    LOG.info("Create new GBIF DOI {} for dataset {}", doi, d.getKey());
    d.setDoi(doi);
  }

  /**
   * Sanitize data on Dataset object mainly to restrict HTML tags that can be used.
   *
   * @param dataset
   * @return the original dataset with its content sanitized
   */
  private Dataset sanitizeDataset(Dataset dataset) {
    if (!Strings.isNullOrEmpty(dataset.getDescription())) {
      dataset.setDescription(PARAGRAPH_HTML_SANITIZER.sanitize(dataset.getDescription()));
    }
    return dataset;
  }

  /**
   * We need to implement this interface method here, but there is no way to retrieve the actual user
   * as we cannot access any http request. The real server method does this correctly but has more parameters.
   */
  @Override
  public Metadata insertMetadata(@PathVariable("key") UUID datasetKey, InputStream document) {
    // this method should never be called but from tests
    return insertMetadata(datasetKey, document, "UNKNOWN USER");
  }

  @GetMapping("{key}/constituents")
  @Override
  public PagingResponse<Dataset> listConstituents(@PathVariable("key") UUID datasetKey, Pageable page) {
    return pagingResponse(page, (long) datasetMapper.countConstituents(datasetKey),
      datasetMapper.listConstituents(datasetKey, page));
  }

  @GetMapping("{key}/networks")
  @Override
  public List<Network> listNetworks(@PathVariable("key") UUID datasetKey) {
    return networkMapper.listByDataset(datasetKey);
  }

  @GetMapping("constituents")
  @Override
  public PagingResponse<Dataset> listConstituents(Pageable page) {
    return pagingResponse(page, datasetMapper.countSubdatasets(), datasetMapper.subdatasets(page));
  }

  @GetMapping("{key}/metadata")
  @Override
  public List<Metadata> listMetadata(@PathVariable("key") UUID datasetKey, @RequestParam("type") MetadataType type) {
    return metadataMapper.list(datasetKey, type);
  }

  @GetMapping("metadata/{key}")
  @Override
  @NullToNotFound
  public Metadata getMetadata(@PathVariable("key") int metadataKey) {
    return metadataMapper.get(metadataKey);
  }

  @GetMapping(value = "metadata/{key}/document", produces = MediaType.APPLICATION_XML_VALUE)
  @Override
  @NullToNotFound
  public InputStream getMetadataDocument(@PathVariable("key") int metadataKey) {
    ByteArrayWrapper document = metadataMapper.getDocument(metadataKey);
    if (document == null) {
      return null;
    }
    return new ByteArrayInputStream(document.getData());
  }

  @DeleteMapping("metadata/{key}")
  @Override
  public void deleteMetadata(@PathVariable("key") int metadataKey) {
    metadataMapper.delete(metadataKey);
  }

  @GetMapping("deleted")
  @Override
  public PagingResponse<Dataset> listDeleted(Pageable page) {
    return pagingResponse(page, datasetMapper.countDeleted(), datasetMapper.deleted(page));
  }

  @GetMapping("duplicate")
  @Override
  public PagingResponse<Dataset> listDuplicates(Pageable page) {
    return pagingResponse(page, datasetMapper.countDuplicates(), datasetMapper.duplicates(page));
  }

  @GetMapping("withNoEndpoint")
  @Override
  public PagingResponse<Dataset> listDatasetsWithNoEndpoint(Pageable page) {
    return pagingResponse(page, datasetMapper.countWithNoEndpoint(), datasetMapper.withNoEndpoint(page));
  }

  /**
   * Utility method to run batch jobs on all dataset elements
   */
  private void doOnAllOccurrenceDatasets(Consumer<Dataset> onDataset) {
    PagingRequest pagingRequest = new PagingRequest(0, ALL_DATASETS_LIMIT);
    PagingResponse<Dataset> response = listByType(DatasetType.OCCURRENCE, pagingRequest);
    do {
      response
        .getResults()
        .forEach(
          d -> {
            try {
              LOG.info("trying to crawl dataset {}", d.getKey());
              onDataset.accept(d);
            } catch (Exception ex) {
              LOG.error("Error processing dataset {} while crawling all: {}", d.getKey(), ex.getMessage());
            }
          });
      pagingRequest.addOffset(response.getResults().size());
      response = listByType(DatasetType.OCCURRENCE, pagingRequest);
    } while (!response.isEndOfRecords());
  }

  /**
   * This is a REST only (e.g. not part of the Java API) method that allows the registry console to trigger the
   * crawling of the dataset. This simply emits a message to rabbitmq requesting the crawl, and applies
   * necessary security.
   */
  @PostMapping("crawlall")
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public void crawlAll(@RequestParam("platform") String platform) {
    CompletableFuture.runAsync(
      () -> doOnAllOccurrenceDatasets(dataset -> crawl(dataset.getKey(), platform)));
  }

  /**
   * This is a REST only (e.g. not part of the Java API) method that allows the registry console to trigger the
   * crawling of the dataset. This simply emits a message to rabbitmq requesting the crawl, and applies
   * necessary security.
   */
  @PostMapping("{key}/crawl")
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public void crawl(@PathVariable("key") UUID datasetKey, @RequestParam("platform") String platform) {
    Platform indexingPlatform = Platform.parse(platform).orElse(Platform.ALL);
    if (messagePublisher != null) {
      LOG.info("Requesting crawl of dataset[{}]", datasetKey);
      try {
        // we'll bump this to the top of the queue since it is a user initiated
        messagePublisher.send(new StartCrawlMessage(datasetKey, StartCrawlMessage.Priority.CRITICAL.getPriority(), indexingPlatform));
      } catch (IOException e) {
        LOG.error("Unable to send message requesting crawl", e);
      }
    } else {
      LOG.warn("Registry is configured to run without messaging capabilities. Unable to crawl dataset[{}]", datasetKey);
    }
  }

  @PostMapping("{datasetKey}/process")
  @Trim
  @Transactional
  @Secured(ADMIN_ROLE)
  public void createDatasetProcessStatus(@PathVariable UUID datasetKey,
                                         @RequestBody @Valid @NotNull @Trim DatasetProcessStatus datasetProcessStatus) {
    checkArgument(datasetKey.equals(datasetProcessStatus.getDatasetKey()),
      "DatasetProcessStatus must have the same key as the dataset");
    createDatasetProcessStatus(datasetProcessStatus);
  }

  @Trim
  @Transactional
  @Secured(ADMIN_ROLE)
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

  @PutMapping("{datasetKey}/process/{attempt}")
  @Trim
  @Transactional
  @Secured(ADMIN_ROLE)
  public void updateDatasetProcessStatus(@PathVariable UUID datasetKey, @PathVariable int attempt,
                                         @RequestBody @Valid @NotNull @Trim DatasetProcessStatus datasetProcessStatus) {
    checkArgument(datasetKey.equals(datasetProcessStatus.getDatasetKey()),
      "DatasetProcessStatus must have the same key as the url");
    checkArgument(attempt == datasetProcessStatus.getCrawlJob().getAttempt(),
      "DatasetProcessStatus must have the same attempt as the url");
    updateDatasetProcessStatus(datasetProcessStatus);
  }

  @Trim
  @Transactional
  @Secured(ADMIN_ROLE)
  @Override
  public void updateDatasetProcessStatus(@Valid @NotNull @Trim DatasetProcessStatus datasetProcessStatus) {
    datasetProcessStatusMapper.update(datasetProcessStatus);
  }

  @GetMapping("{datasetKey}/process/{attempt}")
  @Nullable
  @NullToNotFound
  @Override
  public DatasetProcessStatus getDatasetProcessStatus(@PathVariable UUID datasetKey, @PathVariable int attempt) {
    return datasetProcessStatusMapper.get(datasetKey, attempt);
  }

  @GetMapping("process")
  @Override
  public PagingResponse<DatasetProcessStatus> listDatasetProcessStatus(Pageable page) {
    return new PagingResponse<>(page, (long) datasetProcessStatusMapper.count(),
      datasetProcessStatusMapper.list(page));
  }

  @GetMapping("process/aborted")
  @Override
  public PagingResponse<DatasetProcessStatus> listAbortedDatasetProcesses(Pageable page) {
    return new PagingResponse<>(page, (long) datasetProcessStatusMapper.countAborted(),
      datasetProcessStatusMapper.listAborted(page));
  }

  @GetMapping("{datasetKey}/process")
  @Override
  public PagingResponse<DatasetProcessStatus> listDatasetProcessStatus(@PathVariable UUID datasetKey, Pageable page) {
    return new PagingResponse<>(page, (long) datasetProcessStatusMapper.countByDataset(datasetKey),
      datasetProcessStatusMapper.listByDataset(datasetKey, page));
  }

  @Override
  protected List<UUID> owningEntityKeys(@NotNull Dataset entity) {
    List<UUID> keys = new ArrayList<>();
    keys.add(entity.getPublishingOrganizationKey());
    keys.add(ORGANIZATION_CACHE.getUnchecked(entity.getPublishingOrganizationKey()).getEndorsingNodeKey());
    return keys;
  }

  @GetMapping("doi/{doi:.+}")
  @Override
  public PagingResponse<Dataset> listByDOI(@PathVariable String doi, Pageable page) {
    return new PagingResponse<>(page, datasetMapper.countByDOI(doi), datasetMapper.listByDOI(doi, page));
  }
}
