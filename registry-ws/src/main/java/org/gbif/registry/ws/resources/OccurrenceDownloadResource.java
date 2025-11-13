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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import org.gbif.api.annotation.NullToNotFound;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.occurrence.DownloadStatistics;
import org.gbif.api.model.occurrence.DownloadType;
import org.gbif.api.model.registry.CountryOccurrenceDownloadUsage;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.model.registry.OrganizationOccurrenceDownloadUsage;
import org.gbif.api.service.common.IdentityAccessService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.CountryUsageSortField;
import org.gbif.api.vocabulary.DatasetUsageSortField;
import org.gbif.api.vocabulary.OrganizationUsageSortField;
import org.gbif.api.vocabulary.SortOrder;
import org.gbif.registry.doi.DownloadDoiDataCiteHandlingService;
import org.gbif.registry.persistence.mapper.DatasetOccurrenceDownloadMapper;
import org.gbif.registry.persistence.mapper.DownloadStatisticsMapper;
import org.gbif.registry.persistence.mapper.OccurrenceDownloadMapper;
import org.gbif.registry.ws.provider.PartialDate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Occurrence download resource/web service. */
@Validated
@RestController("occurrenceDownloadResource")
@RequestMapping(value = "occurrence/download", produces = MediaType.APPLICATION_JSON_VALUE)
public class OccurrenceDownloadResource extends BaseDownloadResource {

  public OccurrenceDownloadResource(
      OccurrenceDownloadMapper occurrenceDownloadMapper,
      DatasetOccurrenceDownloadMapper datasetOccurrenceDownloadMapper,
      DownloadStatisticsMapper downloadStatisticsMapper,
      @Lazy DownloadDoiDataCiteHandlingService doiDataCiteHandlingService,
      @Qualifier("baseIdentityAccessService") IdentityAccessService identityService) {
    super(
        occurrenceDownloadMapper,
        datasetOccurrenceDownloadMapper,
        downloadStatisticsMapper,
        doiDataCiteHandlingService,
        identityService,
        DownloadType.OCCURRENCE);
  }

