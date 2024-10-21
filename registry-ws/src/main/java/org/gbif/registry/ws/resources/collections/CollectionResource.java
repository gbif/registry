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
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.gbif.api.annotation.NullToNotFound;
import org.gbif.api.annotation.Trim;
import org.gbif.api.documentation.CommonParameters;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionImportParams;
import org.gbif.api.model.collections.SourceableField;
import org.gbif.api.model.collections.descriptors.Descriptor;
import org.gbif.api.model.collections.descriptors.DescriptorGroup;
import org.gbif.api.model.collections.latimercore.ObjectGroup;
import org.gbif.api.model.collections.request.CollectionSearchRequest;
import org.gbif.api.model.collections.request.DescriptorGroupSearchRequest;
import org.gbif.api.model.collections.request.DescriptorSearchRequest;
import org.gbif.api.model.collections.suggestions.CollectionChangeSuggestion;
import org.gbif.api.model.collections.view.CollectionView;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.DescriptorsService;
import org.gbif.api.util.iterables.Iterables;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.GbifRegion;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.collections.AccessionStatus;
import org.gbif.api.vocabulary.collections.CollectionContentType;
import org.gbif.api.vocabulary.collections.PreservationType;
import org.gbif.api.vocabulary.collections.Source;
import org.gbif.registry.service.collections.batch.CollectionBatchService;
import org.gbif.registry.service.collections.duplicates.CollectionDuplicatesService;
import org.gbif.registry.service.collections.merge.CollectionMergeService;
import org.gbif.registry.service.collections.suggestions.CollectionChangeSuggestionService;
import org.gbif.registry.service.collections.utils.MasterSourceUtils;
import org.gbif.registry.ws.export.CsvWriter;
import org.gbif.registry.ws.resources.Docs;
import org.gbif.ws.WebApplicationException;
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
      @Value("${api.root.url}") String apiBaseUrl) {
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
                    + "`contentType=PALEONTOLOGICAL_OTHER&contentType=EARTH_PLANETARY_MINERALS`.",
            schema = @Schema(implementation = CollectionContentType.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "preservationType",
            description =
                "Preservation type of a GrSciColl collection. Accepts multiple values, for example "
                    + "`preservationType=SAMPLE_CRYOPRESERVED&preservationType=SAMPLE_FLUID_PRESERVED`.",
            schema = @Schema(implementation = PreservationType.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "accessionStatus",
            description = "Accession status of a GrSciColl collection",
            schema = @Schema(implementation = AccessionStatus.class),
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
  public CollectionView getCollectionView(@PathVariable UUID key) {
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
  public ObjectGroup getCollectionAsLatimerCore(@PathVariable UUID key) {
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
  public void delete(@PathVariable UUID key) {
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
      CollectionSearchRequest searchRequest)
      throws IOException {

    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, getExportFileHeader(searchRequest, format));

    try (Writer writer = new BufferedWriter(new OutputStreamWriter(response.getOutputStream()))) {
      CsvWriter.collections(
              Iterables.collections(searchRequest, collectionService, EXPORT_LIMIT), format)
          .export(writer);
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
      @PathVariable UUID collectionKey,
      @RequestParam(value = "format", defaultValue = "CSV") ExportFormat format,
      @RequestPart(value = "descriptorsFile", required = false) MultipartFile descriptorsFile,
      @RequestParam("title") @Trim String title,
      @RequestParam(value = "description", required = false) @Trim String description) {
    return descriptorsService.createDescriptorGroup(
        StreamUtils.copyToByteArray(descriptorsFile.getResource().getInputStream()),
        format,
        title,
        description,
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
      @RequestPart("descriptorsFile") MultipartFile descriptorsFile,
      @RequestParam("title") @Trim String title,
      @RequestParam(value = "description", required = false) @Trim String description) {
    DescriptorGroup existingDescriptorGroup =
        descriptorsService.getDescriptorGroup(descriptorGroupKey);
    if (existingDescriptorGroup == null) {
      throw new WebApplicationException("Descriptor group was not found", HttpStatus.NOT_FOUND);
    }

    Preconditions.checkArgument(existingDescriptorGroup.getCollectionKey().equals(collectionKey));

    descriptorsService.updateDescriptorGroup(
        descriptorGroupKey,
        StreamUtils.copyToByteArray(descriptorsFile.getResource().getInputStream()),
        format,
        title,
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
                "Individual count of the descriptor. It supports ranges and a '*' can be used as a wildcard",
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
}
