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
package org.gbif.registry.ws.resources;

import org.gbif.api.annotation.NullToNotFound;
import org.gbif.api.annotation.Trim;
import org.gbif.api.documentation.CommonParameters;
import org.gbif.api.exception.ServiceUnavailableException;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.common.search.FacetedSearchRequest;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.model.crawler.DatasetProcessStatus;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Grid;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.LenientEquals;
import org.gbif.api.model.registry.Metadata;
import org.gbif.api.model.registry.Network;
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
import org.gbif.api.util.Range;
import org.gbif.api.util.iterables.Iterables;
import org.gbif.api.vocabulary.Continent;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetSubtype;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.License;
import org.gbif.api.vocabulary.MetadataType;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.Platform;
import org.gbif.common.messaging.api.messages.StartCrawlMessage;
import org.gbif.metadata.common.util.MetadataUtils;
import org.gbif.metadata.eml.EMLWriter;
import org.gbif.registry.doi.DataCiteMetadataBuilderService;
import org.gbif.registry.doi.DatasetDoiDataCiteHandlingService;
import org.gbif.registry.doi.DoiIssuingService;
import org.gbif.registry.domain.ws.DatasetRequestSearchParams;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.persistence.mapper.ContactMapper;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.DatasetProcessStatusMapper;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MetadataMapper;
import org.gbif.registry.persistence.mapper.NetworkMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.service.MapperServiceLocator;
import org.gbif.registry.service.RegistryDatasetService;
import org.gbif.registry.service.WithMyBatis;
import org.gbif.registry.ws.export.CsvWriter;
import org.gbif.ws.NotFoundException;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
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

import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.gbif.registry.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.security.UserRoles.EDITOR_ROLE;
import static org.gbif.registry.security.UserRoles.IPT_ROLE;

@SuppressWarnings("UnstableApiUsage")
@io.swagger.v3.oas.annotations.tags.Tag(
  name = "Datasets",
  description = "A GBIF **dataset** provides occurrence data, checklist data, sampling event data or metadata. " +
    "Publishing organizations register datasets in this Registry, and the data they reference is retrieved and " +
    "indexed in GBIF's occurrence store on a regular schedule.\n\n" +
    "Metadata of datasets follows the GBIF Metadata Profile.\n\n" +
    "The dataset API provides CRUD and discovery services for datasets. Its most prominent use on the GBIF " +
    "portal is to drive the [dataset search](https://www.gbif.org/dataset/search) and dataset pages.\n\n" +
    "Please note deletion of datasets is logical, meaning dataset entries remain registered forever and only get a " +
    "deleted timestamp. On the other hand, deletion of a dataset's contacts, endpoints, identifiers, tags, " +
    "machine tags, comments, and metadata descriptions is physical, meaning the entries are permanently removed.",
  extensions = @io.swagger.v3.oas.annotations.extensions.Extension(
    name = "Order", properties = @ExtensionProperty(name = "Order", value = "0100")))
