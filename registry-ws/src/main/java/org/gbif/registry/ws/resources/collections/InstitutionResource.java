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
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.InstitutionImportParams;
import org.gbif.api.model.collections.SourceableField;
import org.gbif.api.model.collections.merge.ConvertToCollectionParams;
import org.gbif.api.model.collections.request.InstitutionSearchRequest;
import org.gbif.api.model.collections.suggestions.InstitutionChangeSuggestion;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.util.iterables.Iterables;
import org.gbif.api.vocabulary.collections.Discipline;
import org.gbif.api.vocabulary.collections.InstitutionGovernance;
import org.gbif.api.vocabulary.collections.InstitutionType;
import org.gbif.registry.service.collections.batch.InstitutionBatchService;
import org.gbif.registry.service.collections.duplicates.InstitutionDuplicatesService;
import org.gbif.registry.service.collections.merge.InstitutionMergeService;
import org.gbif.registry.service.collections.suggestions.InstitutionChangeSuggestionService;
import org.gbif.registry.service.collections.utils.MasterSourceUtils;
import org.gbif.registry.ws.export.CsvWriter;
import org.gbif.registry.ws.resources.Docs;

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

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

/**
 * Class that acts both as the WS endpoint for {@link Institution} entities and also provides an *
 * implementation of {@link InstitutionService}.
 */
@io.swagger.v3.oas.annotations.tags.Tag(
    name = "Institutions",
    description =
        " This API provides CRUD services for the institution entity. An institution can have a list of "
            + "contacts, which are represented by the person entity. They can also have tags and identifiers. ",
    extensions =
        @io.swagger.v3.oas.annotations.extensions.Extension(
            name = "Order",
            properties = @ExtensionProperty(name = "Order", value = "1100")))
