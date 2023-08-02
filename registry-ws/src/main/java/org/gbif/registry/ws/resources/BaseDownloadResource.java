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
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.common.search.Facet;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.occurrence.DownloadStatistics;
import org.gbif.api.model.occurrence.DownloadType;
import org.gbif.api.model.registry.CountryOccurrenceDownloadUsage;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.model.registry.OrganizationOccurrenceDownloadUsage;
import org.gbif.api.model.registry.PostPersist;
import org.gbif.api.model.registry.PrePersist;
import org.gbif.api.service.common.IdentityAccessService;
import org.gbif.api.service.registry.OccurrenceDownloadService;
import org.gbif.api.util.iterables.Iterables;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.CountryUsageSortField;
import org.gbif.api.vocabulary.DatasetUsageSortField;
import org.gbif.api.vocabulary.License;
import org.gbif.api.vocabulary.OrganizationUsageSortField;
import org.gbif.api.vocabulary.SortOrder;
import org.gbif.registry.doi.DoiIssuingService;
import org.gbif.registry.doi.DownloadDoiDataCiteHandlingService;
import org.gbif.registry.persistence.mapper.DatasetOccurrenceDownloadMapper;
import org.gbif.registry.persistence.mapper.OccurrenceDownloadMapper;
import org.gbif.registry.ws.export.CsvWriter;
import org.gbif.registry.ws.provider.PartialDate;
import org.gbif.registry.ws.util.DateUtils;
import org.gbif.ws.WebApplicationException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Iterators;

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
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import static org.gbif.registry.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.security.util.DownloadSecurityUtils.checkUserIsInSecurityContext;
import static org.gbif.registry.security.util.DownloadSecurityUtils.clearSensitiveData;

/** Base download resource/web service. */
/*
 * OpenAPI documentation:
 *
 * This class has OpenAPI/SpringDoc method annotations, but the non-stats
 * tag is the same as in occurrence→occurrence-ws→OccurrenceDownloadResource.
 *
 * The result is manually moved from the Registry OpenAPI document to the
 * Occurrence OpenAPI document.
 */
@Slf4j
public class BaseDownloadResource implements OccurrenceDownloadService {

  private final OccurrenceDownloadMapper occurrenceDownloadMapper;
  private final DatasetOccurrenceDownloadMapper datasetOccurrenceDownloadMapper;
  private final IdentityAccessService identityService;
  private final DoiIssuingService doiIssuingService;
  private final DownloadDoiDataCiteHandlingService doiDataCiteHandlingService;
  private final DownloadType downloadType;

  // Page size to iterate over dataset usages
  private static final int BATCH_SIZE = 5_000;

  // Page size to iterate over download stats export service
  private static final int STATS_EXPORT_LIMIT = 7_500;

  // Page size to iterate over download stats export service
  private static final int EXPORT_LIMIT = 5_000;

  // Export header prefix
  private static final String FILE_HEADER_PRE = "attachment; filename=datasets_download_usage_";

  // Download stats file header
  private static final String EXPORT_FILE_HEADER_PRE = "attachment; filename=downloads_statistics.";

  private static final Logger LOG = LoggerFactory.getLogger(BaseDownloadResource.class);

  private static final Marker NOTIFY_ADMIN = MarkerFactory.getMarker("NOTIFY_ADMIN");

  // For short citation
  private static final SimpleDateFormat LONG_UN = new SimpleDateFormat("d MMMMM yyyy", Locale.UK);

