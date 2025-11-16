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
package org.gbif.registry.ws.resources.collections;

import org.gbif.api.annotation.NullToNotFound;
import org.gbif.api.annotation.Trim;
import org.gbif.api.documentation.CommonParameters;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionImportParams;
import org.gbif.api.model.collections.SourceableField;
import org.gbif.api.model.collections.descriptors.Descriptor;
import org.gbif.api.model.collections.descriptors.DescriptorChangeSuggestion;
import org.gbif.api.model.collections.descriptors.DescriptorChangeSuggestionRequest;
import org.gbif.api.model.collections.descriptors.DescriptorGroup;
import org.gbif.api.model.collections.latimercore.ObjectGroup;
import org.gbif.api.model.collections.request.CollectionDescriptorsSearchRequest;
import org.gbif.api.model.collections.request.CollectionSearchRequest;
import org.gbif.api.model.collections.request.DescriptorGroupSearchRequest;
import org.gbif.api.model.collections.request.DescriptorSearchRequest;
import org.gbif.api.model.collections.request.InstitutionSearchRequest;
import org.gbif.api.model.collections.suggestions.CollectionChangeSuggestion;
import org.gbif.api.model.collections.suggestions.Status;
import org.gbif.api.model.collections.suggestions.Type;
import org.gbif.api.model.collections.search.CollectionSearchResponse;
import org.gbif.api.model.collections.search.FacetedSearchResponse;
import org.gbif.api.model.collections.view.CollectionView;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.DescriptorChangeSuggestionService;
import org.gbif.api.service.collections.DescriptorsService;
import org.gbif.api.util.iterables.Iterables;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.GbifRegion;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.collections.CollectionFacetParameter;
import org.gbif.api.vocabulary.collections.Source;
import org.gbif.registry.service.collections.batch.CollectionBatchService;
import org.gbif.registry.service.collections.CollectionsSearchService;
import org.gbif.registry.service.collections.DefaultCollectionService;
import org.gbif.registry.service.collections.duplicates.CollectionDuplicatesService;
import org.gbif.registry.service.collections.merge.CollectionMergeService;
import org.gbif.registry.service.collections.suggestions.CollectionChangeSuggestionService;
import org.gbif.registry.service.collections.utils.MasterSourceUtils;
import org.gbif.registry.ws.export.CsvWriter;
import org.gbif.registry.ws.resources.Docs;
import org.gbif.ws.WebApplicationException;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.base.Preconditions;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.SneakyThrows;

/**
 * Class that acts both as the WS endpoint for {@link Collection} entities and also provides an
 * implementation of {@link CollectionService}.
 */
@io.swagger.v3.oas.annotations.tags.Tag(
    name = "Collections",
    description =
        "The collections API provides CRUD services for collections, institutions and person entities. "
            + "The data was originally migrated from the Global Registry of Scientific Collections (GRSciColl) and adapted to "
            + "follow the same conventions as other registry services. Therefore, the deletion of collections, institutions and "
            + "persons is logical, meaning these entries remain registered forever and only get a deleted timestamp. On the "
            + "other hand, the deletion of tags and identifiers is physical, meaning the entries are permanently removed.\n\n"
            + "*Please note that this part of the API is still under development, and may change in the future.*\n\n"
            + "## Collection\n"
            + "This API provides CRUD services for the collection entity. A collection can be associated with an institution "
            + "and can have a list of contacts, which are represented by the person entity. It can also have tags and identifiers.",
    extensions =
        @io.swagger.v3.oas.annotations.extensions.Extension(
            name = "Order",
            properties = @ExtensionProperty(name = "Order", value = "1000")))
