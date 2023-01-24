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
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.InstitutionImportParams;
import org.gbif.api.model.collections.SourceableField;
import org.gbif.api.model.collections.merge.ConvertToCollectionParams;
import org.gbif.api.model.collections.request.InstitutionSearchRequest;
import org.gbif.api.model.collections.suggestions.InstitutionChangeSuggestion;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.util.iterables.Iterables;
import org.gbif.registry.service.collections.duplicates.InstitutionDuplicatesService;
import org.gbif.registry.service.collections.merge.InstitutionMergeService;
import org.gbif.registry.service.collections.suggestions.InstitutionChangeSuggestionService;
import org.gbif.registry.service.collections.utils.MasterSourceUtils;
import org.gbif.registry.ws.export.CsvWriter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;

/**
 * Class that acts both as the WS endpoint for {@link Institution} entities and also provides an *
 * implementation of {@link InstitutionService}.
 */
@io.swagger.v3.oas.annotations.tags.Tag(
  name = "Institutions",
  description = " This API provides CRUD services for the institution entity. An institution can have a list of " +
    "contacts, which are represented by the person entity. They can also have tags and identifiers. ",
  extensions = @io.swagger.v3.oas.annotations.extensions.Extension(
    name = "Order", properties = @ExtensionProperty(name = "Order", value = "1100")))
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
      InstitutionChangeSuggestionService institutionChangeSuggestionService) {
    super(
        institutionMergeService,
        institutionService,
        institutionChangeSuggestionService,
        duplicatesService,
        Institution.class);
    this.institutionService = institutionService;
    this.institutionMergeService = institutionMergeService;
  }

  @GetMapping("{key}")
  @NullToNotFound("/grscicoll/institution/{key}")
  public Institution get(@PathVariable UUID key) {
    return institutionService.get(key);
  }

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

  @GetMapping("deleted")
  public PagingResponse<Institution> listDeleted(
      @RequestParam(value = "replacedBy", required = false) UUID replacedBy, Pageable page) {
    return institutionService.listDeleted(replacedBy, page);
  }

  @GetMapping("suggest")
  public List<KeyCodeNameResult> suggest(@RequestParam(value = "q", required = false) String q) {
    return institutionService.suggest(q);
  }

  @PostMapping("{key}/convertToCollection")
  public UUID convertToCollection(
      @PathVariable("key") UUID entityKey, @RequestBody ConvertToCollectionParams params) {
    return institutionMergeService.convertToCollection(
        entityKey, params.getInstitutionForNewCollectionKey(), params.getNameForNewInstitution());
  }

  @PostMapping(value = "import", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  public UUID createFromOrganization(@RequestBody @Trim InstitutionImportParams importParams) {
    return institutionService.createFromOrganization(
        importParams.getOrganizationKey(), importParams.getInstitutionCode());
  }

  @GetMapping("sourceableFields")
  public List<SourceableField> getSourceableFields() {
    return MasterSourceUtils.INSTITUTION_SOURCEABLE_FIELDS;
  }
}
