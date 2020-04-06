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
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.common.search.Facet;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.model.registry.PrePersist;
import org.gbif.api.service.common.IdentityAccessService;
import org.gbif.api.service.registry.OccurrenceDownloadService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.License;
import org.gbif.registry.doi.generator.DoiGenerator;
import org.gbif.registry.doi.handler.DataCiteDoiHandlerStrategy;
import org.gbif.registry.persistence.mapper.DatasetOccurrenceDownloadMapper;
import org.gbif.registry.persistence.mapper.OccurrenceDownloadMapper;
import org.gbif.registry.ws.provider.PartialDate;
import org.gbif.ws.WebApplicationException;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
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

// TODO: 04/04/2020 some methods should accept doi instead of key, see oldMaster
/** Occurrence download resource/web service. */
@RestController
@RequestMapping(value = "occurrence/download", produces = MediaType.APPLICATION_JSON_VALUE)
public class OccurrenceDownloadResource implements OccurrenceDownloadService {

  private final OccurrenceDownloadMapper occurrenceDownloadMapper;
  private final DatasetOccurrenceDownloadMapper datasetOccurrenceDownloadMapper;
  private final IdentityAccessService identityService;
  private final DataCiteDoiHandlerStrategy doiHandlingStrategy;
  private final DoiGenerator doiGenerator;

  // Page size to iterate over dataset usages
  private static final int BATCH_SIZE = 5_000;

  public OccurrenceDownloadResource(
      OccurrenceDownloadMapper occurrenceDownloadMapper,
      DatasetOccurrenceDownloadMapper datasetOccurrenceDownloadMapper,
      DoiGenerator doiGenerator,
      @Lazy DataCiteDoiHandlerStrategy doiHandlingStrategy,
      @Qualifier("ligthweightIdentityAccessService") IdentityAccessService identityService) {
    this.occurrenceDownloadMapper = occurrenceDownloadMapper;
    this.datasetOccurrenceDownloadMapper = datasetOccurrenceDownloadMapper;
    this.doiHandlingStrategy = doiHandlingStrategy;
    this.doiGenerator = doiGenerator;
    this.identityService = identityService;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  @Transactional
  @Secured(ADMIN_ROLE)
  @Override
  public void create(
      @RequestBody @NotNull @Trim @Validated({PrePersist.class, Default.class})
          Download occurrenceDownload) {
    occurrenceDownload.setDoi(doiGenerator.newDownloadDOI());
    occurrenceDownload.setLicense(License.UNSPECIFIED);
    occurrenceDownloadMapper.create(occurrenceDownload);
  }

  @GetMapping("{key}")
  @NullToNotFound("/occurrence/download/{key}")
  @Override
  public Download get(@NotNull @PathVariable("key") String key) {
    Download download = occurrenceDownloadMapper.get(key);

    if (download != null) { // the user can request a non-existing download
      final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      clearSensitiveData(authentication, download);
    }
    return download;
  }

  @GetMapping("{prefix}/{suffix}")
  @NullToNotFound("/occurrence/download/{prefix}/{suffix}")
  public Download getByDoi(@PathVariable String prefix, @PathVariable String suffix) {
    Download download = occurrenceDownloadMapper.getByDOI(new DOI(prefix, suffix));

    if (download != null) { // the user can request a non-existing download
      final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      clearSensitiveData(authentication, download);
    }
    return download;
  }

  /** Lists all the downloads. This operation can be executed by role ADMIN only. */
  @GetMapping
  @Secured(ADMIN_ROLE)
  @Override
  public PagingResponse<Download> list(
      Pageable page,
      @Nullable @RequestParam(value = "status", required = false) Set<Download.Status> status) {
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
      @NotNull @PathVariable String user,
      Pageable page,
      @Nullable @RequestParam(value = "status", required = false) Set<Download.Status> status) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    checkUserIsInSecurityContext(user, authentication);
    return new PagingResponse<>(
        page,
        (long) occurrenceDownloadMapper.countByUser(user, status),
        occurrenceDownloadMapper.listByUser(user, page, status));
  }

  @PutMapping(
      value = {"", "{key}"},
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @Transactional
  @Override
  public void update(@RequestBody @NotNull Download download) {
    // The current download is retrieved because its user could be modified during the update
    Download currentDownload = get(download.getKey());
    Preconditions.checkNotNull(currentDownload);
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    checkUserIsInSecurityContext(currentDownload.getRequest().getCreator(), authentication);
    GbifUser user = identityService.get(authentication.getName());
    doiHandlingStrategy.downloadChanged(download, currentDownload, user);
    occurrenceDownloadMapper.update(download);
  }

  @GetMapping("{key}/datasets")
  @Override
  public PagingResponse<DatasetOccurrenceDownloadUsage> listDatasetUsages(
      @NotNull @PathVariable("key") String downloadKey, Pageable page) {
    Download download = get(downloadKey);
    if (download != null) {
      List<DatasetOccurrenceDownloadUsage> usages =
          datasetOccurrenceDownloadMapper.listByDownload(downloadKey, page);
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
      @NotNull @PathVariable("key") String downloadKey,
      @RequestBody @NotNull Map<UUID, Long> datasetCitations) {
    Iterators.partition(datasetCitations.entrySet().iterator(), BATCH_SIZE)
        .forEachRemaining(
            batch ->
                datasetOccurrenceDownloadMapper.createUsages(
                    downloadKey,
                    batch.stream().collect(Collectors.toMap(Entry::getKey, Entry::getValue))));
  }

  // TODO: 04/04/2020 see commit from oldMaster 31/03/2020
  @GetMapping("{key:.+}/citation")
  @NullToNotFound
  public String getCitation(@NotNull @PathVariable("key") String keyOrDoi) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @GetMapping("statistics/downloadsByUserCountry")
  @Override
  public Map<Integer, Map<Integer, Long>> getDownloadsByUserCountry(
      @Nullable @PartialDate Date fromDate,
      @Nullable @PartialDate Date toDate,
      @Nullable Country userCountry) {
    return groupByYear(
        occurrenceDownloadMapper.getDownloadsByUserCountry(
            fromDate,
            toDate,
            Optional.ofNullable(userCountry).map(Country::getIso2LetterCode).orElse(null)));
  }

  @GetMapping("statistics/downloadedRecordsByDataset")
  @Override
  public Map<Integer, Map<Integer, Long>> getDownloadedRecordsByDataset(
      @Nullable @PartialDate Date fromDate,
      @Nullable @PartialDate Date toDate,
      @Nullable Country publishingCountry,
      @RequestParam(value = "datasetKey", required = false) UUID datasetKey) {
    return groupByYear(
        occurrenceDownloadMapper.getDownloadedRecordsByDataset(
            fromDate,
            toDate,
            Optional.ofNullable(publishingCountry).map(Country::getIso2LetterCode).orElse(null),
            datasetKey));
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