@Validated
@Primary
@RestController
@RequestMapping(value = "dataset", produces = MediaType.APPLICATION_JSON_VALUE)
public class DatasetResource extends BaseNetworkEntityResource<Dataset>
    implements DatasetService, DatasetSearchService, DatasetProcessStatusService {

  private static final Logger LOG = LoggerFactory.getLogger(DatasetResource.class);

  private static final int ALL_DATASETS_LIMIT = 200;

  // Page size to iterate over search export service
  private static final int SEARCH_EXPORT_LIMIT = 300;

  // Search export file header
  private static final String EXPORT_FILE_PRE = "attachment; filename=gbif_datasets.";

  private final RegistryDatasetService registryDatasetService;
  private final DatasetSearchService searchService;
  private final MetadataMapper metadataMapper;
  private final DatasetMapper datasetMapper;
  private final ContactMapper contactMapper;
  private final IdentifierMapper identifierMapper;
  private final TagMapper tagMapper;
  private final NetworkMapper networkMapper;
  private final DatasetProcessStatusMapper datasetProcessStatusMapper;
  private final DatasetDoiDataCiteHandlingService doiDataCiteHandlingService;
  private final DataCiteMetadataBuilderService metadataBuilderService;
  private final DoiIssuingService doiIssuingService;
  private final WithMyBatis withMyBatis;
  private final EMLWriter emlWriter;

  // The messagePublisher can be optional
  private final MessagePublisher messagePublisher;

  public DatasetResource(
      MapperServiceLocator mapperServiceLocator,
      EventManager eventManager,
      RegistryDatasetService registryDatasetService,
      @Qualifier("datasetSearchServiceEs") DatasetSearchService searchService,
      DatasetDoiDataCiteHandlingService doiDataCiteHandlingService,
      DataCiteMetadataBuilderService metadataBuilderService,
      DoiIssuingService doiIssuingService,
      WithMyBatis withMyBatis,
      @Autowired(required = false) MessagePublisher messagePublisher) {
    super(
        mapperServiceLocator.getDatasetMapper(),
        mapperServiceLocator,
        Dataset.class,
        eventManager,
        withMyBatis);
    this.registryDatasetService = registryDatasetService;
    this.searchService = searchService;
    this.metadataMapper = mapperServiceLocator.getMetadataMapper();
    this.datasetMapper = mapperServiceLocator.getDatasetMapper();
    this.contactMapper = mapperServiceLocator.getContactMapper();
    this.identifierMapper = mapperServiceLocator.getIdentifierMapper();
    this.tagMapper = mapperServiceLocator.getTagMapper();
    this.datasetProcessStatusMapper = mapperServiceLocator.getDatasetProcessStatusMapper();
    this.networkMapper = mapperServiceLocator.getNetworkMapper();
    this.doiDataCiteHandlingService = doiDataCiteHandlingService;
    this.metadataBuilderService = metadataBuilderService;
    this.doiIssuingService = doiIssuingService;
    this.messagePublisher = messagePublisher;
    this.withMyBatis = withMyBatis;
    this.emlWriter = EMLWriter.newInstance(false);
  }

  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @Parameters(
    value = {
      @Parameter(
        name = "country",
        description = "The 2-letter country code (as per ISO-3166-1) of the country publishing the dataset.",
        schema = @Schema(implementation = Country.class),
        in = ParameterIn.QUERY,
        explode = Explode.FALSE),
      @Parameter(
        name = "type",
        description = "The primary type of the dataset.",
        schema = @Schema(implementation = DatasetType.class),
        in = ParameterIn.QUERY,
        explode = Explode.TRUE),
      @Parameter(
        name = "subtype",
        description = "The sub-type of the dataset.",
        schema = @Schema(implementation = DatasetSubtype.class),
        in = ParameterIn.QUERY,
        explode = Explode.TRUE),
      @Parameter(
        name = "license",
        description = "The dataset's licence.",
        schema = @Schema(implementation = License.class),
        in = ParameterIn.QUERY,
        explode = Explode.TRUE),
      @Parameter(
        name = "identifier",
        description = "An identifier such as a DOI or UUID.",
        schema = @Schema(implementation = String.class),
        in = ParameterIn.QUERY),
      @Parameter(
        name = "keyword",
        description = "Filters datasets by a case insensitive plain text keyword. The search is done on the merged " +
          "collection of tags, the dataset keywordCollections and temporalCoverages.",
        schema = @Schema(implementation = String.class),
        in = ParameterIn.QUERY),
      @Parameter(
        name = "publishingOrg",
        description = "Filters datasets by their publishing organization UUID key",
        schema = @Schema(implementation = UUID.class),
        in = ParameterIn.QUERY),
      @Parameter(
        name = "hostingOrg",
        description = "Filters datasets by their hosting organization UUID key",
        schema = @Schema(implementation = UUID.class),
        in = ParameterIn.QUERY),
      @Parameter(
        name = "endorsingNodeKey",
        description = "Node key that endorsed this dataset's publisher",
        schema = @Schema(implementation = UUID.class),
        in = ParameterIn.QUERY),
      @Parameter(
        name = "decade",
        description = "Filters datasets by their temporal coverage broken down to decades. Decades are given as a full " +
          "year, e.g. 1880, 1960, 2000, etc, and will return datasets wholly contained in the decade as well as those " +
          "that cover the entire decade or more. Facet by decade to get the break down, i.e. `facet=DECADE&limit=0`",
        schema = @Schema(implementation = Short.class),
        in = ParameterIn.QUERY),
      @Parameter(
        name = "publishingCountry",
        description = "Filters datasets by their owning organization's country given as a ISO 639-1 (2 letter) country code",
        schema = @Schema(implementation = Country.class),
        in = ParameterIn.QUERY,
        explode = Explode.FALSE),
      @Parameter(
        name = "projectId",
        description = "Filter or facet based on the project ID of a given dataset. A dataset can have a project id if " +
          "it is the result of a project. multiple datasets can have the same project id.",
        schema = @Schema(implementation = String.class),
        in = ParameterIn.QUERY,
        example = "AA003-AA003311F"),
      @Parameter(
        name = "hostingCountry",
        description = "Filters datasets by their hosting organization's country given as a ISO 639-1 (2 letter) country code",
        schema = @Schema(implementation = Country.class),
        in = ParameterIn.QUERY,
        explode = Explode.FALSE),
      @Parameter(
        name = "continent",
        description = "Not implemented.",
        schema = @Schema(implementation = Continent.class),
        in = ParameterIn.QUERY,
        deprecated = true,
        explode = Explode.FALSE),
      @Parameter(
        name = "networkKey",
        description = "Network associated to a dataset",
        schema = @Schema(implementation = UUID.class),
        in = ParameterIn.QUERY),
      @Parameter(
        name = "request",
        hidden = true
      ),
      @Parameter(
        name = "searchRequest",
        hidden = true
      ),
      @Parameter(
        name = "suggestRequest",
        hidden = true
      )
    })
  @interface DatasetSearchParameters {}

  @Operation(
    operationId = "searchDatasets",
    summary = "Search across all datasets.",
    description = "Full-text search across all datasets. Results are ordered by relevance.",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0101")))
  @DatasetSearchParameters
  @CommonParameters.QParameter
  @CommonParameters.HighlightParameter
  @FacetedSearchRequest.FacetParameters
  @Pageable.OffsetLimitParameters
  @ApiResponse(
    responseCode = "200",
    description = "Dataset search successful")
  @ApiResponse(
    responseCode = "400",
    description = "Invalid search query provided")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("search")
  @Override
  public SearchResponse<DatasetSearchResult, DatasetSearchParameter> search(
      DatasetSearchRequest searchRequest) {
    return searchService.search(searchRequest);
  }

  @Operation(
    operationId = "searchDatasetsExport",
    summary = "Export search across all datasets.",
    description = "Download full-text search results as CSV or TSV.",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0102")))
  @DatasetSearchParameters
  @CommonParameters.QParameter
  @ApiResponse(
    responseCode = "200",
    description = "Dataset search successful")
  @ApiResponse(
    responseCode = "400",
    description = "Invalid search query provided")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("search/export")
  public void search(
      HttpServletResponse response,
      @RequestParam(value = "format", defaultValue = "TSV") ExportFormat format,
      DatasetSearchRequest searchRequest)
      throws IOException {

    response.setHeader(
        HttpHeaders.CONTENT_DISPOSITION, EXPORT_FILE_PRE + format.name().toLowerCase());

    try (Writer writer = new BufferedWriter(new OutputStreamWriter(response.getOutputStream()))) {
      CsvWriter.datasetSearchResultCsvWriter(
              Iterables.datasetSearchResults(searchRequest, searchService, SEARCH_EXPORT_LIMIT),
              format)
          .export(writer);
    }
  }

  @Operation(
    operationId = "suggestDatasets",
    summary = "Suggest datasets.",
    description = "Search that returns up to 20 matching datasets. Results are ordered by relevance. " +
      "The response is smaller than a dataset search.",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0103")))
  @DatasetSearchParameters
  @CommonParameters.QParameter
  @ApiResponse(
    responseCode = "200",
    description = "Dataset search successful")
  @ApiResponse(
    responseCode = "400",
    description = "Invalid search query provided")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("suggest")
  @Override
  public List<DatasetSuggestResult> suggest(DatasetSuggestRequest suggestRequest) {
    return searchService.suggest(suggestRequest);
  }

  @Operation(
    operationId = "getDataset",
    summary = "Get details of a single dataset",
    description = "Details of a single dataset.  Also works for deleted datasets.",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0110")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(
    responseCode = "200",
    description = "Dataset found and returned")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{key}")
  @NullToNotFound("/dataset/{key}")
  @Override
  public Dataset get(@PathVariable UUID key) {
    return registryDatasetService.get(key);
  }

  /**
   * All network entities support simple (!) search with "&q=". This is to support the console user
   * interface, and is in addition to any complex, faceted search that might additionally be
   * supported, such as dataset search.
   */
  @Operation(
      operationId = "listDatasets",
      summary = "List all datasets",
      description = "Lists all current datasets (deleted datasets are not listed).",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0100")))
  @Parameters(
      value = {
        @Parameter(
            name = "country",
            description =
                "The 2-letter country code (as per ISO-3166-1) of the country publishing the dataset.",
            schema = @Schema(implementation = Country.class),
            in = ParameterIn.QUERY,
            explode = Explode.FALSE),
        @Parameter(
            name = "type",
            description = "The primary type of the dataset.",
            schema = @Schema(implementation = DatasetType.class),
            in = ParameterIn.QUERY,
            explode = Explode.TRUE),
        @Parameter(
            name = "modified",
            description =
                "The modified date of the dataset. Accepts ranges and a '*' can be used as a wildcard, e.g.:modified=2023-04-01,*",
            schema = @Schema(implementation = DatasetType.class),
            in = ParameterIn.QUERY,
            explode = Explode.TRUE)
      })
  @SimpleSearchParameters
  @ApiResponse(responseCode = "200", description = "Dataset search successful")
  @ApiResponse(responseCode = "400", description = "Invalid search query provided")
  @GetMapping
  public PagingResponse<Dataset> list(
      @Nullable Country country, @Valid DatasetRequestSearchParams request, Pageable page) {
    if (country != null || request.getType() != null || request.getModified() != null) {
      return listInternal(country, request.getType(), request.getModified(), page);
    } else if (request.getIdentifier() != null) {
      return listByIdentifier(request.getIdentifier(), page);
    } else if (request.getMachineTagNamespace() != null) {
      return listByMachineTag(
          request.getMachineTagNamespace(),
          request.getMachineTagName(),
          request.getMachineTagValue(),
          page);
    } else if (!Strings.isNullOrEmpty(request.getQ())) {
      return search(request.getQ(), page);
    } else {
      return list(page);
    }
  }

  private PagingResponse<Dataset> listInternal(
      Country country, DatasetType type, Range<LocalDate> modified, Pageable page) {
    Date from =
        modified != null && modified.lowerEndpoint() != null
            ? Date.from(modified.lowerEndpoint().atStartOfDay(ZoneId.systemDefault()).toInstant())
            : null;
    Date to =
        modified != null && modified.upperEndpoint() != null
            ? Date.from(modified.upperEndpoint().atStartOfDay(ZoneId.systemDefault()).toInstant())
            : null;
    long total = datasetMapper.countWithFilter(country, type, null, from, to);
    return pagingResponse(
        page, total, datasetMapper.listWithFilter(country, type, null, from, to, page));
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
    return registryDatasetService.augmentWithMetadata(super.search(query, page));
  }

  @Override
  public PagingResponse<Dataset> list(Pageable page) {
    PagingResponse<Dataset> datasets = super.list(page);
    return registryDatasetService.augmentWithMetadata(datasets);
  }

  @Override
  public InputStream getMetadataDocument(UUID datasetKey) {
    byte[] bytes = getMetadataDocumentAsBytes(datasetKey);

    if (bytes != null) {
      return new ByteArrayInputStream(bytes);
    }

    return null;
  }

  @Operation(
    operationId = "getDocuments",
    summary = "Retrieve GBIF metadata document of the dataset",
    description = "Gets a GBIF generated EML document overlaying GBIF information with any existing metadata document data.",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0300")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(
    responseCode = "200",
    description = "GBIF metadata documents")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping(value = "{key}/document", produces = MediaType.APPLICATION_XML_VALUE)
  public byte[] getMetadataDocumentAsBytes(@PathVariable("key") UUID datasetKey) {
    // the fully augmented dataset
    Dataset dataset = get(datasetKey);
    if (dataset != null) {
      // generate new EML
      try {
        StringWriter eml = new StringWriter();
        emlWriter.writeTo(dataset, eml);
        return eml.toString().getBytes(StandardCharsets.UTF_8);
      } catch (Exception e) {
        throw new ServiceUnavailableException("Failed to serialize dataset " + datasetKey, e);
      }
    }
    return null;
  }

  @Operation(
    operationId = "deleteDataset",
    summary = "Delete an existing dataset",
    description = "Deletes an existing dataset. The dataset entry gets a deleted timestamp but remains registered.",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0203")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(
    responseCode = "204",
    description = "Dataset marked as deleted")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @DeleteMapping("{key}")
  @Secured({ADMIN_ROLE, EDITOR_ROLE, IPT_ROLE})
  @Transactional
  @Override
  public void delete(@PathVariable UUID key) {
    Dataset dataset = get(key);
    super.delete(key);

    if (dataset != null && dataset.getDoi() != null) {
      doiDataCiteHandlingService.datasetDeleted(dataset.getDoi());
    }
  }

  @Operation(
    operationId = "addDocument",
    summary = "Add a metadata document to the record",
    description = "Pushes a new original source metadata document for a dataset into the registry, replacing any " +
      "previously existing document of the same type.",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0301")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(
    responseCode = "200",
    description = "Metadata document added, metadata document identifier returned")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping(value = "{key}/document", consumes = MediaType.APPLICATION_XML_VALUE)
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public Metadata insertMetadata(
      @PathVariable("key") UUID datasetKey, @RequestBody byte[] document) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return insertMetadata(datasetKey, new ByteArrayInputStream(document), authentication.getName());
  }

  private Metadata insertMetadata(UUID datasetKey, InputStream document, String user) {
    // check if the dataset actually exists
    Dataset dataset = super.get(datasetKey);
    if (dataset == null) {
      throw new NotFoundException(
          "Dataset " + datasetKey + " not existing", URI.create("/dataset/{key}/document"));
    } else if (dataset.getDeleted() != null) {
      throw new NotFoundException(
          "Dataset " + datasetKey + " has been deleted", URI.create("/dataset/{key}/document"));
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
      type = MetadataUtils.detectParserType(in);
      // TODO: should we not also validate the EML/DC document ???
    } catch (IOException e) {
      throw new IllegalArgumentException("Unreadable document", e);
    }

    // first, determine if this document is already stored, returning it with no action
    // we do this, because updating metadata when nothing has changed, results in registry change
    // events being
    // propagated which can trigger crawlers which will run an update etc.
    List<Metadata> existingDocs = listMetadata(datasetKey, type);
    for (Metadata existing : existingDocs) {
      try (InputStream in = getMetadataDocument(existing.getKey())) {
        String existingContent =
            CharStreams.toString(new InputStreamReader(in, StandardCharsets.UTF_8));
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
    // first remove all existing metadata of the same type (so we end up storing only one document
    // per type)
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
          "Dataset {} locked for automatic updates. Uploaded metadata document does not modify registered dataset information",
          datasetKey);

    } else {
      // we retrieve the preferred document and only update if this new metadata is the preferred
      // one
      // e.g. we could put a DC document while an EML document exists that takes preference
      updateFromPreferredMetadata(datasetKey, user);
      LOG.info(
          "Dataset {} updated with base information from metadata document {}",
          datasetKey,
          metaKey);
    }

    return metadata;
  }

  /**
   * When we get a new Metadata document, this method is responsible to preserve the GBIF properties
   * on the dataset object to make sure they are not overwritten.
   *
   * @param updatedDataset normally instantiated from a metadata document
   * @param existingDataset current {@link Dataset} object from the database.
   * @return same instance as updatedDataset but with GBIF properties preserved (taken from
   *     existingDataset)
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
      LOG.warn(
          "New dataset license {} cannot replace old license {}! Restoring old license.",
          updatedDataset.getLicense(),
          existingDataset.getLicense());
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
      throw new NotFoundException(
          "Dataset " + uuid + " not existing", URI.create("/dataset/{key}/document"));
    } else if (dataset.getDeleted() != null) {
      throw new NotFoundException(
          "Dataset " + uuid + " has been deleted", URI.create("/dataset/{key}/document"));
    }
    // retrieve preferred metadata document, if it exists
    Dataset updDataset = registryDatasetService.getPreferredMetadataDataset(uuid);
    if (updDataset != null) {
      updDataset = preserveGBIFDatasetProperties(updDataset, dataset);
      // keep the DOI only if none can be extracted from the metadata
      if (updDataset.getDoi() == null && dataset.getDoi() != null) {
        updDataset.setDoi(dataset.getDoi());
      }

      updDataset.setModifiedBy(user);
      updDataset.setModified(new Date());

      // persist contacts, overwriting any existing ones
      replaceContacts(uuid, updDataset.getContacts(), user);
      addIdentifiers(uuid, updDataset.getIdentifiers(), user);
      addTags(uuid, updDataset.getTags(), user);

      // now update the core dataset only, remove associated data to avoid confusion and potential
      // validation problems
      updDataset.getContacts().clear();
      updDataset.getIdentifiers().clear();
      updDataset.getTags().clear();
      updDataset.getMachineTags().clear();

      update(updDataset);
    } else {
      LOG.debug("Dataset [key={}] has no preferred metadata document, skipping update!", uuid);
    }
  }

  private <T extends LenientEquals<T>> boolean containedIn(T id, Collection<T> ids) {
    for (T id2 : ids) {
      if (id.lenientEquals(id2)) {
        return true;
      }
    }
    return false;
  }

  /** Add all not yet existing identifiers to the db! */
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

  /** Add all not yet existing identifiers to the db! */
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
   * Decide whether the current license should be overwritten based on following rule(s): Only
   * overwrite current license is overwriting license is a GBIF-supported license.
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
   * Creates a new Dataset. </br> Before creating it, method: 1. assigns it a {@link DOI} as per <a
   * href="http://dev.gbif.org/issues/browse/POR-2554">GBIF DOI business rules</a> 2. ensures it has
   * a {@link License} as per <a href="http://dev.gbif.org/issues/browse/POR-3133">GBIF License
   * business rules</a>
   */
  @Operation(
    operationId = "createDataset",
    summary = "Create a new dataset",
    description = "Creates a new dataset.  Note contacts, endpoints, identifiers, tags, machine tags, comments and " +
      "metadata descriptions must be added in subsequent requests.",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0201")))
  @ApiResponse(
    responseCode = "201",
    description = "Dataset created, new dataset's UUID returned")
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PrePersist.class, Default.class})
  @Trim
  @Transactional
  @Secured({ADMIN_ROLE, EDITOR_ROLE, IPT_ROLE})
  @Override
  public UUID create(@RequestBody @Trim Dataset dataset) {
    if (dataset.getDoi() == null) {
      dataset.setDoi(doiIssuingService.newDatasetDOI());
    }
    // Assign CC-BY 4.0 (default license) when license not specified yet
    // See https://github.com/gbif/registry/issues/71#issuecomment-438280021 for background on
    // possibly changing this.
    if (dataset.getLicense() == null) {
      LOG.warn(
          "Dataset created by {} {} with the V1 API does not specify a license, defaulting to CC_BY_4_0",
          dataset.getPublishingOrganizationKey(),
          dataset.getCreatedBy());
      dataset.setLicense(License.CC_BY_4_0);
    }

    final UUID key = super.create(dataset);
    // now that we have a UUID schedule to scheduleRegistration the DOI
    // to get the latest timestamps we need to read a new copy of the dataset
    doiDataCiteHandlingService.scheduleDatasetRegistration(
        dataset.getDoi(), metadataBuilderService.buildMetadata(get(key)), key);
    return key;
  }

  /**
   * Updates the dataset.
   *
   * @param dataset dataset
   */
  // Method overridden only for documentation.
  @Operation(
    operationId = "updateDataset",
    summary = "Update an existing dataset",
    description = "Updates the existing dataset.  Note contacts, endpoints, identifiers, tags, machine tags, comments and " +
      "metadata descriptions are not changed with this method.",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0202")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(
    responseCode = "204",
    description = "Dataset updated")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PutMapping(value = "{key}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PostPersist.class, Default.class})
  @Override
  public void update(@PathVariable("key") UUID key, @Valid @RequestBody @Trim Dataset dataset) {
    super.update(key, dataset);
  }

  @Override
  public void update(Dataset dataset) {
    Dataset old = super.get(dataset.getKey());
    if (old == null) {
      throw new IllegalArgumentException("Dataset " + dataset.getKey() + " not existing");
    }
    // replace current license? Only if dataset being updated has a supported license
    if (!replaceLicense(dataset.getLicense())) {
      LOG.warn(
          "New dataset license {} cannot replace old license {}! Restoring old license.",
          dataset.getLicense(),
          old.getLicense());
      dataset.setLicense(old.getLicense());
    }

    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String user = null;
    // this method is also used by some CLIs and in these cases the authenticated user can be null
    if (authentication != null) {
      user = authentication.getName();
    } else if (!Strings.isNullOrEmpty(dataset.getModifiedBy())) {
      // this can also be null since it's not a required field
      user = dataset.getModifiedBy();
    } else {
      user = old.getModifiedBy();
    }

    update(dataset, old, user);
  }

  /**
   * This method does a regular dataset update as defined in the super.update(), but also deals with
   * setting, changing or removing DOIs from the dataset.doi property and the list of attached
   * identifiers.
   *
   * <p>DOI update logic:
   *
   * <ul>
   *   <li>If oldDoi exists and the new DOI is the same nothing happens
   *   <li>If oldDoi exists and the new DOI is different, the new one is used for the dataset and
   *       the old one is moved to the identifiers table. If the new DOI existed in the identifiers
   *       table it will be removed.
   *   <li>If the dataset has no DOI and no oldDoi exists a new GBIF DOI is issued
   *   <li>If the dataset has no DOI and the oldDoi is a GBIF DOI, the oldDoi is kept
   *   <li>If the dataset has no DOI and the oldDoi is not a GBIF DOI, the oldDoi is moved to the
   *       identifiers table. In case the identifiers table already contains a GBIF DOI this is
   *       removed and used for the dataset. If there was no GBIF DOI yet a new one is issued
   * </ul>
   *
   * <p>Also see http://dev.gbif.org/issues/browse/POR-2554 for a discussion.
   *
   * @param dataset the dataset to be used to update the dataset table in postgres
   * @param oldDataset the current dataset before update
   * @param user the gbif user doing the update
   */
  private void update(Dataset dataset, Dataset oldDataset, String user) {
    DOI oldDoi = oldDataset.getDoi();
    List<Identifier> existingIds = oldDataset.getIdentifiers();

    // no need to parse EML for the DOI, just get the current mybatis dataset props
    if (dataset.getDoi() == null) {
      // a dataset must have a DOI. If it came in with none a GBIF DOI needs to exist
      if (oldDoi != null && doiIssuingService.isGbif(oldDoi)) {
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
    doiDataCiteHandlingService.datasetChanged(get(dataset.getKey()), oldDoi);
  }

  /** Add old DOI to list of alt identifiers in dataset. */
  private void addDOIAsAlternateId(DOI altId, UUID datasetKey, String user) {
    // update alt ids of dataset
    Identifier id = new Identifier();
    id.setType(IdentifierType.DOI);
    id.setIdentifier(altId.toString());
    id.setCreatedBy(user);
    id.setCreated(new Date());
    LOG.info(
        "DOI changed. Adding previous DOI {} to alternative identifier list for dataset {}",
        altId,
        datasetKey);
    withMyBatis.addIdentifier(identifierMapper, datasetMapper, datasetKey, id);
  }

  /** Removes a DOI from the alternative identifiers list of a dataset if it exists. */
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
   * Scan list of alternate identifiers to find a previous, deleted GBIF DOI and update the dataset
   * instance. If none can be found use a newly generated one.
   */
  private void reactivatePreviousGbifDoiOrMintNew(List<Identifier> existingIds, Dataset d) {
    for (Identifier id : existingIds) {
      if (DOI.isParsable(id.getIdentifier())) {
        DOI doi = new DOI(id.getIdentifier());
        if (doiIssuingService.isGbif(doi)) {
          // remove from id list and make primary DOI
          LOG.info("Reactivating old GBIF DOI {} for dataset {}", doi, d.getKey());
          datasetMapper.deleteIdentifier(d.getKey(), id.getKey());
          d.setDoi(doi);
          return;
        }
      }
    }
    // we never had a GBIF DOI for this dataset, give it a new one
    DOI doi = doiIssuingService.newDatasetDOI();
    LOG.info("Create new GBIF DOI {} for dataset {}", doi, d.getKey());
    d.setDoi(doi);
  }

  /**
   * We need to implement this interface method here, but there is no way to retrieve the actual
   * user as we cannot access any http request. The real server method does this correctly but has
   * more parameters.
   */
  @Override
  public Metadata insertMetadata(UUID datasetKey, InputStream document) {
    // this method should never be called but from tests
    return insertMetadata(datasetKey, document, "UNKNOWN USER");
  }

  @Operation(
    operationId = "getConstituents",
    summary = "Retrieve all constituents of the dataset",
    description = "Lists the dataset's subdataset constituents (datasets that have a parentDatasetKey equal to the one requested).",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0230")))
  @Docs.DefaultEntityKeyParameter
  @Pageable.OffsetLimitParameters
  @ApiResponse(
    responseCode = "200",
    description = "List of constituents")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{key}/constituents")
  @Override
  public PagingResponse<Dataset> listConstituents(
      @PathVariable("key") UUID datasetKey, Pageable page) {
    return pagingResponse(
        page,
        (long) datasetMapper.countConstituents(datasetKey),
        datasetMapper.listConstituents(datasetKey, page));
  }

  @Operation(
    operationId = "getNetworks",
    summary = "List the networks the dataset belongs to",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0220")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(
    responseCode = "200",
    description = "List of networks")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{key}/networks")
  @Override
  public List<Network> listNetworks(@PathVariable("key") UUID datasetKey) {
    return networkMapper.listByDataset(datasetKey);
  }

  // TODO: What for?
  @Operation(
    operationId = "getAllConstituents",
    summary = "Retrieve all constituent datasets",
    description = "Lists datasets that are a constituent of any dataset.",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0230")))
  @Pageable.OffsetLimitParameters
  @ApiResponse(
    responseCode = "200",
    description = "List of datasets")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("constituents")
  @Override
  public PagingResponse<Dataset> listConstituents(Pageable page) {
    return pagingResponse(page, datasetMapper.countSubdatasets(), datasetMapper.subdatasets(page));
  }

  @Hidden
  @Operation(
    operationId = "getDatasetGrids",
    summary = "Retrieve all grids of a dataset")
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(
    responseCode = "200",
    description = "List of dataset grids")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{key}/gridded")
  @Override
  public List<Grid> listGrids(@PathVariable("key") UUID datasetKey) {
    return datasetMapper.listGrids(datasetKey);
  }

  @Operation(
    operationId = "getAllMetadata",
    summary = "Retrieve all dataset source metadata",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0302")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(
    responseCode = "200",
    description = "List of source metadata documents")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{key}/metadata")
  @Override
  public List<Metadata> listMetadata(
      @PathVariable UUID key, @RequestParam(value = "type", required = false) MetadataType type) {
    return registryDatasetService.listMetadata(key, type);
  }

  @Operation(
    operationId = "getMetadata",
    summary = "Retrieve metadata about a source metadata document of a dataset",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0303")))
  @ApiResponse(
    responseCode = "200",
    description = "Metadata about a metadata document")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("metadata/{key}")
  @Override
  @NullToNotFound("/dataset/metadata/{key}")
  public Metadata getMetadata(@PathVariable int key) {
    return metadataMapper.get(key);
  }

  @Override
  @NullToNotFound
  public InputStream getMetadataDocument(int key) {
    return new ByteArrayInputStream(getMetadataDocumentAsBytes(key));
  }

  // TODO: 05/04/2020 change API to return byte[]?
  @Operation(
    operationId = "getMetadataDocument",
    summary = "Retrieve a source metadata document of the dataset",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0304")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(
    responseCode = "200",
    description = "Source metadata document in XML format")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping(value = "metadata/{key}/document", produces = MediaType.APPLICATION_XML_VALUE)
  @NullToNotFound("/dataset/metadata/{key}/document")
  public byte[] getMetadataDocumentAsBytes(@PathVariable int key) {
    return registryDatasetService.getMetadataDocument(key);
  }

  @Operation(
    operationId = "deleteMetadata",
    summary = "Delete a source metadata document from the record",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0305")))
  @ApiResponse(
    responseCode = "204",
    description = "Metadata document deleted")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @DeleteMapping("metadata/{key}")
  @Override
  public void deleteMetadata(@PathVariable("key") int metadataKey) {
    metadataMapper.delete(metadataKey);
  }

  @Operation(
    operationId = "getDeletedDatasets",
    summary = "List all deleted datasets",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0500")))
  @Pageable.OffsetLimitParameters
  @ApiResponse(
    responseCode = "200",
    description = "List of deleted datasets")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("deleted")
  @Override
  public PagingResponse<Dataset> listDeleted(Pageable page) {
    return pagingResponse(page, datasetMapper.countDeleted(), datasetMapper.deleted(page));
  }

  @Operation(
    operationId = "getDuplicateDatasets",
    summary = "List all duplicate datasets",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0510")))
  @Pageable.OffsetLimitParameters
  @ApiResponse(
    responseCode = "200",
    description = "Duplicate datasets")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("duplicate")
  @Override
  public PagingResponse<Dataset> listDuplicates(Pageable page) {
    return pagingResponse(page, datasetMapper.countDuplicates(), datasetMapper.duplicates(page));
  }

  @Operation(
    operationId = "getNoEndpointDatasets",
    summary = "List all datasets with no endpoint",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0520")))
  @Pageable.OffsetLimitParameters
  @ApiResponse(
    responseCode = "200",
    description = "Datasets with no endpoint")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("withNoEndpoint")
  @Override
  public PagingResponse<Dataset> listDatasetsWithNoEndpoint(Pageable page) {
    return pagingResponse(
        page, datasetMapper.countWithNoEndpoint(), datasetMapper.withNoEndpoint(page));
  }

  /** Utility method to run batch jobs on all dataset elements */
  private void doOnAllOccurrenceDatasets(
      Consumer<Dataset> onDataset, List<UUID> datasetsToExclude) {
    PagingRequest pagingRequest = new PagingRequest(0, ALL_DATASETS_LIMIT);
    PagingResponse<Dataset> response;
    do {
      response = list(pagingRequest);
      response.getResults().stream()
          .filter(d -> datasetsToExclude == null || !datasetsToExclude.contains(d.getKey()))
          .forEach(
              d -> {
                try {
                  LOG.info("trying to crawl dataset {}", d.getKey());
                  onDataset.accept(d);
                } catch (Exception ex) {
                  LOG.error(
                      "Error processing dataset {} while crawling all: {}",
                      d.getKey(),
                      ex.getMessage());
                }
              });
      pagingRequest.addOffset(response.getResults().size());
    } while (!response.isEndOfRecords());
  }

  /**
   * This is a REST only (e.g. not part of the Java API) method that allows the registry console to
   * trigger the crawling of the dataset. This simply emits a message to rabbitmq requesting the
   * crawl, and applies necessary security.
   */
  // TODO: Deprecate and remove?
  @Hidden
  @PostMapping("crawlall")
  @Secured(ADMIN_ROLE)
  public void crawlAll(
      @RequestParam(value = "platform", required = false) String platform,
      @Nullable CrawlAllParams crawlAllParams) {
    CompletableFuture.runAsync(
        () ->
            doOnAllOccurrenceDatasets(
                dataset -> crawl(dataset.getKey(), platform),
                crawlAllParams != null ? crawlAllParams.datasetsToExclude : null));
  }

  /**
   * This is a REST only (e.g. not part of the Java API) method that allows the registry console to
   * trigger the crawling of the dataset. This simply emits a message to rabbitmq requesting the
   * crawl, and applies necessary security.
   */
  @Operation(
    operationId = "crawlDataset",
    summary = "Schedule a new ingestion of the dataset",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0210")))
  @Docs.DefaultEntityKeyParameter
  @Parameter(
    name = "platform",
    hidden = true)
  @ApiResponse(
    responseCode = "204",
    description = "Ingestion request accepted, or dataset is already being processed.")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping("{key}/crawl")
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public void crawl(
      @PathVariable("key") UUID datasetKey,
      @RequestParam(value = "platform", required = false) String platform) {
    Platform indexingPlatform = Platform.parse(platform).orElse(Platform.ALL);
    if (messagePublisher != null) {
      LOG.info("Requesting crawl of dataset[{}]", datasetKey);
      try {
        // we'll bump this to the top of the queue since it is a user initiated
        messagePublisher.send(
            new StartCrawlMessage(
                datasetKey, StartCrawlMessage.Priority.CRITICAL.getPriority(), indexingPlatform));
      } catch (IOException e) {
        LOG.error("Unable to send message requesting crawl", e);
      }
    } else {
      LOG.warn(
          "Registry is configured to run without messaging capabilities. Unable to crawl dataset[{}]",
          datasetKey);
    }
  }

  @Hidden
  @PostMapping(value = "{datasetKey}/process", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  @Transactional
  @Secured(ADMIN_ROLE)
  public void createDatasetProcessStatus(
      @PathVariable UUID datasetKey,
      @RequestBody @Valid @NotNull @Trim DatasetProcessStatus datasetProcessStatus) {
    checkArgument(
        datasetKey.equals(datasetProcessStatus.getDatasetKey()),
        "DatasetProcessStatus must have the same key as the dataset");
    createDatasetProcessStatus(datasetProcessStatus);
  }

  @Trim
  @Transactional
  @Secured(ADMIN_ROLE)
  @Override
  public void createDatasetProcessStatus(
      @Valid @NotNull @Trim DatasetProcessStatus datasetProcessStatus) {
    checkNotNull(
        datasetProcessStatus.getDatasetKey(), "DatasetProcessStatus must have the dataset key");
    checkNotNull(
        datasetProcessStatus.getCrawlJob(),
        "DatasetProcessStatus must have the crawl job with an attempt number");
    DatasetProcessStatus existing =
        datasetProcessStatusMapper.get(
            datasetProcessStatus.getDatasetKey(), datasetProcessStatus.getCrawlJob().getAttempt());
    checkArgument(
        existing == null,
        "Cannot create dataset process status [%s] for attempt[%s] as one already exists",
        datasetProcessStatus.getDatasetKey(),
        datasetProcessStatus.getCrawlJob().getAttempt());
    datasetProcessStatusMapper.create(datasetProcessStatus);
  }

  @Hidden
  @PutMapping(value = "{datasetKey}/process/{attempt}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  @Transactional
  @Secured(ADMIN_ROLE)
  public void updateDatasetProcessStatus(
      @PathVariable UUID datasetKey,
      @PathVariable int attempt,
      @RequestBody @Valid @NotNull @Trim DatasetProcessStatus datasetProcessStatus) {
    checkArgument(
        datasetKey.equals(datasetProcessStatus.getDatasetKey()),
        "DatasetProcessStatus must have the same key as the url");
    checkArgument(
        attempt == datasetProcessStatus.getCrawlJob().getAttempt(),
        "DatasetProcessStatus must have the same attempt as the url");
    updateDatasetProcessStatus(datasetProcessStatus);
  }

  @Trim
  @Transactional
  @Secured(ADMIN_ROLE)
  @Override
  public void updateDatasetProcessStatus(
      @Valid @NotNull @Trim DatasetProcessStatus datasetProcessStatus) {
    datasetProcessStatusMapper.update(datasetProcessStatus);
  }

  @Operation(
    operationId = "datasetCrawlAttempt",
    summary = "Get details of a particular crawl attempt for the dataset",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0212")))
  @Docs.DefaultEntityKeyParameter
  @Pageable.OffsetLimitParameters
  @ApiResponse(
    responseCode = "200",
    description = "Crawl attempt record")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{key}/process/{attempt}")
  @Nullable
  @NullToNotFound("/dataset/{key}/process/{attempt}")
  @Override
  public DatasetProcessStatus getDatasetProcessStatus(
      @PathVariable UUID key, @PathVariable int attempt) {
    return datasetProcessStatusMapper.get(key, attempt);
  }

  @Hidden
  @GetMapping("process")
  @Override
  public PagingResponse<DatasetProcessStatus> listDatasetProcessStatus(Pageable page) {
    return new PagingResponse<>(
        page, (long) datasetProcessStatusMapper.count(), datasetProcessStatusMapper.list(page));
  }

  @Hidden
  @GetMapping("process/aborted")
  @Override
  public PagingResponse<DatasetProcessStatus> listAbortedDatasetProcesses(Pageable page) {
    return new PagingResponse<>(
        page,
        (long) datasetProcessStatusMapper.countAborted(),
        datasetProcessStatusMapper.listAborted(page));
  }

  @Operation(
    operationId = "listDatasetCrawlAttempt",
    summary = "Get details of all crawl attempts for a dataset",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0211")))
  @Docs.DefaultEntityKeyParameter
  @Pageable.OffsetLimitParameters
  @ApiResponse(
    responseCode = "200",
    description = "Crawl attempt records")
  @GetMapping("{key}/process")
  @Override
  public PagingResponse<DatasetProcessStatus> listDatasetProcessStatus(
      @PathVariable UUID key, Pageable page) {
    return new PagingResponse<>(
        page,
        (long) datasetProcessStatusMapper.countByDataset(key),
        datasetProcessStatusMapper.listByDataset(key, page));
  }

  @Override
  public PagingResponse<Dataset> listByDOI(String doi, Pageable page) {
    return new PagingResponse<>(
        page, datasetMapper.countByDOI(doi), datasetMapper.listByDOI(doi, page));
  }

  @Operation(
    operationId = "datasetByDoi",
    summary = "Retrieve a dataset by DOI",
    description = "Retrieves datasets (may be more than one) referencing the given DOI.",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0110")))
  @Parameters(
    value = {
      @Parameter(
        name = "prefix",
        description = "Plain DOI prefix (before the slash)",
        example = "10.15468",
        in = ParameterIn.PATH),
      @Parameter(
        name = "suffix",
        description = "Plain DOI suffix (after the slash)",
        example = "igasai",
        in = ParameterIn.PATH)
    })
  @Pageable.OffsetLimitParameters
  @ApiResponse(
    responseCode = "200",
    description = "Dataset list")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("doi/{prefix}/{suffix}")
  public PagingResponse<Dataset> listByDOI(
      @PathVariable("prefix") String prefix, @PathVariable("suffix") String suffix, Pageable page) {
    return listByDOI(new DOI(prefix, suffix).getDoiName(), page);
  }

  /** Encapsulates the params to pass in the body for the crawAll method. */
  private static class CrawlAllParams {

    List<UUID> datasetsToExclude = new ArrayList<>();

    // getters and setters needed for jackson

    public List<UUID> getDatasetsToExclude() {
      return datasetsToExclude;
    }

    public void setDatasetsToExclude(List<UUID> datasetsToExclude) {
      this.datasetsToExclude = datasetsToExclude;
    }
  }
}