@RestController
@RequestMapping(value = "grscicoll/collection", produces = MediaType.APPLICATION_JSON_VALUE)
public class CollectionResource
    extends BaseCollectionEntityResource<Collection, CollectionChangeSuggestion> {

  private final CollectionService collectionService;
  private final DescriptorsService descriptorsService;
  private final DescriptorChangeSuggestionService descriptorChangeSuggestionService;
  private final CollectionsSearchService collectionsSearchService;

  // Prefix for the export file format
  private static final String EXPORT_FILE_NAME = "%scollections.%s";

  // Page size to iterate over download stats export service
  private static final int EXPORT_LIMIT = 1_000;
  private static final String INVALID_COLLECTION_KEY_IN_DESCRIPTOR_GROUP =
      "The collection key in the path doesn't match the one in the descriptor group";

  public CollectionResource(
      CollectionMergeService collectionMergeService,
      CollectionDuplicatesService duplicatesService,
      CollectionService collectionService,
      CollectionChangeSuggestionService collectionChangeSuggestionService,
      CollectionBatchService batchService,
      DescriptorsService descriptorsService,
      CollectionsSearchService collectionsSearchService,
      @Value("${api.root.url}") String apiBaseUrl,
    DescriptorChangeSuggestionService descriptorChangeSuggestionService) {
    super(
        collectionMergeService,
        collectionService,
        collectionChangeSuggestionService,
        duplicatesService,
        batchService,
        apiBaseUrl,
        Collection.class);
    this.collectionService = collectionService;
    this.descriptorsService = descriptorsService;
    this.descriptorChangeSuggestionService = descriptorChangeSuggestionService;
    this.collectionsSearchService = collectionsSearchService;
  }

  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @Parameters(
      value = {
        @Parameter(
            name = "institution",
            description = "A key for the institution. Deprecated: use institutionKey instead.",
            schema = @Schema(implementation = UUID.class),
            in = ParameterIn.QUERY,
            deprecated = true),
        @Parameter(
            name = "contentType",
            description =
                "Content type of a GrSciColl collection. Accepts multiple values, for example "
                    + "`contentType=Paleontological&contentType=EarthPlanetary`.",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "preservationType",
            description =
                "Preservation type of a GrSciColl collection. Accepts multiple values, for example "
                    + "`preservationType=SampleCryopreserved&preservationType=SampleFluidPreserved`.",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "accessionStatus",
            description = "Accession status of a GrSciColl collection. Accepts multiple values, for example "
                    + "`accessionStatus=Institutional&accessionStatus=Project",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "personalCollection",
            description = "Flag for personal GRSciColl collections",
            schema = @Schema(implementation = Boolean.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "sourceId",
            description = "sourceId of MasterSourceMetadata",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "source",
            description = "Source attribute of MasterSourceMetadata",
            schema = @Schema(implementation = Source.class),
            in = ParameterIn.QUERY)
      })
  @SearchRequestParameters
  @interface CollectionSearchParameters {}

  @Operation(
      operationId = "getCollection",
      summary = "Get details of a single collection",
      description = "Details of a single collection.  Also works for deleted collections.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0200")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "200", description = "Collection found and returned")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{key}")
  @NullToNotFound("/grscicoll/collection/{key}")
  public CollectionView getCollectionView(@PathVariable("key") UUID key) {
    return collectionService.getCollectionView(key);
  }

  @Operation(
      operationId = "getCollectionAsLatimerCore",
      summary = "Get details of a single collection in Latimer Core format",
      description = "Details of a single collection.  Also works for deleted collections.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0200")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "200", description = "Collection found and returned")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("latimerCore/{key}")
  @NullToNotFound("/grscicoll/collection/{key}")
  public ObjectGroup getCollectionAsLatimerCore(@PathVariable("key") UUID key) {
    return collectionService.getAsLatimerCore(key);
  }

  // Method overridden only for documentation.
  @Operation(
      operationId = "createCollection",
      summary = "Create a new collection",
      description = "Creates a new collection.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0201")))
  @ApiResponse(
      responseCode = "201",
      description = "Collection created, new collection's UUID returned")
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  public UUID create(@RequestBody @Trim Collection collection) {
    return super.create(collection);
  }

  @Operation(
      operationId = "createCollectionFromLatimerCore",
      summary = "Create a new collection posted in Latimer Core format.",
      description = "Creates a new collection.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0201")))
  @ApiResponse(
      responseCode = "201",
      description = "Collection created, new collection's UUID returned")
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping(value = "latimerCore", consumes = MediaType.APPLICATION_JSON_VALUE)
  public UUID createFromLatimerCore(@RequestBody @Trim ObjectGroup objectGroup) {
    return collectionService.createFromLatimerCore(objectGroup);
  }

  // Method overridden only for documentation.
  @Operation(
      operationId = "updateCollection",
      summary = "Update an existing collection",
      description = "Updates the existing collection.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0202")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "204", description = "Collection updated")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PutMapping(value = "{key}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  public void update(@PathVariable("key") UUID key, @RequestBody @Trim Collection collection) {
    super.update(key, collection);
  }

  @Operation(
      operationId = "updateCollectionFromLatimerCore",
      summary = "Update an existing collection sent in Latimer Core format",
      description = "Updates the existing collection.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0202")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "204", description = "Collection updated")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PutMapping(value = "latimerCore/{key}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public void updateFromLatimerCore(
      @PathVariable("key") UUID key, @RequestBody @Trim ObjectGroup objectGroup) {
    collectionService.updateFromLatimerCore(objectGroup);
  }

  // Method overridden only for documentation.
  @Operation(
      operationId = "deleteCollection",
      summary = "Delete an existing collection",
      description =
          "Deletes an existing collection. The collection entry gets a deleted timestamp but remains registered.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0203")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "204", description = "Collection marked as deleted")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @DeleteMapping("{key}")
  @Override
  public void delete(@PathVariable("key") UUID key) {
    super.delete(key);
  }

  @Operation(
      operationId = "listCollections",
      summary = "List all collections",
      description = "Lists all current collections (deleted collections are not listed).",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0100")))
  @CollectionSearchParameters
  @ApiResponse(responseCode = "200", description = "Collection search successful")
  @ApiResponse(responseCode = "400", description = "Invalid search query provided")
  @GetMapping
  public PagingResponse<CollectionView> list(CollectionSearchRequest searchRequest) {
    return collectionService.list(searchRequest);
  }

  @Operation(
      operationId = "listCollectionsAsLatimerCore",
      summary = "List all collections in Latimer Core format",
      description = "Lists all current collections (deleted collections are not listed).",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0100")))
  @CollectionSearchParameters
  @ApiResponse(responseCode = "200", description = "Collection search successful")
  @ApiResponse(responseCode = "400", description = "Invalid search query provided")
  @GetMapping("latimerCore")
  public PagingResponse<ObjectGroup> listAsLatimerCore(CollectionSearchRequest searchRequest) {
    return collectionService.listAsLatimerCore(searchRequest);
  }

  private String getExportFileHeader(CollectionSearchRequest searchRequest, ExportFormat format) {
    String preFileName =
        CsvWriter.notNullJoiner(
            "-",
            join(searchRequest.getGbifRegion(), GbifRegion::name),
            join(searchRequest.getCountry(), Country::getIso2LetterCode),
            join(searchRequest.getCity()),
            join(searchRequest.getInstitution(), UUID::toString),
            join(searchRequest.getAlternativeCode()),
            join(searchRequest.getCode()),
            join(searchRequest.getName()),
            join(searchRequest.getContact(), UUID::toString),
            join(searchRequest.getIdentifierType(), IdentifierType::name),
            join(searchRequest.getIdentifier()),
            join(searchRequest.getMachineTagNamespace()),
            join(searchRequest.getMachineTagName()),
            join(searchRequest.getMachineTagValue()),
            join(searchRequest.getFuzzyName()),
            searchRequest.getQ());
    if (!preFileName.isEmpty()) {
      preFileName += "-";
    }
    return ContentDisposition.builder("attachment")
        .filename(String.format(EXPORT_FILE_NAME, preFileName, format.name().toLowerCase()))
        .build()
        .toString();
  }

  @Operation(
      operationId = "listCollectionsExport",
      summary = "Export search across all collections.",
      description = "Download full-text search results as CSV or TSV.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0102")))
  @CollectionSearchParameters
  @ApiResponse(responseCode = "200", description = "Collection search successful")
  @ApiResponse(responseCode = "400", description = "Invalid search query provided")
  @GetMapping("export")
  public void export(
      HttpServletResponse response,
      @RequestParam(value = "format", defaultValue = "TSV") ExportFormat format,
      CollectionDescriptorsSearchRequest searchRequest)
      throws IOException {

    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, getExportFileHeader(searchRequest, format));

    try (Writer writer = new BufferedWriter(new OutputStreamWriter(response.getOutputStream()))) {
      // Use collectionService.searchCollections() directly to support all descriptor parameters
      // This is not ideal since we make duplicate db calls
      List<CollectionView> collections = getAllCollectionsForExport(searchRequest);
      CsvWriter.collections(collections, format).export(writer);
    }
  }

  private List<CollectionView> getAllCollectionsForExport(CollectionDescriptorsSearchRequest searchRequest) {
    List<CollectionView> allCollections = new ArrayList<>();
    long offset = searchRequest.getOffset() != null ? searchRequest.getOffset() : 0L;
    int limit = searchRequest.getLimit() != null ? searchRequest.getLimit() : 20;

    FacetedSearchResponse<CollectionSearchResponse, CollectionFacetParameter> searchResponse;
    do {
      searchRequest.setOffset(offset);
      searchRequest.setLimit(limit);

      searchResponse = collectionsSearchService.searchCollections(searchRequest);

        // Convert CollectionSearchResponse to CollectionView - Might be improved in case of performance issues
        List<UUID> keys = searchResponse.getResults().stream()
            .map(CollectionSearchResponse::getKey)
            .collect(Collectors.toList());
        List<CollectionView> pageCollections = ((DefaultCollectionService) collectionService).getCollectionViews(keys);

      allCollections.addAll(pageCollections);
      offset += limit;

    } while (searchResponse.getCount() != null && offset < searchResponse.getCount());

    return allCollections;
  }



  @Operation(
      operationId = "listCollectionsForInstitutions",
      summary = "List collections for institutions matching search criteria",
      description = "Returns collections that belong to institutions matching the provided search criteria. "
          + "The method first retrieves all matching institutions using pagination, then returns their collections.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0104")))
  @ApiResponse(responseCode = "200", description = "Collections found and returned")
  @ApiResponse(responseCode = "400", description = "Invalid search query provided")
  @GetMapping("listForInstitution")
  public PagingResponse<CollectionView> listForInstitutions(InstitutionSearchRequest searchRequest) {
    List<CollectionView> collections = collectionService.getCollectionsForInstitutionsBySearch(searchRequest);
    return new PagingResponse<>(new PagingResponse<>(), (long) collections.size(), collections);
  }

  @Operation(
      operationId = "exportCollectionsForInstitutions",
      summary = "Export collections for institutions matching search criteria",
      description = "Download collections that belong to institutions matching the provided search criteria. "
          + "The method first retrieves all matching institutions using pagination, then exports their collections "
          + "in the specified format (TSV or CSV).",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0103")))
  @ApiResponse(responseCode = "200", description = "Collections exported successfully")
  @ApiResponse(responseCode = "204", description = "No institutions found matching the search criteria")
  @ApiResponse(responseCode = "400", description = "Invalid search query provided")
  @GetMapping("exportForInstitution")
  public void exportForInstitutions(HttpServletResponse response,
    @RequestParam(value = "format", defaultValue = "TSV") ExportFormat format, InstitutionSearchRequest searchRequest) throws IOException {

    List<CollectionView> collections = collectionService.getCollectionsForInstitutionsBySearch(searchRequest);

    if (collections.isEmpty()) {
      response.setStatus(HttpServletResponse.SC_NO_CONTENT);
      return;
    }

    // Create collection search request with the search parameters for the file header
    CollectionSearchRequest collectionRequest = CollectionSearchRequest.builder()
        .country(searchRequest.getCountry())
        .gbifRegion(searchRequest.getGbifRegion())
        .city(searchRequest.getCity())
        .name(searchRequest.getName())
        .code(searchRequest.getCode())
        .alternativeCode(searchRequest.getAlternativeCode())
        .identifierType(searchRequest.getIdentifierType())
        .identifier(searchRequest.getIdentifier())
        .machineTagNamespace(searchRequest.getMachineTagNamespace())
        .machineTagName(searchRequest.getMachineTagName())
        .machineTagValue(searchRequest.getMachineTagValue())
        .fuzzyName(searchRequest.getFuzzyName())
        .q(searchRequest.getInstitutionalGovernance() != null
            ? searchRequest.getInstitutionalGovernance().stream().filter(Objects::nonNull).collect(Collectors.joining(","))
            : null)
        .build();

    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, getExportFileHeader(collectionRequest, format));

    try (Writer writer = new BufferedWriter(new OutputStreamWriter(response.getOutputStream()))) {
      CsvWriter.collections(collections, format).export(writer);
    }
  }

  @Operation(
      operationId = "listDeleted",
      summary = "Retrieve all deleted collection records",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0500")))
  @CollectionSearchParameters
  @ApiResponse(responseCode = "200", description = "List of deleted collection records")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("deleted")
  public PagingResponse<CollectionView> listDeleted(CollectionSearchRequest searchRequest) {
    return collectionService.listDeleted(searchRequest);
  }

  @Operation(
      operationId = "suggestCollections",
      summary = "Suggest collections.",
      description =
          "Search that returns up to 20 matching collections. Results are ordered by relevance. "
              + "The response is smaller than a collection search.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0103")))
  @CommonParameters.QParameter
  @ApiResponse(responseCode = "200", description = "Collection search successful")
  @ApiResponse(responseCode = "400", description = "Invalid search query provided")
  @GetMapping("suggest")
  public List<KeyCodeNameResult> suggest(@RequestParam(value = "q", required = false) String q) {
    return collectionService.suggest(q);
  }

  @Operation(
      operationId = "importCollection",
      summary = "Import a collection",
      description = "Imports a collection from a dataset.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0495")))
  @ApiResponse(
      responseCode = "200",
      description = "Collection imported, key returned.",
      content = @Content)
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping(value = "import", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  public UUID createFromDataset(@RequestBody @Trim CollectionImportParams importParams) {
    return collectionService.createFromDataset(
        importParams.getDatasetKey(), importParams.getCollectionCode());
  }

  @Hidden
  @GetMapping("sourceableFields")
  public List<SourceableField> getSourceableFields() {
    return MasterSourceUtils.COLLECTION_SOURCEABLE_FIELDS;
  }

  @Operation(
      operationId = "getCollectionDescriptorGroups",
      summary = "Lists the descriptor groups of the collection.",
      description = "Lists the descriptor groups of the collection.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0500")))
  @Parameters(
      value = {
        @Parameter(
            name = "title",
            description = "Descriptor group title",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "description",
            description = "Descriptor group description",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "deleted",
            description = "Boolean flag to indicate if we want deleted descriptor groups",
            schema = @Schema(implementation = Boolean.class),
            in = ParameterIn.QUERY)
      })
  @CommonParameters.QParameter
  @Pageable.OffsetLimitParameters
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(
      responseCode = "200",
      description = "Collection descriptor groups found and returned")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{collectionKey}/descriptorGroup")
  @NullToNotFound("/grscicoll/collection/{collectionKey}/descriptorGroup")
  public PagingResponse<DescriptorGroup> listCollectionDescriptorGroups(
      @PathVariable("collectionKey") UUID collectionKey,
      DescriptorGroupSearchRequest searchRequest) {
    return descriptorsService.listDescriptorGroups(collectionKey, searchRequest);
  }

  @Operation(
      operationId = "createCollectionDescriptorGroup",
      summary = "Create a new collection descriptor group",
      description = "Creates a new collection descriptor group.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0501")))
  @ApiResponse(
      responseCode = "201",
      description = "Collection descriptor group created, new descriptor group's key returned")
  @Docs.DefaultUnsuccessfulWriteResponses
  @SneakyThrows
  @PostMapping(
      value = "{collectionKey}/descriptorGroup",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public long createDescriptorGroup(
      @PathVariable("collectionKey") UUID collectionKey,
      @RequestParam(value = "format", defaultValue = "CSV") ExportFormat format,
      @RequestPart(value = "descriptorsFile", required = false) MultipartFile descriptorsFile,
      @RequestParam("title") @Trim String title,
      @RequestParam(value = "description", required = false) @Trim String description,
      @RequestParam(value = "tags", required = false) Set<String> tags) {
    return descriptorsService.createDescriptorGroup(
        StreamUtils.copyToByteArray(descriptorsFile.getResource().getInputStream()),
        format,
        title,
        description,
        tags,
        collectionKey);
  }

  @Operation(
      operationId = "updateCollectionDescriptorGroup",
      summary = "Update an existing collection descriptor group",
      description =
          "Updates the existing collection descriptor group. It updates the metadata and reimport the file by deleting the "
              + "existing descriptors and creating new ones.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0502")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "204", description = "Collection descriptor group updated")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @SneakyThrows
  @PutMapping(
      value = "{collectionKey}/descriptorGroup/{key}",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public void updateDescriptorGroup(
      @PathVariable("collectionKey") UUID collectionKey,
      @PathVariable("key") long descriptorGroupKey,
      @RequestParam(value = "format", defaultValue = "CSV") ExportFormat format,
      @RequestPart(value = "descriptorsFile", required = false) MultipartFile descriptorsFile,
      @RequestParam("title") @Trim String title,
      @RequestParam(value = "description", required = false) @Trim String description,
      @RequestParam(value = "tags", required = false) Set<String> tags) {
    DescriptorGroup existingDescriptorGroup =
        descriptorsService.getDescriptorGroup(descriptorGroupKey);
    if (existingDescriptorGroup == null) {
      throw new WebApplicationException("Descriptor group was not found", HttpStatus.NOT_FOUND);
    }

    Preconditions.checkArgument(existingDescriptorGroup.getCollectionKey().equals(collectionKey));
    byte[] file = descriptorsFile != null ? StreamUtils.copyToByteArray(descriptorsFile.getResource().getInputStream()) : null;
    descriptorsService.updateDescriptorGroup(
        descriptorGroupKey,
        file,
        format,
        title,
        tags,
        description);
  }

  @Operation(
      operationId = "getCollectionDescriptorGroup",
      summary = "Get details of a single collection descriptor",
      description = "Details of a single collection descriptor",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0500")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "200", description = "Collection found and returned")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{collectionKey}/descriptorGroup/{key}")
  @NullToNotFound("/grscicoll/collection/{collectionKey}/descriptorGroup/{key}")
  public DescriptorGroup getCollectionDescriptorGroup(
      @PathVariable("collectionKey") UUID collectionKey,
      @PathVariable("key") long descriptorGroupKey) {
    DescriptorGroup existingDescriptorGroup =
        descriptorsService.getDescriptorGroup(descriptorGroupKey);

    if (existingDescriptorGroup != null) {
      Preconditions.checkArgument(
          existingDescriptorGroup.getCollectionKey().equals(collectionKey),
          INVALID_COLLECTION_KEY_IN_DESCRIPTOR_GROUP);
    }

    return existingDescriptorGroup;
  }

  @Operation(
      operationId = "deleteCollectionDescriptorGroup",
      summary = "Deletes a collection descriptor group",
      description = "Deletes a collection descriptor group",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0510")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "204", description = "Descriptor group marked as deleted")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @DeleteMapping("{collectionKey}/descriptorGroup/{key}")
  public void deleteCollectionDescriptorGroup(
      @PathVariable("collectionKey") UUID collectionKey,
      @PathVariable("key") long descriptorGroupKey) {
    DescriptorGroup existingDescriptorGroup =
        descriptorsService.getDescriptorGroup(descriptorGroupKey);
    if (existingDescriptorGroup == null) {
      throw new WebApplicationException("Descriptor group was not found", HttpStatus.NOT_FOUND);
    }

    Preconditions.checkArgument(
        existingDescriptorGroup.getCollectionKey().equals(collectionKey),
        INVALID_COLLECTION_KEY_IN_DESCRIPTOR_GROUP);

    descriptorsService.deleteDescriptorGroup(descriptorGroupKey);
  }

  @Operation(
      operationId = "CollectionDescriptorGroupExport",
      summary = "Exports a collection descriptor group.",
      description = "Download a collection descriptor group as CSV or TSV.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0520")))
  @CollectionSearchParameters
  @ApiResponse(responseCode = "200", description = "Collection search successful")
  @ApiResponse(responseCode = "400", description = "Invalid search query provided")
  @GetMapping(value = "{collectionKey}/descriptorGroup/{key}/export", produces = "application/zip")
  public ResponseEntity<Resource> exportDescriptorGroup(
      HttpServletResponse response,
      @PathVariable("collectionKey") UUID collectionKey,
      @PathVariable("key") long descriptorGroupKey,
      @RequestParam(value = "format", defaultValue = "TSV") ExportFormat format)
      throws IOException {
    DescriptorGroup existingDescriptorGroup =
        descriptorsService.getDescriptorGroup(descriptorGroupKey);
    if (existingDescriptorGroup == null) {
      throw new WebApplicationException("Descriptor group was not found", HttpStatus.NOT_FOUND);
    }

    Preconditions.checkArgument(
        existingDescriptorGroup.getCollectionKey().equals(collectionKey),
        INVALID_COLLECTION_KEY_IN_DESCRIPTOR_GROUP);

    DescriptorSearchRequest searchRequest =
        DescriptorSearchRequest.builder().descriptorGroupKey(descriptorGroupKey).build();

    Preconditions.checkArgument(
        descriptorsService.countDescriptors(searchRequest) > 0,
        "The descriptor group doesn't have descriptors");

    long ts = System.currentTimeMillis();

    Path interpretedCsv =
        Files.createFile(
            Paths.get("interpreted" + ts + "." + format.name().toLowerCase()),
            PosixFilePermissions.asFileAttribute(filePermissions()));
    try (Writer writer =
        new OutputStreamWriter(new FileOutputStream(interpretedCsv.toFile().getName()))) {
      CsvWriter.descriptors(
              Iterables.descriptors(descriptorsService, searchRequest, EXPORT_LIMIT), format)
          .export(writer);
    }

    Path verbatimCsv =
        Files.createFile(
            Paths.get("verbatim" + ts + "." + format.name().toLowerCase()),
            PosixFilePermissions.asFileAttribute(filePermissions()));
    Set<String> verbatimFields = descriptorsService.getVerbatimNames(descriptorGroupKey);
    try (Writer writer =
        new OutputStreamWriter(Files.newOutputStream(Paths.get(verbatimCsv.toFile().getName())))) {
      CsvWriter.descriptorVerbatims(
              Iterables.descriptorVerbatims(descriptorsService, searchRequest, EXPORT_LIMIT),
              format,
              verbatimFields)
          .exportMap(writer);
    }

    Path zipFile = zipFiles(existingDescriptorGroup, interpretedCsv, verbatimCsv);
    ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(zipFile));
    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=" + zipFile.toFile().getName())
        .body(resource);
  }

  private static Set<PosixFilePermission> filePermissions() {
    return Set.of(
        PosixFilePermission.OWNER_WRITE,
        PosixFilePermission.OWNER_READ,
        PosixFilePermission.OWNER_EXECUTE,
        PosixFilePermission.OTHERS_WRITE,
        PosixFilePermission.OTHERS_READ,
        PosixFilePermission.OTHERS_EXECUTE);
  }

  private static Path zipFiles(
      DescriptorGroup existingDescriptorGroup, Path interpretedCsv, Path verbatimCsv)
      throws IOException {
    Path zipFile =
        Files.createTempFile(
            "descriptorGroup" + existingDescriptorGroup.getKey() + "_export", ".zip");

    try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
      for (Path f : Arrays.asList(interpretedCsv, verbatimCsv)) {
        try (FileInputStream fis = new FileInputStream(f.toFile())) {
          ZipEntry zipEntry = new ZipEntry(f.toFile().getName().replaceAll("[0-9]", ""));
          zipOut.putNextEntry(zipEntry);
          byte[] bytes = new byte[1024];
          int length;
          while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
          }
          zipOut.closeEntry();
        }
        Files.delete(f);
      }
    }

    return zipFile;
  }

  @Operation(
      operationId = "getCollectionDescriptors",
      summary = "Lists the descriptors.",
      description = "Lists the descriptors.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0700")))
  @Parameters(
      value = {
        @Parameter(
            name = "descriptorGroupKey",
            description = "Key of the descriptor group",
            schema = @Schema(implementation = Long.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "usageKey",
            description = "Taxon usage key of the descriptor",
            schema = @Schema(implementation = Integer.class),
            in = ParameterIn.QUERY,
            explode = Explode.TRUE),
        @Parameter(
            name = "usageName",
            description = "Taxon usage name of the descriptor",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY,
            explode = Explode.TRUE),
        @Parameter(
            name = "usageRank",
            description = "Taxon usage rank of the descriptor",
            schema = @Schema(implementation = Rank.class),
            in = ParameterIn.QUERY,
            explode = Explode.TRUE),
        @Parameter(
            name = "taxonKey",
            description = "Taxon key of the descriptor",
            schema = @Schema(implementation = Integer.class),
            in = ParameterIn.QUERY,
            explode = Explode.TRUE),
        @Parameter(
            name = "country",
            description = "Country of the descriptor",
            schema = @Schema(implementation = Country.class),
            in = ParameterIn.QUERY,
            explode = Explode.TRUE),
        @Parameter(
            name = "individualCount",
            description =
                "Individual count of the descriptor. It supports ranges and a `*` can be used as a wildcard",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "identifiedBy",
            description = "Identified by field of the descriptor",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY,
            explode = Explode.TRUE),
        @Parameter(
            name = "dateIdentified",
            description = "Date identified field of the descriptor",
            schema = @Schema(implementation = Date.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "dateIdentifiedFrom",
            description = "Date identified of the descriptor is equal or higher than the specified",
            schema = @Schema(implementation = Date.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "dateIdentifiedBefore",
            description = "Date identified of the descriptor is lower than the specified",
            schema = @Schema(implementation = Date.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "typeStatus",
            description = "Type status of the descriptor",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY,
            explode = Explode.TRUE),
        @Parameter(
            name = "recordedBy",
            description = "RecordedBy of the descriptor",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY,
            explode = Explode.TRUE),
        @Parameter(
            name = "discipline",
            description = "Discipline of the descriptor",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY,
            explode = Explode.TRUE),
        @Parameter(
            name = "objectClassification",
            description = "Object classification of the descriptor",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY,
            explode = Explode.TRUE),
        @Parameter(
            name = "issues",
            description = "Issues of the descriptor",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY,
            explode = Explode.TRUE)
      })
  @CommonParameters.QParameter
  @Pageable.OffsetLimitParameters
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "200", description = "Descriptors found and returned")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{collectionKey}/descriptorGroup/{key}/descriptor")
  @NullToNotFound("/grscicoll/collection/{collectionKey}/descriptorGroup/{key}/descriptor")
  public PagingResponse<Descriptor> listCollectionDescriptors(
      @PathVariable("collectionKey") UUID collectionKey,
      @PathVariable("key") long descriptorGroupKey,
      DescriptorSearchRequest searchRequest) {
    DescriptorGroup existingDescriptorGroup =
        descriptorsService.getDescriptorGroup(descriptorGroupKey);
    if (existingDescriptorGroup == null) {
      return null;
    }

    searchRequest.setDescriptorGroupKey(descriptorGroupKey);
    return descriptorsService.listDescriptors(searchRequest);
  }

  @Operation(
      operationId = "getCollectionDescriptor",
      summary = "Lists the descriptor records.",
      description = "Lists the descriptor records.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0710")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "200", description = "Descriptor found and returned")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{collectionKey}/descriptorGroup/{descriptorGroupKey}/descriptor/{key}")
  @NullToNotFound(
      "/grscicoll/collection/{collectionKey}/descriptorGroup/{descriptorGroupKey}/descriptor/{key}")
  public Descriptor getCollectionDescriptor(
      @PathVariable("collectionKey") UUID collectionKey,
      @PathVariable("descriptorGroupKey") long descriptorGroupKey,
      @PathVariable("key") long descriptorKey) {
    DescriptorGroup existingDescriptorGroup =
        descriptorsService.getDescriptorGroup(descriptorGroupKey);
    if (existingDescriptorGroup == null) {
      return null;
    }

    Descriptor descriptor = descriptorsService.getDescriptor(descriptorKey);
    Preconditions.checkArgument(descriptor.getDescriptorGroupKey().equals(descriptorGroupKey));
    Preconditions.checkArgument(existingDescriptorGroup.getCollectionKey().equals(collectionKey));
    return descriptor;
  }

  @Operation(
      operationId = "reinterpretCollectionDescriptorGroup",
      summary = "Reinterprets a collection descriptor group",
      description = "Reinterprets a collection descriptor group.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0501")))
  @ApiResponse(responseCode = "204", description = "Collection descriptor group reinterpreted")
  @Docs.DefaultUnsuccessfulWriteResponses
  @SneakyThrows
  @PostMapping("{collectionKey}/descriptorGroup/{key}/reinterpret")
  public void reinterpretCollectionDescriptorGroup(
      @PathVariable("collectionKey") UUID collectionKey,
      @PathVariable("key") long descriptorGroupKey) {
    DescriptorGroup existingDescriptorGroup =
        descriptorsService.getDescriptorGroup(descriptorGroupKey);

    if (existingDescriptorGroup != null) {
      Preconditions.checkArgument(
          existingDescriptorGroup.getCollectionKey().equals(collectionKey),
          INVALID_COLLECTION_KEY_IN_DESCRIPTOR_GROUP);
    }
    descriptorsService.reinterpretDescriptorGroup(descriptorGroupKey);
  }

  @Operation(
      operationId = "reinterpretCollectionDescriptorGroups",
      summary = "Reinterprets all the descriptor groups of the collection",
      description = "Reinterprets all the descriptor groups of the collection",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0501")))
  @ApiResponse(responseCode = "204", description = "Collection descriptor groups reinterpreted")
  @Docs.DefaultUnsuccessfulWriteResponses
  @SneakyThrows
  @PostMapping("{collectionKey}/descriptorGroup/reinterpretAll")
  public void reinterpretCollectionDescriptorGroups(
      @PathVariable("collectionKey") UUID collectionKey) {
    descriptorsService.reinterpretCollectionDescriptorGroups(collectionKey);
  }

  @Operation(
      operationId = "reinterpretAllDescriptorGroups",
      summary = "Reinterprets all the descriptor groups",
      description = "Reinterprets all the descriptor groups",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0501")))
  @ApiResponse(responseCode = "204", description = "All descriptor groups reinterpreted")
  @Docs.DefaultUnsuccessfulWriteResponses
  @SneakyThrows
  @PostMapping("reinterpretAllDescriptorGroups")
  public void reinterpretAllDescriptorGroups() {
    descriptorsService.reinterpretAllDescriptorGroups();
  }

  @Operation(
      operationId = "createDescriptorSuggestion",
      summary = "Create a new descriptor change suggestion",
      description = "Creates a new suggestion for changing a collection's descriptor.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0300")))
  @ApiResponse(
      responseCode = "201",
      description = "Descriptor suggestion created, new suggestion's key returned")
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping(value = "{collectionKey}/descriptorGroup/suggestion", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public DescriptorChangeSuggestion createDescriptorSuggestion(
      @PathVariable("collectionKey") UUID collectionKey,
      @RequestPart(value = "file", required = false) MultipartFile file,
      @RequestParam("type") Type type,
      @RequestParam(value = "descriptorGroupKey", required = false) Long descriptorGroupKey,
      @RequestParam("title") String title,
      @RequestParam(value = "description", required = false) String description,
      @RequestParam("format") ExportFormat format,
      @RequestParam("comments") List<String> comments,
      @RequestParam("proposerEmail") String proposerEmail,
      @RequestParam(value = "tags", required = false) Set<String> tags) throws IOException {

    if (type == Type.CREATE && (file == null || file.isEmpty())) {
      throw new IllegalArgumentException("File is required for CREATE type suggestions");
    }

    DescriptorChangeSuggestionRequest request = new DescriptorChangeSuggestionRequest();
    request.setCollectionKey(collectionKey);
    request.setType(type);
    request.setDescriptorGroupKey(descriptorGroupKey);
    request.setTitle(title);
    request.setDescription(description);
    request.setFormat(format);
    request.setComments(comments);
    request.setTags(tags);
    request.setProposerEmail(proposerEmail);

    return descriptorChangeSuggestionService.createSuggestion(
      file != null ? file.getInputStream() : null,
      file != null ? file.getOriginalFilename() : null,
      request);
  }

  @Operation(
      operationId = "exportDescriptorSuggestionFile",
      summary = "Export a descriptor suggestion file",
      description = "Downloads the file associated with a descriptor change suggestion.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0301")))
  @ApiResponse(responseCode = "200", description = "File found and returned")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping(value = "{collectionKey}/descriptorGroup/suggestion/{key}/file", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<Resource> exportDescriptorSuggestionFile(
      @PathVariable("collectionKey") UUID collectionKey,
      @PathVariable("key") Integer key) throws IOException {

    // Get the suggestion details to include in the filename
    DescriptorChangeSuggestion suggestion = descriptorChangeSuggestionService.getSuggestion(key);
    if (suggestion == null) {
      throw new WebApplicationException("Descriptor suggestion was not found", HttpStatus.NOT_FOUND);
    }

    // Verify collection key matches
    Preconditions.checkArgument(
        suggestion.getCollectionKey().equals(collectionKey),
        "The collection key in the path does not match the suggestion's collection key");

    // Get the file content
    InputStream stream = descriptorChangeSuggestionService.getSuggestionFile(key);
    if (stream == null) {
      throw new WebApplicationException("Descriptor group suggestion file was not found",
        HttpStatus.NOT_FOUND);
    }
    byte[] fileBytes = IOUtils.toByteArray(stream);

    // Generate a descriptive filename
    String extension = suggestion.getFormat() != null ?
        suggestion.getFormat().name().toLowerCase() : "csv";
    String filename = "descriptor_suggestion_" + suggestion.getTitle() + "." + extension;

    // Create the ByteArrayResource from the file bytes
    ByteArrayResource resource = new ByteArrayResource(fileBytes);

    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
        .contentLength(fileBytes.length)
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(resource);
  }

  @Operation(
      operationId = "getDescriptorSuggestion",
      summary = "Get a descriptor change suggestion",
      description = "Retrieves details of a single descriptor change suggestion.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0302")))
  @ApiResponse(responseCode = "200", description = "Suggestion found and returned")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping(value = "{collectionKey}/descriptorGroup/suggestion/{key}")
  public DescriptorChangeSuggestion getDescriptorSuggestion(
      @PathVariable("collectionKey") UUID collectionKey,
      @PathVariable("key") long key) {
    return descriptorChangeSuggestionService.getSuggestion(key);
  }

  @Operation(
      operationId = "applyDescriptorSuggestion",
      summary = "Apply a descriptor change suggestion",
      description = "Applies a pending descriptor change suggestion.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0303")))
  @ApiResponse(responseCode = "200", description = "Suggestion applied successfully")
  @Docs.DefaultUnsuccessfulWriteResponses
  @PutMapping(value = "{collectionKey}/descriptorGroup/suggestion/{key}/apply", consumes = MediaType.ALL_VALUE)
  public void applyDescriptorSuggestion(
      @PathVariable("collectionKey") UUID collectionKey,
      @PathVariable("key") long key) throws IOException {
    descriptorChangeSuggestionService.applySuggestion(key);
  }

  @Operation(
      operationId = "discardDescriptorSuggestion",
      summary = "Discard a descriptor change suggestion",
      description = "Discards a pending descriptor change suggestion.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0304")))
  @ApiResponse(responseCode = "200", description = "Suggestion discarded successfully")
  @Docs.DefaultUnsuccessfulWriteResponses
  @PutMapping(value = "{collectionKey}/descriptorGroup/suggestion/{key}/discard")
  public void discardDescriptorSuggestion(
      @PathVariable("collectionKey") UUID collectionKey,
      @PathVariable("key") long key) {
    descriptorChangeSuggestionService.discardSuggestion(key);
  }

  @Operation(
      operationId = "updateDescriptorSuggestion",
      summary = "Update a descriptor change suggestion",
      description = "Updates an existing descriptor change suggestion.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0306")))
  @ApiResponse(responseCode = "200", description = "Suggestion updated successfully")
  @Docs.DefaultUnsuccessfulWriteResponses
  @PutMapping(value = "{collectionKey}/descriptorGroup/suggestion/{key}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public void updateDescriptorSuggestion(
      @PathVariable("collectionKey") UUID collectionKey,
      @PathVariable("key") long key,
      @RequestPart(value = "file", required = false) MultipartFile file,
      @RequestParam("type") Type type,
      @RequestParam("title") String title,
      @RequestParam("description") String description,
      @RequestParam("format") ExportFormat format,
      @RequestParam("comments") List<String> comments,
      @RequestParam(value = "tags", required = false) Set<String> tags,
      @RequestParam("proposerEmail") String proposerEmail) throws IOException {

    DescriptorChangeSuggestionRequest request = new DescriptorChangeSuggestionRequest();
    request.setCollectionKey(collectionKey);
    request.setType(type);
    request.setTitle(title);
    request.setDescription(description);
    request.setFormat(format);
    request.setComments(comments);
    request.setTags(tags);
    request.setProposerEmail(proposerEmail);

    descriptorChangeSuggestionService.updateSuggestion(
      key,
      request,
      file != null ? file.getInputStream() : null,
      file != null ? file.getOriginalFilename() : null);
  }

  @Operation(
      operationId = "listDescriptorSuggestions",
      summary = "List descriptor change suggestions",
      description = "Lists all descriptor change suggestions for a collection.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0306")))
  @ApiResponse(responseCode = "200", description = "List of suggestions returned")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping(value = "{collectionKey}/descriptorGroup/suggestion")
  public PagingResponse<DescriptorChangeSuggestion> listDescriptorSuggestions(
      @PathVariable("collectionKey") UUID collectionKey,
      @RequestParam(value = "status", required = false) Status status,
      @RequestParam(value = "type", required = false) Type type,
      @RequestParam(value = "proposerEmail", required = false) String proposerEmail,
      @RequestParam(value = "country", required = false) Country country,
      Pageable page) {
    return descriptorChangeSuggestionService.list(page, status, type, proposerEmail, collectionKey, country);
  }

  @Operation(
      operationId = "listAllDescriptorSuggestions",
      summary = "List all descriptor change suggestions",
      description = "Lists all descriptor change suggestions across all collections.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0307")))
  @ApiResponse(responseCode = "200", description = "List of suggestions returned")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping(value = "descriptorGroup/suggestion")
  public PagingResponse<DescriptorChangeSuggestion> listAllDescriptorSuggestions(
      @RequestParam(value = "status", required = false) Status status,
      @RequestParam(value = "type", required = false) Type type,
      @RequestParam(value = "proposerEmail", required = false) String proposerEmail,
      @RequestParam(value = "country", required = false) Country country,
      Pageable page) {
    return descriptorChangeSuggestionService.list(page, status, type, proposerEmail, null, country);
  }

}