  private static final EnumSet<Download.Status> FAILED_STATES =
      EnumSet.of(Download.Status.KILLED, Download.Status.CANCELLED, Download.Status.FAILED);

  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @Parameter(name = "key", description = "The key of the download", in = ParameterIn.PATH)
  @interface DownloadKeyParameter {}

  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @Parameters(
      value = {
        @Parameter(
            name = "prefix",
            description = "The DOI prefix of the download, 10.15468 for GBIF downloads",
            in = ParameterIn.PATH),
        @Parameter(
            name = "suffix",
            description = "The DOI suffix of the download, begins 'dl.' for GBIF downloads",
            in = ParameterIn.PATH)
      })
  @interface DoiParameters {}

  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @Parameters(
      value = {
        @Parameter(
            name = "fromDate",
            description = "The year and month (YYYY-MM) to start from.",
            schema = @Schema(implementation = Date.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "toDate",
            description = "The year and month (YYYY-MM) to end at.",
            schema = @Schema(implementation = Date.class),
            in = ParameterIn.QUERY)
      })
  @interface FromToParameters {}

  public BaseDownloadResource(
      OccurrenceDownloadMapper occurrenceDownloadMapper,
      DatasetOccurrenceDownloadMapper datasetOccurrenceDownloadMapper,
      DoiIssuingService doiIssuingService,
      @Lazy DownloadDoiDataCiteHandlingService doiDataCiteHandlingService,
      @Qualifier("baseIdentityAccessService") IdentityAccessService identityService,
      DownloadType downloadType) {
    this.occurrenceDownloadMapper = occurrenceDownloadMapper;
    this.datasetOccurrenceDownloadMapper = datasetOccurrenceDownloadMapper;
    this.doiIssuingService = doiIssuingService;
    this.doiDataCiteHandlingService = doiDataCiteHandlingService;
    this.identityService = identityService;
    this.downloadType = downloadType;
  }

  @Hidden // Users create downloads through occurrence-ws.
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PrePersist.class, Default.class})
  @Trim
  @Secured(ADMIN_ROLE)
  @Override
  public void create(@RequestBody @Trim Download occurrenceDownload) {
    try {
      occurrenceDownload.setDoi(doiIssuingService.newDownloadDOI());
      occurrenceDownload.setLicense(License.UNSPECIFIED);
      occurrenceDownload.getRequest().setType(downloadType);
      occurrenceDownloadMapper.create(occurrenceDownload);
    } catch (Exception ex) {
      LOG.error(NOTIFY_ADMIN, "Error creating download", ex);
      throw new RuntimeException(ex);
    }
  }

  private void assertDownloadType(Download download) {
    if (download != null && downloadType != download.getRequest().getType()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Wrong download type for this endpoint");
    }
  }

  @Override
  public Download get(String keyOrDoi) {
    Download download = getByKey(keyOrDoi, false);

    if (download == null && DOI.isParsable(keyOrDoi)) { // maybe it's a DOI?
      DOI doi = new DOI(keyOrDoi);
      download = getByDoi(doi.getPrefix(), doi.getSuffix());
    }

    assertDownloadType(download);

    return download;
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
  public Download getByKey(
      @NotNull @PathVariable("key") String key,
      @RequestParam(value = "statistics", required = false) Boolean statistics) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    Download download;
    if (Boolean.TRUE.equals(statistics)) {
      download = occurrenceDownloadMapper.getWithCounts(key);
    } else {
      download = occurrenceDownloadMapper.get(key);
    }
    clearSensitiveData(authentication, download);
    assertDownloadType(download);

    // the doi is removed from datacite when the download is in a failed state and should be hidden.
    // It is also removed in the update method but old downloads still keep it in the DB
    // https://github.com/gbif/registry/issues/367
    if (FAILED_STATES.contains(download.getStatus())) {
      download.setDoi(null);
    }

    return download;
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
  public Download getByDoi(
      @NotNull @PathVariable String prefix, @NotNull @PathVariable String suffix) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    Download download = occurrenceDownloadMapper.getByDOI(new DOI(prefix, suffix));
    clearSensitiveData(authentication, download);
    assertDownloadType(download);

    return download;
  }

  /** Lists all the downloads. This operation can be executed by role ADMIN only. */
  @Hidden // Admin method hidden
  @GetMapping
  @Secured(ADMIN_ROLE)
  @Override
  public PagingResponse<Download> list(
      Pageable page,
      @RequestParam(value = "status", required = false) Set<Download.Status> status,
      @RequestParam(value = "source", required = false) String source) {
    return new PagingResponse<>(
        page,
        (long) occurrenceDownloadMapper.count(status, downloadType, source),
        occurrenceDownloadMapper.list(page, status, downloadType, source));
  }

  @Hidden // Admin method hidden
  @GetMapping("/count")
  @Secured(ADMIN_ROLE)
  @Override
  public long count(
      @RequestParam(value = "status", required = false) Set<Download.Status> status,
      @RequestParam(value = "source", required = false) String source) {
    return occurrenceDownloadMapper.count(status, downloadType, source);
  }

  /**
   * Lists all the downloads. Users will see only their own downloads; an admin user can see other
   * users' downloads.
   */
  @Tag(name = "Occurrence downloads")
  @Operation(
      operationId = "listOccurrenceDownloadsByUser",
      summary = "Lists all downloads from a user",
      description =
          "Retrieves the status, definitions and locations of all occurrence download by your own user.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0120")))
  @Parameters(
      value = {
        @Parameter(
            name = "user",
            description = "Username (administrator account required to see other users).",
            in = ParameterIn.PATH),
        @Parameter(
            name = "status",
            description = "List of statuses to filter by.",
            schema = @Schema(implementation = Download.Status.class),
            in = ParameterIn.QUERY,
            explode = Explode.TRUE),
        @Parameter(
            name = "from",
            description = "Date time in ISO format to filter downloads by its creation date.",
            in = ParameterIn.QUERY),
        @Parameter(
            name = "statistics",
            description =
                "If true it returns the counts of datasets, organizations and countries. By default it's true to maintain backwards compatibility.",
            in = ParameterIn.QUERY)
      })
  @Pageable.OffsetLimitParameters
  @ApiResponse(responseCode = "200", description = "Occurrence download information.")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
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

    log.debug(
        "List downloads for user {}, status {}, from {} and statistics {}",
        user,
        status,
        from,
        statistics);

    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    checkUserIsInSecurityContext(user, authentication);

    long count = occurrenceDownloadMapper.countByUser(user, status, downloadType, from);

    List<Download> downloads;
    if (Boolean.FALSE.equals(statistics)) {
      downloads =
          occurrenceDownloadMapper.listByUserLightweight(user, page, status, downloadType, from);
    } else {
      downloads = occurrenceDownloadMapper.listByUser(user, page, status, downloadType, from);
    }

    return new PagingResponse<>(page, count, downloads);
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
  @Parameters(
      value = {
        @Parameter(
            name = "user",
            description = "Username (administrator account required to see other users).",
            in = ParameterIn.PATH),
        @Parameter(
            name = "status",
            description = "List of statuses to filter by.",
            schema = @Schema(implementation = Download.Status.class),
            in = ParameterIn.QUERY,
            explode = Explode.TRUE),
        @Parameter(
            name = "from",
            description = "Date time in ISO format to filter downloads by its creation date.",
            in = ParameterIn.QUERY)
      })
  @Pageable.OffsetLimitParameters
  @ApiResponse(responseCode = "200", description = "Occurrence downloads count.")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @GetMapping("user/{user}/count")
  @Override
  public long countByUser(
      @PathVariable String user,
      @RequestParam(value = "status", required = false) Set<Download.Status> status,
      @RequestParam(value = "from", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime from) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    checkUserIsInSecurityContext(user, authentication);
    return occurrenceDownloadMapper.countByUser(user, status, downloadType, from);
  }

  /**
   * Lists downloads which may be eligible for erasure, based on age, size etc. This operation can
   * be executed by role ADMIN only.
   *
   * <p>Internal use only; this method may be changed without warning.
   */
  @Hidden
  @GetMapping("internal/eraseAfter")
  @Secured(ADMIN_ROLE)
  @Override
  public PagingResponse<Download> listByEraseAfter(
      Pageable page,
      @RequestParam(value = "eraseAfter", required = false) String eraseAfterAsString,
      @RequestParam(value = "size", required = false) Long size,
      @RequestParam(value = "erasureNotification", required = false)
          String erasureNotificationAsString) {
    Date eraseAfter = DateUtils.STRING_TO_DATE.apply(eraseAfterAsString);
    Date erasureNotification = DateUtils.STRING_TO_DATE.apply(erasureNotificationAsString);

    return new PagingResponse<>(
        page,
        (long) occurrenceDownloadMapper.countByEraseAfter(eraseAfter, size, erasureNotification),
        occurrenceDownloadMapper.listByEraseAfter(page, eraseAfter, size, erasureNotification));
  }

  @SuppressWarnings("MVCPathVariableInspection")
  @Hidden
  @PutMapping(
      value = {"", "{key}", "{prefix}/{suffix}"},
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PostPersist.class, Default.class})
  @Transactional
  @Override
  public void update(@RequestBody Download download) {
    // The current download is retrieved because its user could be modified during the update
    Download currentDownload = get(download.getKey());
    Preconditions.checkNotNull(currentDownload);
    assertDownloadType(download);

    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    checkUserIsInSecurityContext(currentDownload.getRequest().getCreator(), authentication);

    GbifUser user = identityService.get(authentication.getName());
    doiDataCiteHandlingService.downloadChanged(download, currentDownload, user);

    if (FAILED_STATES.contains(download.getStatus())) {
      // we remove the doi from the DB in failed states
      download.setDoi(null);
    }

    occurrenceDownloadMapper.update(download);
  }

  @Override
  public PagingResponse<DatasetOccurrenceDownloadUsage> listDatasetUsages(
      String keyOrDoi, Pageable page) {
    Download download = get(keyOrDoi);
    return listDatasetUsagesInternal(download.getKey(), page, download);
  }

  @Tag(name = "Occurrence downloads")
  @Operation(
      operationId = "listDatasetUsagesByDownloadDOI",
      summary = "Lists datasets present in a download",
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
  public PagingResponse<DatasetOccurrenceDownloadUsage> listDatasetUsagesByDoi(
      @PathVariable("prefix") String prefix, @PathVariable("suffix") String suffix, Pageable page) {
    Download download = getByDoi(prefix, suffix);
    return listDatasetUsagesInternal(download.getKey(), page, download);
  }

  @Tag(name = "Occurrence downloads")
  @Operation(
      operationId = "listDatasetUsagesByDownloadKey",
      summary = "Lists datasets present in a download",
      description = "Shows the datasets with occurrences present in the given occurrence download.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0131")))
  @Parameters(
      value = {
        @Parameter(
            name = "datasetTitle",
            description = "Title of the dataset to filter by.",
            in = ParameterIn.QUERY,
            schema = @Schema(implementation = String.class)),
        @Parameter(
            name = "sortBy",
            description = "Field to sort the results by.",
            in = ParameterIn.QUERY,
            schema = @Schema(implementation = DatasetUsageSortField.class)),
        @Parameter(
            name = "sortOrder",
            description = "Sort order. Only taken into account when sortBy is used.",
            in = ParameterIn.QUERY,
            schema = @Schema(implementation = SortOrder.class))
      })
  @DownloadKeyParameter
  @Pageable.OffsetLimitParameters
  @ApiResponse(
      responseCode = "200",
      description = "Dataset usage within an occurrence download information.")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{key}/datasets")
  @Override
  public PagingResponse<DatasetOccurrenceDownloadUsage> listDatasetUsages(
      @PathVariable("key") String key,
      @RequestParam(value = "datasetTitle", required = false) String datasetTitle,
      @RequestParam(value = "sortBy", required = false) DatasetUsageSortField sortBy,
      @RequestParam(value = "sortOrder", required = false) SortOrder sortOrder,
      Pageable page) {
    Download download = get(key);
    return listDatasetUsagesInternal(key, page, download, datasetTitle, sortBy, sortOrder);
  }

  @Tag(name = "Occurrence downloads")
  @Operation(
      operationId = "exportDatasetUsagesByDownloadKey",
      summary = "Exports datasets present in a download in TSV or CSV format",
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
  public void exportListDatasetUsagesByKey(
      HttpServletResponse response,
      @PathVariable("key") String key,
      @RequestParam(value = "format", defaultValue = "TSV") ExportFormat format)
      throws IOException {

    response.setHeader(
        HttpHeaders.CONTENT_DISPOSITION, FILE_HEADER_PRE + key + '.' + format.name().toLowerCase());

    try (Writer writer = new BufferedWriter(new OutputStreamWriter(response.getOutputStream()))) {
      CsvWriter.datasetOccurrenceDownloadUsageCsvWriter(
              Iterables.datasetOccurrenceDownloadUsages(this, key, EXPORT_LIMIT), format)
          .export(writer);
    }
  }

  private PagingResponse<DatasetOccurrenceDownloadUsage> listDatasetUsagesInternal(
      String key, Pageable page, Download download) {
    return listDatasetUsagesInternal(key, page, download, null, null, null);
  }

  private PagingResponse<DatasetOccurrenceDownloadUsage> listDatasetUsagesInternal(
      String key,
      Pageable page,
      Download download,
      String datasetTitle,
      DatasetUsageSortField sortBy,
      SortOrder sortOrder) {
    if (download != null) {
      page = page != null ? page : new PagingRequest();
      List<DatasetOccurrenceDownloadUsage> usages =
          datasetOccurrenceDownloadMapper.listByDownload(
              key, Strings.emptyToNull(datasetTitle), sortBy, sortOrder, page);
      final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      clearSensitiveData(authentication, usages);
      return new PagingResponse<>(page, download.getNumberDatasets(), usages);
    }
    throw new WebApplicationException("Download was not found", HttpStatus.NOT_FOUND);
  }

  @Tag(name = "Organization usages occurrence downloads")
  @Operation(
      operationId = "listOrganizationUsagesByDownloadKey",
      summary = "Lists organizations present in a download",
      description =
          "Shows the organizations with occurrences present in the given occurrence download.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0131")))
  @Parameters(
      value = {
        @Parameter(
            name = "organizationTitle",
            description = "Title of the organization to filter by.",
            in = ParameterIn.QUERY,
            schema = @Schema(implementation = String.class)),
        @Parameter(
            name = "sortBy",
            description = "Field to sort the results by.",
            in = ParameterIn.QUERY,
            schema = @Schema(implementation = OrganizationUsageSortField.class)),
        @Parameter(
            name = "sortOrder",
            description = "Sort order. Only taken into account when sortBy is used.",
            in = ParameterIn.QUERY,
            schema = @Schema(implementation = SortOrder.class))
      })
  @DownloadKeyParameter
  @Pageable.OffsetLimitParameters
  @ApiResponse(
      responseCode = "200",
      description = "Organization usage within an occurrence download information.")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{key}/organizations")
  @Override
  public PagingResponse<OrganizationOccurrenceDownloadUsage> listOrganizationUsages(
      @PathVariable("key") String downloadKey,
      @RequestParam(value = "organizationTitle", required = false) String organizationTitle,
      @RequestParam(value = "sortBy", required = false) OrganizationUsageSortField sortBy,
      @RequestParam(value = "sortOrder", required = false) SortOrder sortOrder,
      Pageable page) {
    Download download = get(downloadKey);
    if (download != null) {
      page = page != null ? page : new PagingRequest();

      List<OrganizationOccurrenceDownloadUsage> results =
          datasetOccurrenceDownloadMapper.listOrganizationsByDownload(
              downloadKey, Strings.emptyToNull(organizationTitle), sortBy, sortOrder, page);

      int count =
          datasetOccurrenceDownloadMapper.countOrganizationsByDownload(
              downloadKey, Strings.emptyToNull(organizationTitle));

      return new PagingResponse<>(page, (long) count, results);
    }
    throw new WebApplicationException("Download was not found", HttpStatus.NOT_FOUND);
  }

  @Tag(name = "Country usages occurrence downloads")
  @Operation(
      operationId = "listCountryUsagesByDownloadKey",
      summary = "Lists countries present in a download",
      description =
          "Shows the countries with occurrences present in the given occurrence download.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0131")))
  @Parameters(
      value = {
        @Parameter(
            name = "sortBy",
            description = "Field to sort the results by.",
            in = ParameterIn.QUERY,
            schema = @Schema(implementation = CountryUsageSortField.class)),
        @Parameter(
            name = "sortOrder",
            description = "Sort order. Only taken into account when sortBy is used.",
            in = ParameterIn.QUERY,
            schema = @Schema(implementation = SortOrder.class))
      })
  @DownloadKeyParameter
  @Pageable.OffsetLimitParameters
  @ApiResponse(
      responseCode = "200",
      description = "Country usage within an occurrence download information.")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{key}/countries")
  @Override
  public PagingResponse<CountryOccurrenceDownloadUsage> listCountryUsages(
      @PathVariable("key") String downloadKey,
      @RequestParam(value = "sortBy", required = false) CountryUsageSortField sortBy,
      @RequestParam(value = "sortOrder", required = false) SortOrder sortOrder,
      Pageable page) {
    Download download = get(downloadKey);
    if (download != null) {
      page = page != null ? page : new PagingRequest();

      List<CountryOccurrenceDownloadUsage> results =
          datasetOccurrenceDownloadMapper.listCountriesByDownload(
              downloadKey, sortBy, sortOrder, page);

      int count = datasetOccurrenceDownloadMapper.countCountriesByDownload(downloadKey);

      return new PagingResponse<>(page, (long) count, results);
    }
    throw new WebApplicationException("Download was not found", HttpStatus.NOT_FOUND);
  }

  @Hidden
  @PostMapping(value = "{key}/datasets", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Transactional
  @Secured(ADMIN_ROLE)
  @Override
  public void createUsages(
      @PathVariable("key") String downloadKey, @RequestBody Map<UUID, Long> datasetCitations) {
    LOG.debug("Dataset citations for download key {}:", downloadKey);
    datasetCitations.forEach((key, value) -> LOG.debug("{} - {}", key, value));

    Iterators.partition(datasetCitations.entrySet().iterator(), BATCH_SIZE)
        .forEachRemaining(
            batch ->
                datasetOccurrenceDownloadMapper.createOrUpdateUsages(
                    downloadKey,
                    batch.stream().collect(Collectors.toMap(Entry::getKey, Entry::getValue))));
  }

  @Override
  @NullToNotFound
  public String getCitation(String keyOrDoi) {
    Download download = get(keyOrDoi);
    return getCitationInternal(download);
  }

  @Tag(name = "Occurrence downloads")
  @Operation(
      operationId = "getDownloadCitationByKey",
      summary = "Shows the citation for a download",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0140")))
  @DownloadKeyParameter
  @ApiResponse(responseCode = "200", description = "Download citation.")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{key}/citation")
  @NullToNotFound(useUrlMapping = true)
  public String getCitationByKey(@NotNull @PathVariable("key") String key) {
    Download download = getByKey(key, false);
    return getCitationInternal(download);
  }

  @Tag(name = "Occurrence downloads")
  @Operation(
      operationId = "getDownloadCitationByDOI",
      summary = "Shows the citation for a download",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0141")))
  @DoiParameters
  @ApiResponse(responseCode = "200", description = "Download citation.")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{prefix}/{suffix}/citation")
  @NullToNotFound(useUrlMapping = true)
  public String getCitationByDoi(
      @NotNull @PathVariable("prefix") String prefix,
      @NotNull @PathVariable("suffix") String suffix) {
    Download download = getByDoi(prefix, suffix);
    return getCitationInternal(download);
  }

  private String getCitationInternal(Download download) {
    if (download != null) {
      assertDownloadType(download);
      return "GBIF.org ("
          + LONG_UN.format(download.getCreated())
          + ") GBIF Occurrence Download "
          + download.getDoi().getUrl().toString()
          + '\n';
    }
    return null;
  }

  @Tag(name = "Occurrence download statistics")
  @Operation(
      operationId = "getDownloadsByUserCountry",
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
    return groupByYear(
        occurrenceDownloadMapper.getDownloadsByUserCountry(
            fromDate,
            toDate,
            Optional.ofNullable(userCountry).map(Country::getIso2LetterCode).orElse(null),
            downloadType));
  }

  @Tag(name = "Occurrence download statistics")
  @Operation(
      operationId = "getDownloadedRecordsBySource",
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
    return groupByYear(
        occurrenceDownloadMapper.getDownloadsBySource(fromDate, toDate, source, downloadType));
  }

  @Tag(name = "Occurrence download statistics")
  @Operation(
      operationId = "getDownloadedRecordsByDataset",
      summary = "Summarize downloaded records by dataset",
      description =
          "Summarizes downloaded record totals by month, filtered by a publishing organization's country, territory or island and/or a single dataset",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0220")))
  @FromToParameters
  @Parameters(
      value = {
        @Parameter(
            name = "publishingCountry",
            description =
                "The ISO 3166-2 code for the publishing organization's country, territory or island.",
            schema = @Schema(implementation = Country.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "datasetKey",
            description = "The UUID for a dataset.",
            schema = @Schema(implementation = UUID.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "publishingOrgKey",
            description = "The UUID for a publishing organization.",
            schema = @Schema(implementation = UUID.class),
            in = ParameterIn.QUERY)
      })
  @ApiResponse(responseCode = "200", description = "Download statistics.")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("statistics/downloadedRecordsByDataset")
  @Override
  public Map<Integer, Map<Integer, Long>> getDownloadedRecordsByDataset( // TODO rename method?
      @PartialDate Date fromDate,
      @PartialDate Date toDate,
      Country publishingCountry,
      @RequestParam(value = "datasetKey", required = false) UUID datasetKey,
      @RequestParam(value = "publishingOrgKey", required = false) UUID publishingOrgKey) {
    return groupByYear(
        occurrenceDownloadMapper.getDownloadedRecordsByDataset(
            fromDate,
            toDate,
            Optional.ofNullable(publishingCountry).map(Country::getIso2LetterCode).orElse(null),
            datasetKey,
            publishingOrgKey,
            downloadType));
  }

  @Tag(name = "Occurrence download statistics")
  @Operation(
      operationId = "getDownloadedRecordsByDataset",
      summary = "Summarize downloads by dataset",
      description =
          "Summarizes downloads by month, filtered by a publishing organization's country, territory or island and/or a single dataset",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0230")))
  @FromToParameters
  @Parameters(
      value = {
        @Parameter(
            name = "publishingCountry",
            description =
                "The ISO 3166-2 code for the publishing organization's country, territory or island.",
            schema = @Schema(implementation = Country.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "datasetKey",
            description = "The UUID for a dataset.",
            schema = @Schema(implementation = UUID.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "publishingOrgKey",
            description = "The UUID for a publishing organization.",
            schema = @Schema(implementation = UUID.class),
            in = ParameterIn.QUERY)
      })
  @ApiResponse(responseCode = "200", description = "Download statistics.")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("statistics/downloadsByDataset")
  @Override
  public Map<Integer, Map<Integer, Long>> getDownloadsByDataset(
      @PartialDate Date fromDate,
      @PartialDate Date toDate,
      Country publishingCountry,
      @RequestParam(value = "datasetKey", required = false) UUID datasetKey,
      @RequestParam(value = "publishingOrgKey", required = false) UUID publishingOrgKey) {
    return groupByYear(
        occurrenceDownloadMapper.getDownloadsByDataset(
            fromDate,
            toDate,
            Optional.ofNullable(publishingCountry).map(Country::getIso2LetterCode).orElse(null),
            datasetKey,
            publishingOrgKey,
            downloadType));
  }

  @Tag(name = "Occurrence download statistics")
  @Operation(
      operationId = "getDownloadedStatistics",
      summary = "Provides summarized download statistics",
      description = "Filters for downloads matching the provided criteria, then provide counts by year, month and " +
        "dataset of the total number of downloads, and the total number of records included in those downloads.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0200")))
  @FromToParameters
  @Parameters(
      value = {
        @Parameter(
            name = "publishingCountry",
            description =
                "The ISO 3166-2 code for the publishing organization's country, territory or island.",
            schema = @Schema(implementation = Country.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "datasetKey",
            description = "The UUID for a dataset.",
            schema = @Schema(implementation = UUID.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "publishingOrgKey",
            description = "The UUID for a publishing organization.",
            schema = @Schema(implementation = UUID.class),
            in = ParameterIn.QUERY)
      })
  @Pageable.OffsetLimitParameters
  @ApiResponse(responseCode = "200", description = "Download statistics.")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("statistics")
  @Override
  public PagingResponse<DownloadStatistics> getDownloadStatistics(
      @PartialDate Date fromDate,
      @PartialDate Date toDate,
      Country publishingCountry,
      @RequestParam(value = "datasetKey", required = false) UUID datasetKey,
      @RequestParam(value = "publishingOrgKey", required = false) UUID publishingOrgKey,
      Pageable page) {
    String country =
        Optional.ofNullable(publishingCountry).map(Country::getIso2LetterCode).orElse(null);
    return new PagingResponse<>(
        page,
        occurrenceDownloadMapper.countDownloadStatistics(
            fromDate, toDate, country, datasetKey, publishingOrgKey, downloadType),
        occurrenceDownloadMapper.getDownloadStatistics(
            fromDate, toDate, country, datasetKey, publishingOrgKey, page, downloadType));
  }

  @Tag(name = "Occurrence download statistics")
  @Operation(
      operationId = "exportDownloadedStatistics",
      summary = "Export summary of downloads",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0201")))
  @FromToParameters
  @Parameters(
      value = {
        @Parameter(
            name = "publishingCountry",
            description =
                "The ISO 3166-2 code for the publishing organization's country, territory or island.",
            schema = @Schema(implementation = Country.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "datasetKey",
            description = "The UUID for a dataset.",
            schema = @Schema(implementation = UUID.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "publishingOrgKey",
            description = "The UUID for a publishing organization.",
            schema = @Schema(implementation = UUID.class),
            in = ParameterIn.QUERY)
      })
  @ApiResponse(responseCode = "200", description = "Download statistics.")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("statistics/export")
  public void getDownloadStatistics(
      HttpServletResponse response,
      @RequestParam(value = "format", defaultValue = "TSV") ExportFormat format,
      @PartialDate Date fromDate,
      @PartialDate Date toDate,
      Country publishingCountry,
      @RequestParam(value = "datasetKey", required = false) UUID datasetKey,
      @RequestParam(value = "publishingOrgKey", required = false) UUID publishingOrgKey)
      throws IOException {

    response.setHeader(
        HttpHeaders.CONTENT_DISPOSITION, EXPORT_FILE_HEADER_PRE + format.name().toLowerCase());

    try (Writer writer = new BufferedWriter(new OutputStreamWriter(response.getOutputStream()))) {
      CsvWriter.downloadStatisticsCsvWriter(
              Iterables.downloadStatistics(
                  this,
                  fromDate,
                  toDate,
                  publishingCountry,
                  datasetKey,
                  publishingOrgKey,
                  STATS_EXPORT_LIMIT),
              format)
          .export(writer);
    }
  }

  /** Aggregates the download statistics in tree structure of month grouped by year. */
  private Map<Integer, Map<Integer, Long>> groupByYear(List<Facet.Count> counts) {
    Map<Integer, Map<Integer, Long>> yearsGrouping = new TreeMap<>();
    counts.forEach(
        count ->
            yearsGrouping
                .computeIfAbsent(getYearFromFacetCount(count), year -> new TreeMap<>())
                .put(getMonthFromFacetCount(count), count.getCount()));
    return yearsGrouping;
  }

  private Integer getYearFromFacetCount(Facet.Count count) {
    return Integer.valueOf(count.getName().substring(0, 4));
  }

  private Integer getMonthFromFacetCount(Facet.Count count) {
    return Integer.valueOf(count.getName().substring(5));
  }
}