  @Tag(name = "Occurrence downloads")
  @Operation(
      operationId = "getOccurrenceDownloadByKey",
      summary = "Information about an occurrence download",
      description =
          "Retrieves the status (in-progress, complete, etc), definition and location of an occurrence "
              + "download.  Authorized users see additional details on their own downloads.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0110")))
  @Parameter(
      name = "statistics",
      description = "If true it also shows number of organizations and countries.",
      in = ParameterIn.QUERY)
  @DownloadKeyParameter
  @ApiResponse(responseCode = "200", description = "Occurrence download information.")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{key}")
  @NullToNotFound(useUrlMapping = true)
  @Override
  public Download getByKey(
      @NotNull @PathVariable("key") String key,
      @RequestParam(value = "statistics", required = false) Boolean statistics) {
    return super.getByKey(key, statistics);
  }

  @Tag(name = "Occurrence downloads")
  @Operation(
      operationId = "getOccurrenceDownloadByDOI",
      summary = "Information about an occurrence download",
      description =
          "Retrieves the status (in-progress, complete, etc), definition and location of an occurrence "
              + "download.  Authorized users see additional details on their own downloads.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0111")))
  @DoiParameters
  @ApiResponse(responseCode = "200", description = "Occurrence download information.")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{prefix}/{suffix}")
  @NullToNotFound(useUrlMapping = true)
  @Override
  public Download getByDoi(
      @NotNull @PathVariable String prefix, @NotNull @PathVariable String suffix) {
    return super.getByDoi(prefix, suffix);
  }

  @Tag(name = "Occurrence downloads")
  @Operation(
      operationId = "listOccurrenceDownloadsByUser",
      summary = "Lists all occurrence downloads from a user",
      description =
          "Retrieves the status, definitions and locations of all occurrence downloads by your own user.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0120")))
  @ListByUserCommonDocs
  @GetMapping("user/{user}")
  @Override
  public PagingResponse<Download> listByUser(
      @PathVariable String user,
      Pageable page,
      @RequestParam(value = "status", required = false) Set<Download.Status> status,
      @RequestParam(value = "from", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime from,
      @RequestParam(
              value = "statistics",
              required = false,
              defaultValue = "true") // true by default to keep backwards compatibility
          Boolean statistics) {
    return super.listByUser(user, page, status, from, statistics);
  }

  @Tag(name = "Occurrence downloads")
  @Operation(
      operationId = "countOccurrenceDownloadsByUser",
      summary = "Counts all downloads from a user",
      description = "Retrieves the counts of occurrence downloads done by the user.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0121")))
  @CountByUserCommonDocs
  @GetMapping("user/{user}/count")
  @Override
  public long countByUser(
      @PathVariable String user,
      @RequestParam(value = "status", required = false) Set<Download.Status> status,
      @RequestParam(value = "from", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime from) {
    return super.countByUser(user, status, from);
  }

  @Tag(name = "Occurrence downloads")
  @Operation(
      operationId = "listDatasetUsagesByOccurrenceDownloadDOI",
      summary = "Lists datasets present in an occurrence download",
      description = "Shows the datasets with occurrences present in the given occurrence download.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0130")))
  @DoiParameters
  @Pageable.OffsetLimitParameters
  @ApiResponse(
      responseCode = "200",
      description = "Dataset usage within an occurrence download information.")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{prefix}/{suffix}/datasets")
  @Override
  public PagingResponse<DatasetOccurrenceDownloadUsage> listDatasetUsagesByDoi(
      @PathVariable("prefix") String prefix, @PathVariable("suffix") String suffix, Pageable page) {
    return super.listDatasetUsagesByDoi(prefix, suffix, page);
  }

  @Tag(name = "Occurrence downloads")
  @Operation(
      operationId = "listDatasetUsagesByOccurrenceDownloadKey",
      summary = "Lists datasets present in an occurrence download",
      description = "Shows the datasets with occurrences present in the given occurrence download.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0131")))
  @ListDatasetUsagesCommonDocs
  @GetMapping("{key}/datasets")
  @Override
  public PagingResponse<DatasetOccurrenceDownloadUsage> listDatasetUsages(
      @PathVariable("key") String key,
      @RequestParam(value = "datasetTitle", required = false) String datasetTitle,
      @RequestParam(value = "sortBy", required = false) DatasetUsageSortField sortBy,
      @RequestParam(value = "sortOrder", required = false) SortOrder sortOrder,
      Pageable page) {
    return super.listDatasetUsages(key, datasetTitle, sortBy, sortOrder, page);
  }

  @Tag(name = "Occurrence downloads")
  @Operation(
      operationId = "exportDatasetUsagesByOccurrenceDownloadKey",
      summary = "Exports datasets present in an occurrence download in TSV or CSV format",
      description =
          "Shows the datasets with occurrences present in the given occurrence download in TSV or CSV format.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0132")))
  @DownloadKeyParameter
  @Parameter(
      name = "format",
      description = "The export format.",
      schema = @Schema(implementation = ExportFormat.class),
      in = ParameterIn.QUERY)
  @ApiResponse(
      responseCode = "200",
      description = "Dataset usage within an occurrence download information.")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{key}/datasets/export")
  @Override
  public void exportListDatasetUsagesByKey(
      HttpServletResponse response,
      @PathVariable("key") String key,
      @RequestParam(value = "format", defaultValue = "TSV") ExportFormat format)
      throws IOException {
    super.exportListDatasetUsagesByKey(response, key, format);
  }

  @Tag(name = "Organization usages occurrence downloads")
  @Operation(
      operationId = "listOrganizationUsagesByOccurrenceDownloadKey",
      summary = "Lists organizations present in an occurrence download",
      description =
          "Shows the organizations with occurrences present in the given occurrence download.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0131")))
  @GetMapping("{key}/organizations")
  @Override
  public PagingResponse<OrganizationOccurrenceDownloadUsage> listOrganizationUsages(
      @PathVariable("key") String downloadKey,
      @RequestParam(value = "organizationTitle", required = false) String organizationTitle,
      @RequestParam(value = "sortBy", required = false) OrganizationUsageSortField sortBy,
      @RequestParam(value = "sortOrder", required = false) SortOrder sortOrder,
      Pageable page) {
    return super.listOrganizationUsages(downloadKey, organizationTitle, sortBy, sortOrder, page);
  }

  @Tag(name = "Country usages occurrence downloads")
  @Operation(
      operationId = "listCountryUsagesByOccurrenceDownloadKey",
      summary = "Lists countries present in an occurrence download",
      description =
          "Shows the countries with occurrences present in the given occurrence download.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0131")))
  @ListCountryUsagesCommonDocs
  @GetMapping("{key}/countries")
  @Override
  public PagingResponse<CountryOccurrenceDownloadUsage> listCountryUsages(
      @PathVariable("key") String downloadKey,
      @RequestParam(value = "sortBy", required = false) CountryUsageSortField sortBy,
      @RequestParam(value = "sortOrder", required = false) SortOrder sortOrder,
      Pageable page) {
    return super.listCountryUsages(downloadKey, sortBy, sortOrder, page);
  }

  @Tag(name = "Occurrence downloads")
  @Operation(
      operationId = "getOccurrenceDownloadCitationByKey",
      summary = "Shows the citation for an occurrence download",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0140")))
  @DownloadKeyParameter
  @ApiResponse(responseCode = "200", description = "Download citation.")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{key}/citation")
  @NullToNotFound(useUrlMapping = true)
  @Override
  public String getCitationByKey(@NotNull @PathVariable("key") String key) {
    return super.getCitationByKey(key);
  }

  @Tag(name = "Occurrence downloads")
  @Operation(
      operationId = "getOccurrenceDownloadCitationByDOI",
      summary = "Shows the citation for an occurrence download",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0141")))
  @DoiParameters
  @ApiResponse(responseCode = "200", description = "Download citation.")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{prefix}/{suffix}/citation")
  @NullToNotFound(useUrlMapping = true)
  @Override
  public String getCitationByDoi(
      @NotNull @PathVariable("prefix") String prefix,
      @NotNull @PathVariable("suffix") String suffix) {
    return super.getCitationByDoi(prefix, suffix);
  }

  @Tag(name = "Occurrence download statistics")
  @Operation(
      operationId = "getOccurrenceDownloadsByUserCountry",
      summary = "Summarizes downloads by month, grouped by the user's country, territory or island",
      description =
          "Provides counts of user downloads by month, grouped by the user's ISO 3166-2 country,"
              + " territory or island.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0210")))
  @FromToParameters
  @Parameters(
      value = {
        @Parameter(
            name = "userCountry",
            description = "The ISO 3166-2 code for the user's country, territory or island.",
            schema = @Schema(implementation = Country.class),
            in = ParameterIn.QUERY)
      })
  @ApiResponse(responseCode = "200", description = "Download statistics.")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("statistics/downloadsByUserCountry")
  @Override
  public Map<Integer, Map<Integer, Long>> getDownloadsByUserCountry(
      @PartialDate Date fromDate, @PartialDate Date toDate, Country userCountry) {
    return super.getDownloadsByUserCountry(fromDate, toDate, userCountry);
  }

  @Tag(name = "Occurrence download statistics")
  @Operation(
      operationId = "getOccurrenceDownloadedRecordsBySource",
      summary = "Summarize downloaded record totals by source",
      description = "Summarizes downloaded record totals by source, e.g. www.gbif.org or APIs.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0240")))
  @FromToParameters
  @Parameter(
      name = "source",
      description = "Restrict to a particular source",
      in = ParameterIn.QUERY)
  @ApiResponse(responseCode = "200", description = "Download statistics.")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("statistics/downloadsBySource")
  @Override
  public Map<Integer, Map<Integer, Long>> getDownloadsBySource(
      @PartialDate Date fromDate, @PartialDate Date toDate, String source) {
    return super.getDownloadsBySource(fromDate, toDate, source);
  }

  @Tag(name = "Occurrence download statistics")
  @Operation(
      operationId = "getOccurrenceDownloadedRecordsByDataset",
      summary = "Summarize downloaded records by dataset",
      description =
          "Summarizes downloaded record totals by month, filtered by a publishing organization's country, territory or island and/or a single dataset",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0220")))
  @GetDownloadedRecordsByDatasetCommonDocs
  @GetMapping("statistics/downloadedRecordsByDataset")
  @Override
  public Map<Integer, Map<Integer, Long>> getDownloadedRecordsByDataset(
      @PartialDate Date fromDate,
      @PartialDate Date toDate,
      Country publishingCountry,
      @RequestParam(value = "datasetKey", required = false) UUID datasetKey,
      @RequestParam(value = "publishingOrgKey", required = false) UUID publishingOrgKey) {
    return super.getDownloadedRecordsByDataset(
        fromDate, toDate, publishingCountry, datasetKey, publishingOrgKey);
  }

  @Tag(name = "Occurrence download statistics")
  @Operation(
      operationId = "getOccurrenceDownloadedRecordsByDataset",
      summary = "Summarize downloads by dataset",
      description =
          "Summarizes downloads by month, filtered by a publishing organization's country, territory or island and/or a single dataset",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0230")))
  @GetDownloadsByDatasetCommonDocs
  @GetMapping("statistics/downloadsByDataset")
  @Override
  public Map<Integer, Map<Integer, Long>> getDownloadsByDataset(
      @PartialDate Date fromDate,
      @PartialDate Date toDate,
      Country publishingCountry,
      @RequestParam(value = "datasetKey", required = false) UUID datasetKey,
      @RequestParam(value = "publishingOrgKey", required = false) UUID publishingOrgKey) {
    return super.getDownloadsByDataset(
        fromDate, toDate, publishingCountry, datasetKey, publishingOrgKey);
  }

  @Tag(name = "Occurrence download statistics")
  @Operation(
      operationId = "getOccurrenceDownloadedStatistics",
      summary = "Provides summarized download statistics",
      description =
          "Filters for downloads matching the provided criteria, then provide counts by year, month and "
              + "dataset of the total number of downloads, and the total number of records included in those downloads.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0200")))
  @GetDownloadStatisticsCommonDocs
  @Pageable.OffsetLimitParameters
  @GetMapping("statistics")
  @Override
  public PagingResponse<DownloadStatistics> getDownloadStatistics(
      @PartialDate Date fromDate,
      @PartialDate Date toDate,
      Country publishingCountry,
      @RequestParam(value = "datasetKey", required = false) UUID datasetKey,
      @RequestParam(value = "publishingOrgKey", required = false) UUID publishingOrgKey,
      Pageable page) {
    return super.getDownloadStatistics(
        fromDate, toDate, publishingCountry, datasetKey, publishingOrgKey, page);
  }

  @Tag(name = "Occurrence download statistics")
  @Operation(
      operationId = "exportOccurrenceDownloadedStatistics",
      summary = "Export summary of occurrence downloads",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0201")))
  @GetDownloadStatisticsCommonDocs
  @GetMapping("statistics/export")
  @Override
  public void getDownloadStatistics(
      HttpServletResponse response,
      @RequestParam(value = "format", defaultValue = "TSV") ExportFormat format,
      @PartialDate Date fromDate,
      @PartialDate Date toDate,
      Country publishingCountry,
      @RequestParam(value = "datasetKey", required = false) UUID datasetKey,
      @RequestParam(value = "publishingOrgKey", required = false) UUID publishingOrgKey)
      throws IOException {
    super.getDownloadStatistics(
        response, format, fromDate, toDate, publishingCountry, datasetKey, publishingOrgKey);
  }
}
