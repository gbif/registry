/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.common.search.Facet;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.occurrence.DownloadStatistics;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.model.registry.PostPersist;
import org.gbif.api.model.registry.PrePersist;
import org.gbif.api.service.common.IdentityAccessService;
import org.gbif.api.service.registry.OccurrenceDownloadService;
import org.gbif.api.util.iterables.Iterables;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.License;
import org.gbif.registry.doi.DoiIssuingService;
import org.gbif.registry.doi.DownloadDoiDataCiteHandlingService;
import org.gbif.registry.persistence.mapper.DatasetOccurrenceDownloadMapper;
import org.gbif.registry.persistence.mapper.OccurrenceDownloadMapper;
import org.gbif.registry.ws.export.CsvWriter;
import org.gbif.registry.ws.provider.PartialDate;
import org.gbif.ws.WebApplicationException;

import java.io.IOException;
import java.util.Date;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;

import static org.gbif.registry.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.security.util.DownloadSecurityUtils.checkUserIsInSecurityContext;
import static org.gbif.registry.security.util.DownloadSecurityUtils.clearSensitiveData;

/** Occurrence download resource/web service. */
@Validated
@RestController
@RequestMapping(value = "occurrence/download", produces = MediaType.APPLICATION_JSON_VALUE)
public class OccurrenceDownloadResource implements OccurrenceDownloadService {

  private final OccurrenceDownloadMapper occurrenceDownloadMapper;
  private final DatasetOccurrenceDownloadMapper datasetOccurrenceDownloadMapper;
  private final IdentityAccessService identityService;
  private final DoiIssuingService doiIssuingService;
  private final DownloadDoiDataCiteHandlingService doiDataCiteHandlingService;

  // Page size to iterate over dataset usages
  private static final int BATCH_SIZE = 5_000;

  private static final Logger LOG = LoggerFactory.getLogger(OccurrenceDownloadResource.class);

  private static final Marker NOTIFY_ADMIN = MarkerFactory.getMarker("NOTIFY_ADMIN");

