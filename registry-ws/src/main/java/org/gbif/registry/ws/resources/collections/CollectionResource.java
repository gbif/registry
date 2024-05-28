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

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.gbif.api.annotation.NullToNotFound;
import org.gbif.api.annotation.Trim;
import org.gbif.api.documentation.CommonParameters;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionImportParams;
import org.gbif.api.model.collections.SourceableField;
import org.gbif.api.model.collections.latimercore.ObjectGroup;
import org.gbif.api.model.collections.request.CollectionSearchRequest;
import org.gbif.api.model.collections.suggestions.CollectionChangeSuggestion;
import org.gbif.api.model.collections.view.CollectionView;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.util.iterables.Iterables;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.GbifRegion;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

  public final CollectionService collectionService;

  // Prefix for the export file format
  private static final String EXPORT_FILE_NAME = "%scollections.%s";

  // Page size to iterate over download stats export service
  private static final int EXPORT_LIMIT = 1_000;

  public CollectionResource(
      CollectionMergeService collectionMergeService,
      CollectionDuplicatesService duplicatesService,
      CollectionService collectionService,
      CollectionChangeSuggestionService collectionChangeSuggestionService,
      CollectionBatchService batchService,
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
            searchRequest.getGbifRegion() != null
                ? searchRequest.getGbifRegion().stream()
                    .map(GbifRegion::name)
                    .collect(Collectors.joining("-"))
                : null,
            searchRequest.getCountry() != null
                ? searchRequest.getCountry().stream()
                    .map(Country::getIso2LetterCode)
                    .collect(Collectors.joining("-"))
                : null,
            searchRequest.getCity(),
            searchRequest.getInstitution() != null
                ? searchRequest.getInstitution().toString()
                : null,
            searchRequest.getAlternativeCode(),
            searchRequest.getCode(),
            searchRequest.getName(),
            searchRequest.getContact() != null ? searchRequest.getContact().toString() : null,
            searchRequest.getIdentifierType() != null
                ? searchRequest.getIdentifierType().name()
                : null,
            searchRequest.getIdentifier(),
            searchRequest.getMachineTagNamespace(),
            searchRequest.getMachineTagName(),
            searchRequest.getMachineTagValue(),
            searchRequest.getFuzzyName(),
            searchRequest.getQ());
    if (preFileName.length() > 0) {
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
}