@RestController
@RequestMapping(value = "grscicoll/institution", produces = MediaType.APPLICATION_JSON_VALUE)
public class InstitutionResource
    extends BaseCollectionEntityResource<Institution, InstitutionChangeSuggestion> {

  // Prefix for the export file format
  private static final String EXPORT_FILE_NAME = "%sinstitutions.%s";

  // Page size to iterate over download stats export service
  private static final int EXPORT_LIMIT = 1_000;

  private final InstitutionService institutionService;
  private final InstitutionMergeService institutionMergeService;

  public InstitutionResource(
      InstitutionMergeService institutionMergeService,
      InstitutionDuplicatesService duplicatesService,
      InstitutionService institutionService,
      InstitutionChangeSuggestionService institutionChangeSuggestionService,
      InstitutionBatchService batchService,
      @Value("${api.root.url}") String apiBaseUrl) {
    super(
        institutionMergeService,
        institutionService,
        institutionChangeSuggestionService,
        duplicatesService,
        batchService,
        apiBaseUrl,
        Institution.class);
    this.institutionService = institutionService;
    this.institutionMergeService = institutionMergeService;
  }

  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @Parameters(
      value = {
        @Parameter(
            name = "type",
            description = "Type of a GrSciColl institution",
            schema = @Schema(implementation = InstitutionType.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "institutionalGovernance",
            description = "Instutional governance of a GrSciColl institution",
            schema = @Schema(implementation = InstitutionGovernance.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "disciplines",
            description =
                "Discipline of a GrSciColl institution. Accepts multiple values, for example "
                    + "`discipline=ARCHAEOLOGY_PREHISTORIC&discipline=ARCHAEOLOGY_HISTORIC`",
            schema = @Schema(implementation = Discipline.class),
            in = ParameterIn.QUERY)
      })
  @SearchRequestParameters
  @interface InstitutionSearchParameters {}

  @Operation(
      operationId = "getInstitution",
      summary = "Get details of a single institution",
      description = "Details of a single institution.  Also works for deleted institutions.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0200")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "200", description = "Institution found and returned")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{key}")
  @NullToNotFound("/grscicoll/institution/{key}")
  public Institution get(@PathVariable UUID key) {
    return institutionService.get(key);
  }

  // Method overridden only for documentation.
  @Operation(
      operationId = "createInstitution",
      summary = "Create a new institution",
      description = "Creates a new institution.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0201")))
  @ApiResponse(
      responseCode = "201",
      description = "Institution created, new institution's UUID returned")
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  public UUID create(@RequestBody @Trim Institution institution) {
    return super.create(institution);
  }

  // Method overridden only for documentation.
  @Operation(
      operationId = "updateInstitution",
      summary = "Update an existing institution",
      description = "Updates the existing institution.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0202")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "204", description = "Institution updated")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PutMapping(value = "{key}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  public void update(@PathVariable("key") UUID key, @RequestBody @Trim Institution institution) {
    super.update(key, institution);
  }

  // Method overridden only for documentation.
  @Operation(
      operationId = "deleteInstitution",
      summary = "Delete an existing institution",
      description =
          "Deletes an existing institution. The institution entry gets a deleted timestamp but remains registered.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0203")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "204", description = "Institution marked as deleted")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @DeleteMapping("{key}")
  @Override
  public void delete(@PathVariable UUID key) {
    super.delete(key);
  }

  @Operation(
      operationId = "listInstitutions",
      summary = "List all institutions",
      description = "Lists all current institutions (deleted institutions are not listed).",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0100")))
  @InstitutionSearchParameters
  @ApiResponse(responseCode = "200", description = "Institution search successful")
  @ApiResponse(responseCode = "400", description = "Invalid search query provided")
  @GetMapping
  public PagingResponse<Institution> list(InstitutionSearchRequest searchRequest) {
    return institutionService.list(searchRequest);
  }

  private String getExportFileHeader(InstitutionSearchRequest searchRequest, ExportFormat format) {
    String preFileName =
        CsvWriter.notNullJoiner(
            "-",
            searchRequest.getCountry() != null
                ? searchRequest.getCountry().getIso2LetterCode()
                : null,
            searchRequest.getCity(),
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
      operationId = "listInstitutionsExport",
      summary = "Export search across all institutions.",
      description = "Download full-text search results as CSV or TSV.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0102")))
  @CollectionResource.CollectionSearchParameters
  @ApiResponse(responseCode = "200", description = "Institution search successful")
  @ApiResponse(responseCode = "400", description = "Invalid search query provided")
  @GetMapping("export")
  public void export(
      HttpServletResponse response,
      @RequestParam(value = "format", defaultValue = "TSV") ExportFormat format,
      InstitutionSearchRequest searchRequest)
      throws IOException {
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, getExportFileHeader(searchRequest, format));

    try (Writer writer = new BufferedWriter(new OutputStreamWriter(response.getOutputStream()))) {
      CsvWriter.institutions(
              Iterables.institutions(searchRequest, institutionService, EXPORT_LIMIT), format)
          .export(writer);
    }
  }

  @Operation(
      operationId = "listDeleted",
      summary = "Retrieve all deleted institution records",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0500")))
  @ApiResponse(responseCode = "200", description = "List of deleted institution records")
  @InstitutionSearchParameters
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("deleted")
  public PagingResponse<Institution> listDeleted(InstitutionSearchRequest searchRequest) {
    return institutionService.listDeleted(searchRequest);
  }

  @Operation(
      operationId = "suggestInstitutions",
      summary = "Suggest institutions.",
      description =
          "Search that returns up to 20 matching institutions. Results are ordered by relevance. "
              + "The response is smaller than an institution search.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0103")))
  @CommonParameters.QParameter
  @ApiResponse(responseCode = "200", description = "Institution search successful")
  @ApiResponse(responseCode = "400", description = "Invalid search query provided")
  @GetMapping("suggest")
  public List<KeyCodeNameResult> suggest(@RequestParam(value = "q", required = false) String q) {
    return institutionService.suggest(q);
  }

  @Operation(
      operationId = "importCollection",
      summary = "Converts an institution into a collection",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0495")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(
      responseCode = "200",
      description = "Conversion complete, key returned.",
      content = @Content)
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping("{key}/convertToCollection")
  public UUID convertToCollection(
      @PathVariable("key") UUID entityKey, @RequestBody ConvertToCollectionParams params) {
    return institutionMergeService.convertToCollection(
        entityKey, params.getInstitutionForNewCollectionKey(), params.getNameForNewInstitution());
  }

  @Operation(
      operationId = "importInstitution",
      summary = "Import an institution",
      description = "Imports an institution from an organization.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0496")))
  @ApiResponse(
      responseCode = "200",
      description = "Institution imported, key returned.",
      content = @Content)
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping(value = "import", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  public UUID createFromOrganization(@RequestBody @Trim InstitutionImportParams importParams) {
    return institutionService.createFromOrganization(
        importParams.getOrganizationKey(), importParams.getInstitutionCode());
  }

  @Hidden
  @GetMapping("sourceableFields")
  public List<SourceableField> getSourceableFields() {
    return MasterSourceUtils.INSTITUTION_SOURCEABLE_FIELDS;
  }
}