  public OccurrenceDownloadResource(
      OccurrenceDownloadMapper occurrenceDownloadMapper,
      DatasetOccurrenceDownloadMapper datasetOccurrenceDownloadMapper,
      DoiIssuingService doiIssuingService,
      @Lazy DownloadDoiDataCiteHandlingService doiDataCiteHandlingService,
      @Qualifier("baseIdentityAccessService") IdentityAccessService identityService) {
    this.occurrenceDownloadMapper = occurrenceDownloadMapper;
    this.datasetOccurrenceDownloadMapper = datasetOccurrenceDownloadMapper;
    this.doiIssuingService = doiIssuingService;
    this.doiDataCiteHandlingService = doiDataCiteHandlingService;
    this.identityService = identityService;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PrePersist.class, Default.class})
  @Trim
  @Secured(ADMIN_ROLE)
  @Override
  public void create(@RequestBody @Trim Download occurrenceDownload) {
    try {
      occurrenceDownload.setDoi(doiIssuingService.newDownloadDOI());
      occurrenceDownload.setLicense(License.UNSPECIFIED);
      occurrenceDownloadMapper.create(occurrenceDownload);
    } catch (Exception ex) {
      LOG.error(NOTIFY_ADMIN, "Error creating download", ex);
      throw new RuntimeException(ex);
    }
  }

  @Override
  public Download get(String keyOrDoi) {
    Download download = getByKey(keyOrDoi);

    if (download == null && DOI.isParsable(keyOrDoi)) { // maybe it's a DOI?
      DOI doi = new DOI(keyOrDoi);
      download = getByDoi(doi.getPrefix(), doi.getSuffix());
    }

    return download;
  }

  @GetMapping("{key}")
  @NullToNotFound("/occurrence/download/{key}")
  public Download getByKey(@NotNull @PathVariable("key") String key) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Download download = occurrenceDownloadMapper.get(key);
    clearSensitiveData(authentication, download);
    return download;
  }

  @GetMapping("{prefix}/{suffix}")
  @NullToNotFound("/occurrence/download/{prefix}/{suffix}")
  public Download getByDoi(
      @NotNull @PathVariable String prefix, @NotNull @PathVariable String suffix) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Download download = occurrenceDownloadMapper.getByDOI(new DOI(prefix, suffix));
    clearSensitiveData(authentication, download);
    return download;
  }

  /** Lists all the downloads. This operation can be executed by role ADMIN only. */
  @GetMapping
  @Secured(ADMIN_ROLE)
  @Override
  public PagingResponse<Download> list(
      Pageable page,
      @RequestParam(value = "status", required = false) Set<Download.Status> status) {
    if (status == null || status.isEmpty()) {
      return new PagingResponse<>(
          page, (long) occurrenceDownloadMapper.count(), occurrenceDownloadMapper.list(page));
    } else {
      return new PagingResponse<>(
          page,
          (long) occurrenceDownloadMapper.countByStatus(status),
          occurrenceDownloadMapper.listByStatus(page, status));
    }
  }

  @GetMapping("user/{user}")
  @Override
  public PagingResponse<Download> listByUser(
      @PathVariable String user,
      Pageable page,
      @RequestParam(value = "status", required = false) Set<Download.Status> status) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    checkUserIsInSecurityContext(user, authentication);
    return new PagingResponse<>(
        page,
        (long) occurrenceDownloadMapper.countByUser(user, status),
        occurrenceDownloadMapper.listByUser(user, page, status));
  }

  @SuppressWarnings("MVCPathVariableInspection")
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
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    checkUserIsInSecurityContext(currentDownload.getRequest().getCreator(), authentication);
    GbifUser user = identityService.get(authentication.getName());
    doiDataCiteHandlingService.downloadChanged(download, currentDownload, user);
    occurrenceDownloadMapper.update(download);
  }

  @Override
  public PagingResponse<DatasetOccurrenceDownloadUsage> listDatasetUsages(
      String keyOrDoi, Pageable page) {
    Download download = get(keyOrDoi);
    return listDatasetUsagesInternal(download.getKey(), page, download);
  }

  @GetMapping("{prefix}/{suffix}/datasets")
  public PagingResponse<DatasetOccurrenceDownloadUsage> listDatasetUsagesByDoi(
      @PathVariable("prefix") String prefix, @PathVariable("suffix") String suffix, Pageable page) {
    Download download = getByDoi(prefix, suffix);
    return listDatasetUsagesInternal(download.getKey(), page, download);
  }

  @GetMapping("{key}/datasets")
  public PagingResponse<DatasetOccurrenceDownloadUsage> listDatasetUsagesByKey(
      @PathVariable("key") String key, Pageable page) {
    Download download = get(key);
    return listDatasetUsagesInternal(key, page, download);
  }

  private PagingResponse<DatasetOccurrenceDownloadUsage> listDatasetUsagesInternal(
      String key, Pageable page, Download download) {
    if (download != null) {
      List<DatasetOccurrenceDownloadUsage> usages =
          datasetOccurrenceDownloadMapper.listByDownload(key, page);
      final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      clearSensitiveData(authentication, usages);
      return new PagingResponse<>(page, download.getNumberDatasets(), usages);
    }
    throw new WebApplicationException("Download was not found", HttpStatus.NOT_FOUND);
  }

  @PostMapping(value = "{key}/datasets", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Transactional
  @Secured(ADMIN_ROLE)
  @Override
  public void createUsages(
      @PathVariable("key") String downloadKey, @RequestBody Map<UUID, Long> datasetCitations) {
    Iterators.partition(datasetCitations.entrySet().iterator(), BATCH_SIZE)
        .forEachRemaining(
            batch ->
                datasetOccurrenceDownloadMapper.createUsages(
                    downloadKey,
                    batch.stream().collect(Collectors.toMap(Entry::getKey, Entry::getValue))));
  }

  @Override
  @NullToNotFound
  public String getCitation(String keyOrDoi) {
    Download download = get(keyOrDoi);
    return getCitationInternal(download);
  }

  @GetMapping("{key}/citation")
  @NullToNotFound("/occurrence/download/{key}/citation")
  public String getCitationByKey(@NotNull @PathVariable("key") String key) {
    Download download = getByKey(key);
    return getCitationInternal(download);
  }

  @GetMapping("{prefix}/{suffix}/citation")
  @NullToNotFound("/occurrence/download/{prefix}/{suffix}/citation")
  public String getCitationByDoi(
      @NotNull @PathVariable("prefix") String prefix,
      @NotNull @PathVariable("suffix") String suffix) {
    Download download = getByDoi(prefix, suffix);
    return getCitationInternal(download);
  }

  private String getCitationInternal(Download download) {
    if (download != null) {
      // Citations are incorrect, see https://github.com/gbif/occurrence/issues/156.
      // For the moment, just use the main citation.
      // List<DatasetOccurrenceDownloadUsage> usages =
      // datasetOccurrenceDownloadMapper.listByDownload(downloadKey, null);

      // usages.forEach(
      //  usage -> { if (usage != null) sb.append(usage.getDatasetCitation()).append('\n'); }
      // );

      return "GBIF Occurrence Download " + download.getDoi().getUrl().toString() + '\n';

      // usages.forEach(
      //   usage -> { if (usage != null) sb.append(usage.getDatasetCitation()).append('\n'); });
    }
    return null;
  }

  @GetMapping("statistics/downloadsByUserCountry")
  @Override
  public Map<Integer, Map<Integer, Long>> getDownloadsByUserCountry(
      @PartialDate Date fromDate, @PartialDate Date toDate, Country userCountry) {
    return groupByYear(
        occurrenceDownloadMapper.getDownloadsByUserCountry(
            fromDate,
            toDate,
            Optional.ofNullable(userCountry).map(Country::getIso2LetterCode).orElse(null)));
  }

  @GetMapping("statistics/downloadedRecordsByDataset")
  @Override
  public Map<Integer, Map<Integer, Long>> getDownloadedRecordsByDataset(
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
            publishingOrgKey));
  }

  @GetMapping("statistics/downloadsByDataset")
  @Override
  public Map<Integer, Map<Integer, Long>> getDownloadsByDataset(
    @PartialDate Date fromDate,
    @PartialDate Date toDate,
    Country publishingCountry,
    @RequestParam(value = "datasetKey", required = false) UUID datasetKey,
    @RequestParam(value = "publishingOrgKey", required = false) UUID publishingOrgKey
  ) {
    return groupByYear(
      occurrenceDownloadMapper.getDownloadsByDataset(
        fromDate,
        toDate,
        Optional.ofNullable(publishingCountry).map(Country::getIso2LetterCode).orElse(null),
        datasetKey,
        publishingOrgKey));
  }

  @GetMapping("statistics")
  @Override
  public PagingResponse<DownloadStatistics> getDownloadStatistics(
    @PartialDate Date fromDate,
    @PartialDate Date toDate,
    Country publishingCountry,
    @RequestParam(value = "datasetKey", required = false) UUID datasetKey,
    @RequestParam(value = "publishingOrgKey", required = false) UUID publishingOrgKey,
    Pageable page
  ) {
    String country = Optional.ofNullable(publishingCountry).map(Country::getIso2LetterCode).orElse(null);
    return new PagingResponse<>(page,
                                occurrenceDownloadMapper.countDownloadStatistics(fromDate,
                                                                                 toDate,
                                                                                 country,
                                                                                 datasetKey,
                                                                                 publishingOrgKey),
                                occurrenceDownloadMapper.getDownloadStatistics(fromDate,
                                                                               toDate,
                                                                               country,
                                                                               datasetKey,
                                                                               publishingOrgKey,
                                                                               page));
  }

  @GetMapping("statistics/export")
  public void getDownloadStatistics(
    HttpServletResponse response,
    @RequestParam(value = "format", defaultValue = "TSV") ExportFormat format,
    @PartialDate Date fromDate,
    @PartialDate Date toDate,
    Country publishingCountry,
    @RequestParam(value = "datasetKey", required = false) UUID datasetKey,
    @RequestParam(value = "publishingOrgKey", required = false) UUID publishingOrgKey) throws
    IOException {

      String headerValue = "attachment; filename=download_statistics." +  format.name().toLowerCase();
      response.setHeader(HttpHeaders.CONTENT_DISPOSITION, headerValue);


      CsvWriter.downloadStatisticsCsvWriter(Iterables.downloadStatistics(this,
                                                                         fromDate,
                                                                         toDate,
                                                                         publishingCountry,
                                                                         datasetKey,
                                                                         publishingOrgKey),
                                            format)
      .export(response.getWriter());
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
